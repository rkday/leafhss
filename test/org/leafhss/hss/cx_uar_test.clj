(ns org.leafhss.hss.cx-uar-test
  (:require [clojure.test :refer :all]
            [org.leafhss.hss.cx :refer :all]
            [org.leafhss.hss.data :as data]
            [org.leafhss.hss.testdata :refer :all]
            [org.leafhss.hss.constants :refer :all]
            [org.leafhss.hss.common-test :refer :all]
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

(deftest registration
  (testing "Test that a UAR succeeds when the user is already registered"
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             REGISTRATION
                             "example.com")))))
  (testing "Test that a UAR succeeds when the user is not already registered"
    (is (= mock-ok-answer
           (.processRequest
            unregistered-cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             REGISTRATION
                             "example.com"))))))

(deftest deregistration
  (testing "Test that a UAR succeeds when it is for a deregistration of a registered user"
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             DEREGISTRATION
                             "example.com")))))

  (testing "Test that a UAR fails when it is for a deregistration of an unregistered user"
    (is (= mock-unregistered-answer
           (.processRequest
            unregistered-cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             DEREGISTRATION
                             "example.com"))))))

(deftest registration-and-capabilities
  (testing "Test that a UAR succeeds when it requests the capabilities (for SIP restoration)"
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             REGISTRATION_AND_CAPABILITIES
                             "example.com")))))
  (testing "Test that a UAR succeeds when it requests the capabilities (for SIP restoration) and there are no capabilities"
    (is (= mock-ok-answer
           (.processRequest
            no-capabilities-cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             REGISTRATION_AND_CAPABILITIES
                             "example.com")))))
  (testing "Test that a UAR succeeds when it requests the capabilities (for SIP restoration) and there are no mandatory capabilities"
    (is (= mock-ok-answer
           (.processRequest
            optional-capabilities-cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             REGISTRATION_AND_CAPABILITIES
                             "example.com"))))))

(deftest id-failures
  (testing "Test that a UAR fails when the public or private ID it references are unknown or not associated"
    (is (= mock-unknown-answer
           (.processRequest
            public-not-found-cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             REGISTRATION
                             "example.com"))))
    (is (= mock-unknown-answer
           (.processRequest
            private-not-found-cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             REGISTRATION
                             "example.com"))))
    (is (= mock-disassoc-answer
           (.processRequest
            cx-listener
            (create-mock-uar "OTHER@example.com"
                             "sip:rkd@example.com"
                             REGISTRATION
                             "example.com"))))
    ))

(deftest roaming-failure
  (testing "Test that a UAR fails when it comes from a visited network where roaming is forbidden"
    (is (= mock-roaming-not-allowed-answer
           (.processRequest
            cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             REGISTRATION
                             "example.co.uk")))))
  (testing "Test that a UAR specifying deregistration ignores roaming"
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-uar "rkd@example.com"
                             "sip:rkd@example.com"
                             DEREGISTRATION
                             "example.co.uk"))))))

(deftest barring-test
  (testing "Test that a User-Authorization-Request behaves as expected when public IDs are barred"
    (testing "Test that a UAR is rejected when all public IDs are barred"
      (is (= mock-rejected-answer
             (.processRequest
              all-barred-cx-listener
              (create-mock-uar "rkd@example.com"
                               "sip:rkd@example.com"
                               REGISTRATION
                               "example.com")))))
    (testing "Test that a UAR is not rejected when not all public IDs are barred"
      (is (= mock-ok-answer
             (.processRequest
              one-barred-cx-listener
              (create-mock-uar "rkd@example.com"
                               "sip:rkd@example.com"
                               REGISTRATION
                               "example.com")))))))

