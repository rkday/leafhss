(ns org.leafhss.hss.riak
    (:require [clojure.tools.logging :refer [info debug error]]
            [clojurewerkz.welle.core :as wc]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.kv :as kv]
            [clojurewerkz.welle.links :refer [walk start-at step]]
            )
    (:import com.basho.riak.client.http.util.Constants))

;; connects to a Riak node at http://127.0.0.1:8098/riak
(wc/connect!)
(wb/create "subscriptions")
(wb/create "subscription-state")
(wb/create "irs")
(wb/create "impis")
(wb/create "impus")
(kv/store "subscription-state" "1"
          {:scscf-sip-uri nil
           :scscf-diameter-host nil
           :scscf-diameter-realm nil}
          :content-type "application/clojure")
(kv/store "subscriptions" "1"
          {:mandatory-capabilities [1 2 3]
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
           :implicit-registration-sets {1 :private-id nil
                                        2 :private-id "rkd@example.com"}
           :alias-groups {6 {:ifcs "<ServiceProfile><InitialFilterCriteria><Priority>1</Priority><TriggerPoint><ConditionTypeCNF>0</ConditionTypeCNF><SPT><ConditionNegated>0</ConditionNegated><Group>0</Group><Method>INVITE</Method></SPT></TriggerPoint><ApplicationServer><ServerName>sip:vpn@192.168.1.139:5060</ServerName><DefaultHandling>0</DefaultHandling><Extension><ForceB2B/></Extension></ApplicationServer></InitialFilterCriteria></ServiceProfile>"}
                          7 {:ifcs "<ServiceProfile><InitialFilterCriteria><Priority>1</Priority><TriggerPoint><ConditionTypeCNF>0</ConditionTypeCNF><SPT><ConditionNegated>0</ConditionNegated><Group>0</Group><Method>REGISTER</Method></SPT></TriggerPoint><ApplicationServer><ServerName>sip:voicemail@192.168.1.1:5060</ServerName><DefaultHandling>0</DefaultHandling></ApplicationServer></InitialFilterCriteria></ServiceProfile>"}
                          8 {:ifcs "<ServiceProfile></ServiceProfile>"}}}
          :content-type "application/clojure"
          :links [{:bucket "subscription-state" :key "1" :tag "state"}])
