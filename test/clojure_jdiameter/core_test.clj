(ns clojure-jdiameter.core-test
  (:require [clojure.test :refer :all]
            [clojure-jdiameter.core :refer :all]
            [cljito.core :refer :all])
  (:import org.jdiameter.api.Request
           org.jdiameter.api.Answer
           org.jdiameter.api.AvpSet
           org.jdiameter.api.Avp
           ))
(defn create-mock-answer []
  (let [mockset1 (when->
                 (mock AvpSet)
                 (.addGroupedAvp (any-int) (any-int) (any-boolean) (any-boolean))
                 (.thenReturn (mock AvpSet)))
        mockset (when->
                 (mock AvpSet)
                 (.addGroupedAvp (any-int) (any-int) (any-boolean) (any-boolean))
                 (.thenReturn mockset1))]
    (when->
     (mock Answer)
     (.getAvps)
     (.thenReturn mockset))))

(def mock-ok-answer (create-mock-answer))
(def mock-bad-auth-answer (mock Answer))
(def mock-unknown-answer (mock Answer))
(def mock-disassoc-answer (mock Answer))

(defn create-mock-request [priv pub scscf auth-scheme]
  (let [user-name-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn priv))
        public-identity-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn pub))
        server-name-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn scscf))
        mock-authscheme-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn auth-scheme))
        mock-authdata-avpset (when-> (mock AvpSet) (.getAvp 608 10415) (.thenReturn mock-authscheme-avp))
        mock-authdata-avp (when-> (mock Avp) (.getGrouped) (.thenReturn mock-authdata-avpset))
        mock-avpset (mock AvpSet)
        mocked (when-> (mock Request) (.getAvps) (.thenReturn mock-avpset))]
    (when-> mock-avpset
            (.getAvp 2)
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
    (when-> mocked (.createAnswer 5001 10415) (.thenReturn mock-unknown-answer))
    (when-> mocked (.createAnswer 5002 10415) (.thenReturn mock-disassoc-answer))
    (when-> mocked (.createAnswer 5006 10415) (.thenReturn mock-bad-auth-answer))
    mocked))

(deftest a-test1
  (testing "FIXME, I fail."
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-request "rkd" "sip:rkd@example.com" "sip:example.com;transport=tcp" "Unknown"))))))

(deftest a-test2
  (testing "FIXME, I fail."
    (is (= mock-disassoc-answer
           (.processRequest
            cx-listener
            (create-mock-request "Bad user" "sip:rkd@example.com" "sip:example.com;transport=tcp" "Unknown"))))))

(deftest a-test3
  (testing "FIXME, I fail."
    (is (= mock-bad-auth-answer
           (.processRequest
            cx-listener
            (create-mock-request "rkd" "sip:rkd@example.com" "sip:example.com;transport=tcp" "SOMETHINGELSE"))))))
