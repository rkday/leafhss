(ns org.leafhss.hss.cx-mar-test
  (:require [clojure.test :refer :all]
            [org.leafhss.hss.cx :refer :all]
            [org.leafhss.hss.data :as data]
            [org.leafhss.hss.testdata :refer :all]
            [org.leafhss.hss.common-test :refer :all]
            [cljito.core :refer :all])
  (:import org.jdiameter.api.AvpSet
           org.jdiameter.api.Avp))

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

(deftest unknown-test
  (testing "Test that a Multimedia-Auth-Request fails if either the public or private ID it references do not exist"
    (is (= mock-unknown-answer
           (.processRequest
            public-not-found-cx-listener
            (create-mock-mar "rkd@example.com"
                             "sip:rkd@example.com"
                             "sip:example.com;transport=tcp"
                             "Unknown"))))
    (is (= mock-unknown-answer
           (.processRequest
            private-not-found-cx-listener
            (create-mock-mar "rkd@example.com"
                             "sip:rkd@example.com"
                             "sip:example.com;transport=tcp"
                             "Unknown"))))))