(kv/store "irs" "1" {:registered-impis #{}
                     :auth-pending-impis #{}}
          :content-type "application/clojure"
          :links [{:bucket "subscriptions" :key "1" :tag "sub"}])
(kv/store "irs" "2" {:registered-impis #{}
                     :auth-pending-impis #{}}
          :content-type "application/clojure"
          :links [{:bucket "subscriptions" :key "1" :tag "sub"}])
(kv/store "impus" "sip:rkd@example.com" {}
          :content-type "application/clojure"
          :links [{:bucket "irs" :key "1" :tag "irs"}])
(kv/store "impus" "tel:+1234567890" {}
          :content-type "application/clojure"
          :links [{:bucket "irs" :key "1" :tag "irs"}])
(kv/store "impus" "sip:rkd3@example.com" {}
          :content-type "application/clojure"
          :links [{:bucket "irs" :key "1" :tag "irs"}])
(kv/store "impus" "sip:rkd4@example.com" {}
          :content-type "application/clojure"
          :links [{:bucket "irs" :key "1" :tag "irs"}])
(kv/store "impus" "sip:rkd5@example.com" {}
          :content-type "application/clojure"
          :links [{:bucket "irs" :key "2" :tag "irs"}])
(kv/store "impis" "rkd@example.com" {:auth-type "SIP Digest"
                                     :sip-digest {:realm "example.com" :ha1 "secret"}}
          :content-type "application/clojure"
          :links [{:bucket "subscriptions" :key "1" :tag "sub"}])

(kv/store "impis" "rkdaka@example.com" {:auth-type "AKA-MD5-v1"
                                        :aka {:key "12345" :seqn 1}}
          :content-type "application/clojure"
          :links [{:bucket "subscriptions" :key "1" :tag "sub"}])

(defn get-public [impu]
  (let [riakdata (walk
                  (start-at "impus" impu)
                  (step "irs" "irs" true)
                  (step "subscriptions" "sub" true)
                  (step "subscription-state" "state" true))
        subscription-state-key ((comp :key first :links first second) riakdata)]
    (reduce merge
            {:subscription-state-key subscription-state-key}
            (map (comp :value first) riakdata))))

(defn get-public-no-lw [impu]
  (let [impuobj (kv/fetch-one "impus" impu)
        irsnum ((comp :key first :links :result) impuobj)
        irsobj (kv/fetch-one "irs" irsnum)
        subnum ((comp :key first :links :result) irsobj)
        subobj (kv/fetch-one "subscriptions" subnum)
        substateobj (kv/fetch-one "subscription-state" subnum)]
    (reduce merge
            (map (comp :value :result)
                 [irsobj subobj substateobj]))))



(defn get-private [impi]
  ((comp :value :result) (kv/fetch-one "impis" impi)))

(defn update-scscf! [current-state sipuri diameter-host diameter-realm]
  (debug "Setting S-CSCF name to " sipuri)
  (let [state-key (:subscription-state-key current-state)]
    (kv/modify "subscription-state" state-key
               (fn [_]
                 {:scscf-sip-uri sipuri
                  :scscf-diameter-host diameter-host
                  :scscf-diameter-realm diameter-realm}))))

(defn clear-scscf! [current-state]
  (update-scscf! current-state nil nil nil))

(defn update-aka-seqn! [impi]
  (debug "Updating AKA sequence number for " impi)
  (kv/modify "impis" impi (fn [riak-result]
                              (if (get-in riak-result [:aka :seqn])
                                (update-in riak-result [:aka :seqn] inc)
                                riak-result))))

(defn register! [current-state impi impu]
  (debug "Setting state to registered and clearing authentication-pending flag for " impi " and " impu)
  (let [irs (:irs (get (:public-ids current-state) impu))]
    (kv/modify "irs" irs
               (fn [{:keys [registered-impis
                           auth-pending-impis]}]
                 {:registered-impis (conj registered-impis impi)
                  :auth-pending-impis (disj auth-pending-impis impi)}))))

(defn set-auth-pending! [current-state impi impu]
  (debug "Setting authentication-pending flag for " impi " and " impu)
  (let [irs (:irs (get (:public-ids current-state) impu))]
    (kv/modify "irs" irs
               (fn [{:keys [registered-impis
                           auth-pending-impis]}]
                 {:registered-impis registered-impis
                  :auth-pending-impis (conj auth-pending-impis impi)}))))

(defn unregister! [current-state impi impu]
  (debug "Clearing registration for " impi " and " impu)
  (let [irs (:irs (get (:public-ids current-state) impu))]
    (kv/modify "irs" irs
               (fn [{:keys [registered-impis
                           auth-pending-impis]}]
                 {:registered-impis (disj registered-impis impi)
                  :auth-pending-impis (disj auth-pending-impis impi)}))))

(comment defn add-new
         ([impi realm password ifcs]
            (let [impu (strip impi)
                  irs-uuid (new-uuid)
                  subscription-uuid (new-uuid)]
              (kv/store "subscriptions" subscription-uuid
                        {:mandatory-capabilities []
                         :optional-capabilities []
                         :private-ids #{impi}
                         :public-ids {impu {:barred false
                                            :alias-group 1
                                            :irs irs-uuid}}
                         :roaming-networks #{"example.com"}
                         :implicit-registration-sets {irs-uuid {:private-id impu}}
                         :alias-groups {1 {:ifcs ifcs}}}
                        :content-type "application/clojure"
                        :links [{:bucket "subscription-state" :key subscription-uuid :tag "state"}]))
            (kv/store "impus" impu {}
                      :content-type "application/clojure"
                      :links [{:bucket "irs" :key irs-uuid :tag "irs"}])
            (kv/store "irs" irs-uuid {:registered-impis #{}
                                      :auth-pending-impis #{}}
                        :content-type "application/clojure"
                        :links [{:bucket "subscriptions" :key subscription-uuid :tag "sub"}])
            (kv/store "impis" impi {:auth-type "SIP Digest"
                                    :digest {:realm realm :ha1 (md5 (str (impi realm password)))}}
                      :content-type "application/clojure"
                      :links [{:bucket "subscriptions" :key subscription-uuid :tag "sub"}])))
