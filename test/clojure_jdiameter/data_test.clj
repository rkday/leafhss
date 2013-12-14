(ns clojure-jdiameter.data-test
  (:require [clojure.test :refer :all]
            [clojure-jdiameter.data :as data]
            [clojure-jdiameter.testdata :refer :all])
  (:import org.jdiameter.api.Request
           org.jdiameter.api.Answer
           org.jdiameter.api.AvpSet
           org.jdiameter.api.Avp))

(deftest correct-xml
  (testing "that get-userdata returns a correct XML document where the first public identity listed is the SIP URI with the lexicographically-first username")
  (is (= (data/get-userdata "sip:rkd3@example.com" example-public)
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?><IMSSubscription><PrivateID>rkd@example.com</PrivateID><ServiceProfile><PublicIdentity>sip:rkd@example.com</PublicIdentity><PublicIdentity>sip:rkd3@example.com</PublicIdentity><PublicIdentity>tel:+123467890</PublicIdentity><InitialFilterCriteria><Priority>1</Priority><TriggerPoint><ConditionTypeCNF>0</ConditionTypeCNF><SPT><ConditionNegated>0</ConditionNegated><Group>0</Group><Method>INVITE</Method></SPT></TriggerPoint><ApplicationServer><ServerName>sip:vpn@192.168.1.139:5060</ServerName><DefaultHandling>0</DefaultHandling><Extension><ForceB2B></ForceB2B></Extension></ApplicationServer></InitialFilterCriteria></ServiceProfile><ServiceProfile><PublicIdentity>sip:rkd4@example.com</PublicIdentity><InitialFilterCriteria><Priority>1</Priority><TriggerPoint><ConditionTypeCNF>0</ConditionTypeCNF><SPT><ConditionNegated>0</ConditionNegated><Group>0</Group><Method>REGISTER</Method></SPT></TriggerPoint><ApplicationServer><ServerName>sip:voicemail@192.168.1.1:5060</ServerName><DefaultHandling>0</DefaultHandling></ApplicationServer></InitialFilterCriteria></ServiceProfile></IMSSubscription>")))

(deftest associations
  (is (data/associated? example-public "rkd@example.com"))
  (is (not (data/associated? example-public "someone-else@example.com"))))

(deftest correct-auth-scheme
  (is (data/correct-auth-scheme? example-private "Unknown"))
  (is (data/correct-auth-scheme? example-private "SIP Digest"))
  (is (not (data/correct-auth-scheme? example-private "Digest-AKAv1-MD5")))
  (is (data/correct-auth-scheme? example-private-aka "Digest-AKAv1-MD5"))
  (is (not (data/correct-auth-scheme? example-private-aka "SIP Digest")))
  (is (not (data/correct-auth-scheme? example-private-aka "Unknown"))))

(deftest locations
  (is (data/registered-elsewhere? example-public "example2.net"))
  (is (not (data/registered-elsewhere? example-public-unregistered "example2.net")))
  (is (not (data/registered-elsewhere? example-public "example.com")))
  (is (data/registered-here? example-public "example.com"))
  (is (not (data/registered-here? example-public-unregistered "example.com")))
  )

(deftest registered?
  (is (data/no-registrations? example-public-unregistered))
  (is (not (data/no-registrations? example-public))))
