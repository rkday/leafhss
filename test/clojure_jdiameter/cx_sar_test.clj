(ns clojure-jdiameter.cx-sar-test
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

(defn create-mock-sar [priv pub server-name server-assignment-type user-data-already-available]
  (let [user-name-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn priv))
        public-identity-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn pub))
        server-name-avp (when-> (mock Avp) (.getUTF8String) (.thenReturn server-name))
        server-assignment-type-avp (when-> (mock Avp) (.getInteger32) (.thenReturn (int server-assignment-type)))
        user-data-already-available-avp (when-> (mock Avp) (.getInteger32) (.thenReturn (int user-data-already-available)))
        mock-avpset (mock AvpSet)
        mocked (when-> (make-mock 301) (.getAvps) (.thenReturn mock-avpset))]
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
            (.getAvp 614 10415)
            (.thenReturn server-assignment-type-avp))
    (when-> mock-avpset
            (.getAvp 624 10415)
            (.thenReturn user-data-already-available-avp))
    mocked))

(deftest success-test
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-sar "rkd@example.com"
                             "sip:rkd@example.com"
                             "example.com"
                             1
                             0))))

    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-sar "rkd@example.com"
                             "sip:rkd@example.com"
                             "example.com"
                             0
                             0))))
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-sar "rkd@example.com"
                             "sip:rkd@example.com"
                             "example.com"
                             8
                             0))))
    (is (= mock-ok-answer
           (.processRequest
            cx-listener
            (create-mock-sar "rkd@example.com"
                             "sip:rkd@example.com"
                             "example.com"
                             10
                             0))))


    ))

(deftest unknown-test
  (testing "Test that a known Multimedia-Auth-Request passes validation"
    (is (= mock-unknown-answer
           (.processRequest
            public-not-found-cx-listener
            (create-mock-sar "rkd@example.com"
                             "sip:rkd@example.com"
                             "example.com"
                             1
                             0)
            )))
    (is (= mock-unknown-answer
           (.processRequest
            private-not-found-cx-listener
            (create-mock-sar "rkd@example.com"
                             "sip:rkd@example.com"
                             "example.com"
                             1
                             0)
            )))
    (is (= mock-disassoc-answer
           (.processRequest
            cx-listener
            (create-mock-sar "OTHER@example.com"
                             "sip:rkd@example.com"
                             "example.com"
                             1
                             0)

            )))
    (is (= mock-cant-comply-answer
           (.processRequest
            unregistered-cx-listener
            (create-mock-sar "rkd@example.com"
                             "sip:rkd@example.com"
                             "example.com"
                             0
                             0))))

    ))

