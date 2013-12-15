(ns org.leafhss.hss.autorespond
  (:require [digest :refer [md5]]))

(defn make-get-public [scscf visited-network-id]
  (fn get-public [impu]
    (let [impi (second (clojure.string/split impu #":"))]
      {:scscf-sip-uri scscf
       :scscf-diameter-host nil
       :scscf-diameter-realm nil
       :mandatory-capabilities []
       :optional-capabilities []
       :private-ids #{impi}
       :public-ids {impu {:barred false
                          :alias-group 1
                          :irs 1}}
       :roaming-networks #{visited-network-id}
       :alias-groups {1 {:ifcs "<ServiceProfile></ServiceProfile>"}}
       :registered-impis #{impi}
       :auth-pending-impis #{}})))

(defn make-get-private [realm fixed-password]
  (fn get-private [impi]
    {:auth-type "SIP Digest"
     :sip-digest {:realm realm :ha1 (md5 (str impi realm fixed-password))}}))

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
