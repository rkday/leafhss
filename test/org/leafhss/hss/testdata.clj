(ns org.leafhss.hss.testdata)

(def example-public
  {:scscf-sip-uri "example.com"
   :scscf-diameter-host nil
   :scscf-diameter-realm nil
   :mandatory-capabilities [1 2 3]
   :optional-capabilities [4 5 6]
   :ccfs ["aaa://host.example.com:6666;transport=tcp;protocol=diameter" "aaa://host2.example.com:6666;transport=tcp;protocol=diameter"]
   :ecfs ["aaa://host.example.com:6666;transport=tcp;protocol=diameter" "aaa://host2.example.com:6666;transport=tcp;protocol=diameter"]
   :private-ids #{"rkd@example.com" "rkd2@example.com"}
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
   :implicit-registration-sets {1 :private-id nil
                                2 :private-id "rkd@example.com"}
   :alias-groups {6 {:ifcs "<ServiceProfile><InitialFilterCriteria><Priority>1</Priority><TriggerPoint><ConditionTypeCNF>0</ConditionTypeCNF><SPT><ConditionNegated>0</ConditionNegated><Group>0</Group><Method>INVITE</Method></SPT></TriggerPoint><ApplicationServer><ServerName>sip:vpn@192.168.1.139:5060</ServerName><DefaultHandling>0</DefaultHandling><Extension><ForceB2B/></Extension></ApplicationServer></InitialFilterCriteria></ServiceProfile>"}
                  7 {:ifcs "<ServiceProfile><InitialFilterCriteria><Priority>1</Priority><TriggerPoint><ConditionTypeCNF>0</ConditionTypeCNF><SPT><ConditionNegated>0</ConditionNegated><Group>0</Group><Method>REGISTER</Method></SPT></TriggerPoint><ApplicationServer><ServerName>sip:voicemail@192.168.1.1:5060</ServerName><DefaultHandling>0</DefaultHandling></ApplicationServer></InitialFilterCriteria></ServiceProfile>"}
                  8 {:ifcs "<ServiceProfile></ServiceProfile>"}}
   :registered-impis #{"rkd@example.com"}
   :auth-pending-impis #{}
                  }
  )
(def example-public-unregistered (-> example-public
                                     (assoc :scscf-sip-uri nil)
                                     (assoc :registered-impis #{})))

(def example-public-one-barred
    (-> example-public
      (assoc :public-ids {"sip:rkd@example.com" {:barred false
                                                 :alias-group 6
                                                 :irs 1}
                          "sip:rkd3@example.com" {:barred true
                                                  :alias-group 6
                                                  :irs 1}})))


(def example-public-all-barred
    (-> example-public
      (assoc :public-ids {"sip:rkd@example.com" {:barred true
                                                 :alias-group 6
                                                 :irs 1}
                          "sip:rkd3@example.com" {:barred true
                                                  :alias-group 6
                                                  :irs 1}})))

(def example-public-no-capabilities
  (-> example-public
      (assoc :mandatory-capabilities [])
      (assoc :optional-capabilities [])))


(def example-public-optional-capabilities
  (-> example-public
      (assoc :mandatory-capabilities [])))


(defn get-public [impu]
  example-public)

(defn get-public-with-scscf [scscf]
  (fn [impu]
    (assoc (get-public impu) :scscf-sip-uri scscf)))

(def example-private {:auth-type "SIP Digest"
   :sip-digest {:realm "example.com" :ha1 "secret"}})

(def example-private-aka {:auth-type "Digest-AKAv1-MD5"
   :aka {:key "secret" :seqn 6}})


(defn get-private [impi]
  example-private)

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
  (let [updater (fn [{:keys [registered-impis
                           auth-pending-impis]}]
                 {:registered-impis (disj registered-impis impi)
                  :auth-pending-impis (disj auth-pending-impis impi)})
        updates (updater current-state)]
    (merge current-state updates)))

(defn set-scscf-reassignment-pending! [current-state]
  current-state)
