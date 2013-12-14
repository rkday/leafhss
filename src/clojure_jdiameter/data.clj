(ns clojure-jdiameter.data
    (:require [clojure.data.xml :as xml]))

(defn get-service-profiles [impu public-data]
  (let [this-irs (:irs (get (:public-ids public-data) impu))
        username #(first (clojure.string/split % #"@"))
        alias-group-num-is? #(fn [[id id-data]]
                             (and (= (:irs id-data) this-irs)
                                  (= (:alias-group id-data) %)))
        associate-public-ids-with-alias-group (fn [[alias-group-num alias-group-data]]
                                                (assoc alias-group-data :public-ids
                                                       (keys (filter (alias-group-num-is? alias-group-num)
                                                                     (:public-ids public-data)))))]
    (->>
     (map associate-public-ids-with-alias-group (:alias-groups public-data))
     (filter :public-ids) ;; exclude service profiles used by no
     ;; public IDs in this IRS

     ; This sets up the consistent ordering used to determine the
     ; default public ID - it's the SIP URI with the user part which
     ; sorts lexicographically first
     (map #(assoc % :public-ids (sort-by username (:public-ids %))))
     (sort-by #(username (first (:public-ids %)))))))

(defn get-userdata [impu public-data]
  (let [sps (get-service-profiles impu public-data)
        to-xml (fn [sp]
                 (xml/element :ServiceProfile {}
                              (concat
                               (map #(xml/element :PublicIdentity {} %) (:public-ids sp))
                               (:content (xml/parse (java.io.StringReader. (:ifcs sp)))))))]
    (xml/emit-str
     (xml/element :IMSSubscription {}
                  (list (xml/element :PrivateID {} (first (:private-ids public-data)))
                        (map to-xml sps))))))

(defn associated? [current-state impi]
  (contains? (:private-ids current-state) impi))

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

