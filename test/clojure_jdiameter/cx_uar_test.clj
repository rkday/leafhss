(ns clojure-jdiameter.cx-uar-test
  (:require [clojure.test :refer :all]
            [clojure-jdiameter.cx :refer :all]
            [clojure-jdiameter.data :as data]
            [clojure-jdiameter.testdata :refer :all]
            [clojure-jdiameter.common-test :refer :all]
            [cljito.core :refer :all])
  (:import org.jdiameter.api.Request
           org.jdiameter.api.Answer
           org.jdiameter.api.AvpSet
           org.jdiameter.api.Avp))

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
    mocked))

(deftest success-test
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             0
                             "example.com"))))
    (is (= mock-ok-answer
           (.processRequest
            unregistered-cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             0
                             "example.com"))))

    ))

(deftest success-test-deregistration
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             1
                             "example.com"))))))

(deftest success-test-registration-and-capabilities
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             2
                             "example.com"))))))

(deftest unknown-test
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-unknown-answer
           (.processRequest
            public-not-found-cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             2
                             "example.com"))))
    (is (= mock-unknown-answer
           (.processRequest
            private-not-found-cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             2
                             "example.com"))))
    (is (= mock-disassoc-answer
           (.processRequest
            cx-listener
            (create-mock-uar "OTHER@example.com"
                             "sip:rkd@example.com"
                             2
                             "example.com"))))
    (is (= mock-roaming-not-allowed-answer
           (.processRequest
            cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             0
                             "example.co.uk"))))))

