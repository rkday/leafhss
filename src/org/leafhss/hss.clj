(ns org.leafhss.hss
  (:require [taoensso.timbre :as timbre]
            [clojure.tools.cli :refer [parse-opts]]
            [org.leafhss.hss.stack :as stack]
            [org.leafhss.hss.autorespond :refer :all]))

(timbre/refer-timbre)

(set! *warn-on-reflection* true)


(def cli-options
  [;; First three strings describe a short-option, long-option with optional
   ;; example argument description, and a description. All three are optional
   ;; and positional.
   [nil "--autoanswer" "Creates automatic responses to requests (for testing purposes) rather than using a database"]
   [nil "--scscf-sip-uri <URI>" "SIP URI of S-CSCF, used in the Server-Name AVP"
    :default "sip:example.com:5060"]
   [nil "--visited-network-id <ID>" "Name of the home network, used in the Visited-Network AVP"
    :default "example.com"]
   [nil "--digest-realm <REALM>" "Name of the realm, used for calculating the SIP digest"
    :default "example.com"]
   [nil "--standard-password <PASSWORD>" "Standard password used for calculating the SIP digest for all users"
    :default "swordfish"]
   ["-h" "--help"]])

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn usage [options-summary]
  (->> ["Leaf HSS: a simple, modern Home Subscriber Server for IMS networks."
        ""
        "Usage: leafhss [options]"
        ""
        "Options:"
        options-summary
        ""
        ]
       (clojure.string/join \newline)))

(defn -main [& args]
  (let [{:keys [options summary]} (parse-opts args cli-options)]
    (cond
     (:help options) (exit 0 (usage summary)))
    (println "Options are: " (str options))
    (let [[stack session-factory]
           (stack/make-stack (make-get-public (:scscf-sip-uri options) (:visited-network-id options))
                             (make-get-private (:digest-realm options) (:standard-password options))
                             update-scscf!
                             clear-scscf!
                             register!
                             set-auth-pending!
                             unregister!
                             update-aka-seqn!
                             set-scscf-reassignment-pending!)]
      (Thread/sleep 3600000)
      (.destroy stack))))

(comment (let [session (.getNewSession session-factory)
                   req (.createRequest session 303 (ApplicationId/createByAuthAppId 10415 167772151) "ims.hpintelco.org")
                   avps (.getAvps req)]
               (.addAvp avps 2 6 true true true)
               (.send session req nil)))
