(ns org.leafhss.hss.data
    (:require [clojure.data.xml :as xml]))

(defn get-irs-for-impu [current-state impu]
  (:irs (get (:public-ids current-state) impu)))

(defn get-service-profiles [impu public-data]
  (let [this-irs (get-irs-for-impu public-data impu)
        username #(first (clojure.string/split (:uri %) #"@"))
        alias-group-num-is? #(fn [[id id-data]]
                             (and (= (:irs id-data) this-irs)
                                  (= (:alias-group id-data) %)))
        associate-public-ids-with-alias-group (fn [[alias-group-num alias-group-data]]
                                                (assoc alias-group-data :public-ids
                                                       (map (fn [[k v]] {:uri k :barred (:barred v)}) (filter (alias-group-num-is? alias-group-num)
                                                                     (:public-ids public-data)))))]
    (->>
     (map associate-public-ids-with-alias-group (:alias-groups public-data))
     (filter #(seq (:public-ids %))) ;; exclude service profiles used by no
     ;; public IDs in this IRS

     ; This sets up the consistent ordering used to determine the
     ; default public ID - it's the SIP URI with the user part which
     ; sorts lexicographically first
     (map #(assoc % :public-ids (sort-by username (:public-ids %))))
     (sort-by #(username (first (:public-ids %))))
     )))

(defn get-userdata [impu public-data]
  (let [sps (get-service-profiles impu public-data)
        to-xml (fn [sp]
                 (xml/element :ServiceProfile {}
                              (concat
                               (map #(xml/element :PublicIdentity {}
                                                  (list (xml/element :BarringIndication {}
                                                                 (if (:barred %) "1" "0"))
                                                    (xml/element :Identity {}
                                                                 (:uri %)))) (:public-ids sp))
                               (:content (xml/parse (java.io.StringReader. (:ifcs sp)))))))]
    (xml/emit-str
     (xml/element :IMSSubscription {}
                  (list (xml/element :PrivateID {} (first (:private-ids public-data)))
                        (map to-xml sps))))))

(defn associated? [current-state impu impi]
  (let [irs (get-irs-for-impu current-state impu)
        expected-impi (-> (:implicit-registration-sets current-state)
                          (get irs)
                          :private-id)]
    (if expected-impi
      (= impi expected-impi)
      ; this implicit registration set can be authenticated by any
      ; IMPI in this IMS subscription
      (contains? (:private-ids current-state) impi))))

(defn correct-auth-scheme? [known-private auth-scheme]
  (or (= auth-scheme
            (:auth-type known-private))
   (and (= "Unknown" auth-scheme)
        (= "SIP Digest"
           (:auth-type known-private)))))

(defn registered-elsewhere? [current-state server-name ]
  (and (:scscf-sip-uri current-state)
          (not= server-name (:scscf-sip-uri current-state))))

(defn registered-here? [current-state server-name ]
  (= server-name (:scscf-sip-uri current-state)))

(defn no-registrations? [current-state]
  (empty? (:registered-impis current-state)))

(defn all-barred? [current-state]
  (->> (:public-ids current-state)
       (map second)
       (map :barred)
       (every? true?)))
