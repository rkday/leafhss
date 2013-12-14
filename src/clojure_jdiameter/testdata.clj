(ns clojure-jdiameter.testdata)

(defn get-public [impu]
  {:scscf-sip-uri nil
   :scscf-diameter-host nil
   :scscf-diameter-realm nil
   :mandatory-capabilities [1 2 3]
   :optional-capabilities [4 5 6]
   :private-ids #{"rkd@example.com"}
   :public-ids {"sip:rkd@example.com" {:barred false
                                       :alias-group 6
                                       :irs 1}
                "tel:+123467890" {:barred false
                                  :alias-group 6
                                  :irs 1}
                "sip:rkd3@example.com" {:barred false
                                        :alias-group 6
                                        :irs 1}
                "sip:rkd4@example.com" {:barred false
                                        :alias-group 7
                                        :irs 1}
                "sip:rkd5@example.com" {:barred false
                                        :alias-group 8
                                        :irs 2}}
   :roaming-networks #{"example.com"}
   :alias-groups {6 {:ifcs "<ServiceProfile><InitialFilterCriteria><Priority>1</Priority><TriggerPoint><ConditionTypeCNF>0</ConditionTypeCNF><SPT><ConditionNegated>0</ConditionNegated><Group>0</Group><Method>INVITE</Method></SPT></TriggerPoint><ApplicationServer><ServerName>sip:vpn@192.168.1.139:5060</ServerName><DefaultHandling>0</DefaultHandling><Extension><ForceB2B/></Extension></ApplicationServer></InitialFilterCriteria></ServiceProfile>"}
                  7 {:ifcs "<ServiceProfile><InitialFilterCriteria><Priority>1</Priority><TriggerPoint><ConditionTypeCNF>0</ConditionTypeCNF><SPT><ConditionNegated>0</ConditionNegated><Group>0</Group><Method>REGISTER</Method></SPT></TriggerPoint><ApplicationServer><ServerName>sip:voicemail@192.168.1.1:5060</ServerName><DefaultHandling>0</DefaultHandling></ApplicationServer></InitialFilterCriteria></ServiceProfile>"}
                  8 {:ifcs "<ServiceProfile></ServiceProfile>"}}
   :registered-impis #{}
   :auth-pending-impis #{}
                  }
  )

(defn get-public-with-scscf [scscf]
  (fn [impu]
    (assoc (get-public impu) :scscf-sip-uri scscf)))

(defn get-private [impi]
  {:auth-type "SIP Digest"
   :sip-digest {:realm "example.com" :ha1 "secret"}})

(defn clear-scscf! [current-state]
  current-state)

(defn update-scscf! [current-state sipuri diameter-host diameter-realm]
  current-state)

(defn update-aka-seqn! [impi]
  nil)

(defn register! [current-state impi impu]
  current-state)

(defn set-auth-pending! [current-state impi impu]
  current-state)

(defn unregister! [current-state impi impu]
  current-state)

(defn set-scscf-reassignment-pending! [current-state]
  current-state)
