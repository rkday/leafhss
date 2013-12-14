(ns clojure-jdiameter.cx-uar-test
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
                  (get-public-with-scscf "example.com")
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

(defn create-mock-uar [priv pub auth-type visited-network]
  (let [user-name-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn priv))
        public-identity-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn pub))
        auth-type-avp (when-> (mock Avp) (.getInteger32) (.thenReturn (int auth-type)))
        visited-network-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn visited-network))
        mock-avpset (mock AvpSet)
        mocked (when-> (make-mock 300) (.getAvps) (.thenReturn mock-avpset))]
    (when-> mock-avpset
            (.getAvp 1)
            (.thenReturn user-name-avp))
    (when-> mock-avpset
            (.getAvp 601 10415)
            (.thenReturn public-identity-avp))
    (when-> mock-avpset
            (.getAvp 600 10415)
            (.thenReturn visited-network-avp))
    (when-> mock-avpset
            (.getAvp 623 10415)
            (.thenReturn auth-type-avp))
    (when-> mocked (.createAnswer 2001) (.thenReturn mock-ok-answer))
    (when-> mocked (.createAnswer 10415 2002) (.thenReturn mock-ok-answer))
    (when-> mocked (.createAnswer 10415 5002) (.thenReturn mock-disassoc-answer))
    (when-> mocked (.createAnswer 10415 5006) (.thenReturn mock-bad-auth-answer))
    mocked))

(deftest success-test
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             0
                             "example.com"))))))

