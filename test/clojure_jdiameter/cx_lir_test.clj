(ns clojure-jdiameter.cx-lir-test
  (:require [clojure.test :refer :all]
            [clojure-jdiameter.cx :refer :all]
            [clojure-jdiameter.data :as data]
            [clojure-jdiameter.testdata :refer :all]
            [cljito.core :refer :all])
  (:import org.jdiameter.api.Request
           org.jdiameter.api.Answer
           org.jdiameter.api.AvpSet
           org.jdiameter.api.Avp))

(def registered-cx-listener (make-cx-listener
                             (get-public-with-scscf "example.com")
                             get-private
                             update-scscf!
                             clear-scscf!
                             register!
                             set-auth-pending!
                             unregister!
                             update-aka-seqn!
                             set-scscf-reassignment-pending!))

(def unregistered-cx-listener (make-cx-listener
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
(def mock-subsequent-reg-answer (mock Answer))
(def mock-unknown-answer (mock Answer))
(def mock-unregistered-service-answer (create-mock-answer))

(defn create-mock-lir [auth-type pub]
  (let [auth-type-avp (when-> (mock Avp) (.getInteger32) (.thenReturn (int auth-type)))
        public-identity-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn pub))
        mock-avpset (mock AvpSet)
        mocked (when-> (make-mock 302) (.getAvps) (.thenReturn mock-avpset))]
    (when-> mock-avpset
            (.getAvp 601 10415)
            (.thenReturn public-identity-avp))
    (when-> mock-avpset
            (.getAvp 623 10415)
            (.thenReturn auth-type-avp))
    (when-> mocked (.createAnswer 2001) (.thenReturn mock-ok-answer))
    (when-> mocked (.createAnswer 10415 2001) (.thenReturn mock-ok-answer))
    (when-> mocked (.createAnswer 10415 2003) (.thenReturn mock-unregistered-service-answer))
    (when-> mocked (.createAnswer 10415 5001) (.thenReturn mock-unknown-answer))
    mocked))

(deftest success-test
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-ok-answer
           (.processRequest
            registered-cx-listener
            (create-mock-lir 0
                             "sip:rkd@example.com"))))))

(deftest unreg-test
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-unregistered-service-answer
           (.processRequest
            unregistered-cx-listener
            (create-mock-lir 0
                             "sip:rkd@example.com"))))))

(deftest restoration-test
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-ok-answer
           (.processRequest
            unregistered-cx-listener
            (create-mock-lir 2
                             "sip:rkd@example.com"))))))
