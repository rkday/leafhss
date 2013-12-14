(ns clojure-jdiameter.cx-mar-test
  (:require [clojure.test :refer :all]
            [clojure-jdiameter.cx :refer :all]
            [clojure-jdiameter.data :as data]
            [clojure-jdiameter.testdata :refer :all]
            [cljito.core :refer :all])
  (:import org.jdiameter.api.Request
           org.jdiameter.api.Answer
           org.jdiameter.api.AvpSet
           org.jdiameter.api.Avp))

(def cx-listener (make-cx-listener
                  get-public
                  get-private
                  update-scscf!
                  clear-scscf!
                  register!
                  set-auth-pending!
                  unregister!
                  update-aka-seqn!
                  set-scscf-reassignment-pending!))


(defn create-mock-answer []
  (let [mockset1 (when->
                 (mock AvpSet)
                 (.addGroupedAvp (any-int) (any-int) (any-boolean) (any-boolean))
                 (.thenReturn (mock AvpSet)))
        mockset (when->
                 (mock AvpSet)
                 (.addGroupedAvp (any-int) (any-int) (any-boolean) (any-boolean))
                 (.thenReturn mockset1))
        ans (mock Answer)]
    (when->
     ans
     (.getAvps)
     (.thenReturn mockset))
    ans))

(defn make-mock [code]
  (when->
     (mock Request)
     (.getCommandCode)
     (.thenReturn (int code))))

(def mock-ok-answer (create-mock-answer))
(def mock-bad-auth-answer (mock Answer))
(def mock-unknown-answer (mock Answer))
(def mock-disassoc-answer (mock Answer))

(defn create-mock-mar [priv pub scscf auth-scheme]
  (let [user-name-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn priv))
        public-identity-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn pub))
        server-name-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn scscf))
        mock-authscheme-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn auth-scheme))
        mock-authdata-avpset (when-> (mock AvpSet) (.getAvp 608 10415) (.thenReturn mock-authscheme-avp))
        mock-authdata-avp (when-> (mock Avp) (.getGrouped) (.thenReturn mock-authdata-avpset))
        mock-avpset (mock AvpSet)
        mocked (when-> (make-mock 303) (.getAvps) (.thenReturn mock-avpset))]
    (when-> mock-avpset
            (.getAvp 1)
            (.thenReturn user-name-avp))
    (when-> mock-avpset
            (.getAvp 601 10415)
            (.thenReturn public-identity-avp))
    (when-> mock-avpset
            (.getAvp 602 10415)
            (.thenReturn server-name-avp))
    (when-> mock-avpset
            (.getAvp 612 10415)
            (.thenReturn mock-authdata-avp))
    (when-> mocked (.createAnswer 2001) (.thenReturn mock-ok-answer))
    (when-> mocked (.createAnswer 10415 5001) (.thenReturn mock-unknown-answer))
    (when-> mocked (.createAnswer 10415 5002) (.thenReturn mock-disassoc-answer))
    (when-> mocked (.createAnswer 10415 5006) (.thenReturn mock-bad-auth-answer))
    mocked))

(deftest success-test
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-mar "rkd@example.com"
                             "sip:rkd@example.com"
                             "sip:example.com;transport=tcp"
                             "Unknown"))))))

(deftest not-associated-test
  (testing "Test that a MAR referencing a public and private ID which are not associated returns the correct error"
    (is (= mock-disassoc-answer
           (.processRequest
            cx-listener
            (create-mock-mar "Bad user" "sip:rkd@example.com" "sip:example.com;transport=tcp" "Unknown"))))))

(deftest bad-auth-test
  (testing "Test that a MAR referencing an unsupported authentication scheme receives an appropriate answer"
    (is (= mock-bad-auth-answer
           (.processRequest
            cx-listener
            (create-mock-mar "rkd@example.com" "sip:rkd@example.com" "sip:example.com;transport=tcp" "SOMETHINGELSE"))))))
