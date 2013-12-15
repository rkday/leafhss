(ns org.leafhss.hss.cx-lir-test
  (:require [clojure.test :refer :all]
            [org.leafhss.hss.cx :refer :all]
            [org.leafhss.hss.data :as data]
            [org.leafhss.hss.testdata :refer :all]
            [org.leafhss.hss.common-test :refer :all]
            [cljito.core :refer :all])
  (:import org.jdiameter.api.AvpSet
           org.jdiameter.api.Avp))

(defn create-mock-lir [auth-type pub]
  (let [auth-type-avp (when (not (nil? auth-type)) (when->  (mock Avp) (.getInteger32) (.thenReturn (int auth-type))))
        public-identity-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn pub))
        mock-avpset (mock AvpSet)
        mocked (when-> (make-mock 302) (.getAvps) (.thenReturn mock-avpset))]
    (when-> mock-avpset
            (.getAvp 601 10415)
            (.thenReturn public-identity-avp))
    (when-> mock-avpset
            (.getAvp 623 10415)
            (.thenReturn auth-type-avp))
    mocked))

(deftest success-test
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-lir 0
                             "sip:rkd@example.com"))))))

(deftest no-authtype-avp-test
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-lir nil
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

(deftest unknown-test
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-unknown-answer
           (.processRequest
            public-not-found-cx-listener
            (create-mock-lir 0
                             "sip:rkd@example.com")
            )))
    ))

