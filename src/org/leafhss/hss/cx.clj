(ns org.leafhss.hss.cx
  (:require [clojure.tools.logging :refer [info debug error]]
            [org.leafhss.hss.constants :refer :all]
            [org.leafhss.hss.data :refer :all])
  (:import org.jdiameter.server.impl.StackImpl
           org.jdiameter.server.impl.helpers.XMLConfiguration
           org.jdiameter.client.impl.parser.AvpSetImpl
           org.jdiameter.api.NetworkReqListener
           org.jdiameter.api.Network
           org.jdiameter.api.ApplicationId))

(defn create-3gpp-error-response [^org.jdiameter.api.Request r code]
  (let [resp (.createAnswer r THREEGPP code)]
    resp))

(defn create-error-response [^org.jdiameter.api.Request r code]
  (let [resp (.createAnswer r code)]
    resp))

(defn populate-uaa-with-capabilities [^org.jdiameter.api.Answer resp mandatory-capabilities optional-capabilities]
  (if (and (empty? mandatory-capabilities)
           (empty? optional-capabilities))
    resp
    (let [^org.jdiameter.client.impl.parser.AvpSetImpl avps (.getAvps resp)
          server-capabilities (.addGroupedAvp avps 603 10415 true false)]
      (doseq [mandatory-capability mandatory-capabilities]
       (.addAvp server-capabilities (int 604) mandatory-capability 10415 true false true))
      (doseq [optional-capability optional-capabilities]
        (.addAvp server-capabilities (int 605) optional-capability 10415 true false true))
      resp)))

(defn create-uar-response [^org.jdiameter.api.Request r auth-type mandatory-capabilities optional-capabilities server-name]
  (cond
   (= auth-type REGISTRATION_AND_CAPABILITIES)
   (do (debug "Returning SUCCESS response with capabilities to UAR")
       (populate-uaa-with-capabilities (.createAnswer r 2001) mandatory-capabilities optional-capabilities))

   (and (nil? server-name) (= auth-type REGISTRATION))
   (do (debug "Returning FIRST_REGISTRATION response with capabilities to UAR")
       (populate-uaa-with-capabilities (.createAnswer r THREEGPP 2001) mandatory-capabilities optional-capabilities))

   (= auth-type REGISTRATION)
   (do (debug "Returning SUBSEQUENT_REGISTRATION response with Server-Name to UAR")
       (let [resp (.createAnswer r THREEGPP 2002)
             ^org.jdiameter.client.impl.parser.AvpSetImpl avps (.getAvps resp)]
         (.addAvp avps 602 server-name 10415 true false false)
         resp))

   (= auth-type DEREGISTRATION)
   (do (debug "Returning SUCCESS response with Server-Name to UAR")
       (let [resp (.createAnswer r 2001)
             ^org.jdiameter.client.impl.parser.AvpSetImpl avps (.getAvps resp)]
         (.addAvp avps 602 server-name 10415 true false false)
         resp))))

(defn create-sar-error [^org.jdiameter.api.Request r code threegpp pub]
  (let [resp (if threegpp
               (create-3gpp-error-response r code)
               (create-error-response r code))
        ^org.jdiameter.client.impl.parser.AvpSetImpl avps (.getAvps resp)]
    (if-let [priv (first (:private-ids pub))]
      (.addAvp avps 1 priv true false true))
    (when (and threegpp (= IDENTITY_ALREADY_REGISTERED code))
      (.addAvp avps 602 (:scscf-sip-uri pub) THREEGPP true false true))
    resp))

(defn add-ccfs! [^org.jdiameter.client.impl.parser.AvpSetImpl avps pub]
  (let [charging-information (.addGroupedAvp avps 618 THREEGPP true false)
        ecf1 (first (:ecfs pub))
        ecf2 (second (:ecfs pub))
        ccf1 (first (:ccfs pub))
        ccf2 (second (:ccfs pub))]
    (when ecf1
      (.addAvp charging-information 619 ecf1 THREEGPP true false false))
    (when ecf2
      (.addAvp charging-information 620 ecf2 THREEGPP true false false))
    (when ccf1
      (.addAvp charging-information 621 ccf1 THREEGPP true false false))
    (when ccf2
      (.addAvp charging-information 622 ccf2 THREEGPP true false false))))

(defn send-userdata? [sat]
  (contains? #{SAR_REGISTRATION SAR_RE_REGISTRATION SAR_NO_ASSIGNMENT SAR_UNREGISTERED_USER} sat))

(defn create-sar-response [^org.jdiameter.api.Request r pub impu server-assignment-type user-data-already-available]
  (debug "create-sar-response called")
  (let [resp (.createAnswer r 2001)
        ^org.jdiameter.client.impl.parser.AvpSetImpl avps (.getAvps resp)
        priv (first (:private-ids pub))]
    (.addAvp avps 1 priv true false true)
    (when (and (= 0 user-data-already-available) (send-userdata? server-assignment-type))
      (debug "adding user data")
      (.addAvp avps 606 (get-userdata impu pub) THREEGPP true false true)
      (add-ccfs! avps pub))
    resp))

(defn create-lir-response [^org.jdiameter.api.Request r auth-type mandatory-capabilities optional-capabilities server-name]
  (cond
   (= auth-type REGISTRATION_AND_CAPABILITIES)
   (do (debug "Returning 2001 response with capabilities to LIR")
     (populate-uaa-with-capabilities (.createAnswer r 2001) mandatory-capabilities optional-capabilities))
   (nil? server-name)
   (do (debug "Returning 3GPP 2003 response with capabilities to LIR")
     (populate-uaa-with-capabilities (.createAnswer r THREEGPP 2003) mandatory-capabilities optional-capabilities))
   :else
   (do (debug "Returning 2001 response with Server-Name to LIR")
       (let [resp (.createAnswer r 2001)
             ^org.jdiameter.client.impl.parser.AvpSetImpl avps (.getAvps resp)]
         (.addAvp avps 602 server-name 10415 true false false)
         resp))))

(defn create-mar-response [^org.jdiameter.api.Request r priv ^String user-name ^String public-identity]
  (let [resp (.createAnswer r 2001)
        ^org.jdiameter.client.impl.parser.AvpSetImpl avps (.getAvps resp)
        sip-auth-data (.addGroupedAvp avps 612 10415 true false)
        sip-digest-auth (.addGroupedAvp sip-auth-data 635 10415 true false)]
    (.addAvp avps (int 1) user-name false) ;; SIP-Number-Auth-items
    (.addAvp avps (int 601) public-identity (long 10415) true false false) ;; SIP-Number-Auth-items
    (.addAvp avps (int 607) (long 1) (long 10415) true false true) ;; SIP-Number-Auth-items
    (.addAvp sip-auth-data 608 "SIP Digest" 10415 true false false) ;; SIP-Number-Auth-items
    (.addAvp sip-digest-auth 104 ^String (:realm (:sip-digest priv)) false) ;; SIP-Number-Auth-items
    (.addAvp sip-digest-auth 111 "MD5" false) ;; SIP-Number-Auth-items
    (.addAvp sip-digest-auth 110 "auth" false) ;; SIP-Number-Auth-items
    (.addAvp sip-digest-auth 121 ^String (:ha1 (:sip-digest priv)) false) ;; SIP-Number-Auth-items
    resp))

(defn process-uar [^org.jdiameter.api.Request r get-public get-private update-scscf! clear-scscf! register! set-auth-pending! unregister! update-aka-seqn! set-scscf-reassignment-pending!]
  (let [avps (.getAvps r)
        user-name (.getUTF8String (.getAvp avps 1))
        public-identity (.getUTF8String (.getAvp avps 601 THREEGPP))
        auth-type (.getInteger32 (.getAvp avps 623 THREEGPP))
        visited-network-id (.getUTF8String (.getAvp avps 600 THREEGPP))
        known-public (get-public public-identity)
        known-private (get-private user-name)]
    (cond
     (nil? known-private)
     (do (debug "Private ID not found")
         (create-3gpp-error-response r UNKNOWN))
     (nil? known-public)
     (do (debug "Public ID not found")
         (create-3gpp-error-response r UNKNOWN))
     (not (associated? known-public public-identity user-name))
     (do (debug "Public and private identies don't match")
         (create-3gpp-error-response r IDENTITIES_DONT_MATCH))
     (all-barred? known-public)
          (do (debug "All Public IDs are barred")
         (create-error-response r REJECTED))
          (and (= auth-type REGISTRATION)
               (not (contains?
                     (:roaming-networks known-public)
                     visited-network-id)))
          (do (debug "Visited network" visited-network-id "not in permitted network list"
                     (str (:roaming-networks known-public)))
         (create-3gpp-error-response r ROAMING_NOT_ALLOWED))
     (and (= auth-type DEREGISTRATION)
          (not (contains?
                (:auth-pending-impis known-public)
                user-name))
          (not (contains?
                (:registered-impis known-public)
                user-name)))
     (do (debug "Rejected attempt to de-register user who is not registered")
         (create-3gpp-error-response r IDENTITY_NOT_REGISTERED))
     :else (do
             (debug "Request OK!")
             (create-uar-response r
                                  auth-type
                                  (:mandatory-capabilities known-public)
                                  (:optional-capabilities known-public)
                                  (:scscf-sip-uri known-public))))))

(defn process-sar [^org.jdiameter.api.Request r get-public get-private update-scscf! clear-scscf! register! set-auth-pending! unregister! update-aka-seqn! set-scscf-reassignment-pending!]
  (let [avps (.getAvps r)
        user-name (if-let [avp (.getAvp avps 1)] (.getUTF8String avp) nil)
        origin-host (if-let [avp (.getAvp avps 1)] (.getUTF8String avp) nil)
        origin-realm (if-let [avp (.getAvp avps 1)] (.getUTF8String avp) nil)
        public-identity (if-let [avp (.getAvp avps 601 THREEGPP)] (.getUTF8String avp) nil)
        server-name (.getUTF8String (.getAvp avps 602 THREEGPP))
        server-assignment-type (.getInteger32 (.getAvp avps 614 THREEGPP))
        user-data-already-available (.getInteger32 (.getAvp avps 624 THREEGPP))
        known-public (get-public public-identity)
        known-private (get-private user-name)]
    (cond
     (nil? known-private)
     (do (debug "Private ID not found")
         (create-sar-error r UNKNOWN true known-public))
     (nil? known-public)
     (do (debug "Public ID not found")
         (create-sar-error r UNKNOWN true known-public))
     (not (associated? known-public public-identity user-name))
     (do (debug "Public and private identies don't match")
         (create-sar-error r IDENTITIES_DONT_MATCH true known-public))
     (and (= SAR_NO_ASSIGNMENT server-assignment-type)
          (not (registered-here? known-public server-name)))
     (do (debug "S-CSCF does not own this identity, unable to comply with NO_ASSIGNMENT request")
         (create-sar-error r UNABLE_TO_COMPLY false known-public))
     (registered-elsewhere? known-public server-name)
     (do (debug "Public identity already registered")
         (create-sar-error r IDENTITY_ALREADY_REGISTERED true known-public))
     :else
     (do
       (debug "Request OK!")
       (condp contains? server-assignment-type
         #{SAR_REGISTRATION SAR_RE_REGISTRATION}
         (do (update-scscf! known-public server-name origin-host origin-realm)
             (register! known-public user-name public-identity)
             (create-sar-response r known-public public-identity server-assignment-type user-data-already-available)
             )
         #{SAR_NO_ASSIGNMENT}
         (create-sar-response r known-public public-identity server-assignment-type user-data-already-available)
         #{SAR_UNREGISTERED_USER}
         (do
           (update-scscf! known-public server-name origin-host origin-realm)
           (create-sar-response r known-public public-identity server-assignment-type user-data-already-available))
         #{SAR_TIMEOUT_DEREGISTRATION
           SAR_USER_DEREGISTRATION
           SAR_DEREGISTRATION_TOO_MUCH_DATA
           SAR_ADMINISTRATIVE_DEREGISTRATION
           SAR_TIMEOUT_DEREGISTRATION_STORE_SERVER_NAME
           SAR_USER_DEREGISTRATION_STORE_SERVER_NAME}
         (let [new-state (unregister! known-public user-name public-identity)]
           (if (no-registrations? new-state)
             (clear-scscf! known-public))
           (create-sar-response r known-public public-identity server-assignment-type user-data-already-available)
           )
         #{SAR_AUTHENTICATION_FAILURE
           SAR_AUTHENTICATION_TIMEOUT}
         (let [new-state (unregister! known-public user-name public-identity)]
           (if (no-registrations? new-state)
             (clear-scscf! known-public))
           (create-sar-response r known-public public-identity server-assignment-type user-data-already-available)))))))

(defn process-lir [^org.jdiameter.api.Request r get-public get-private update-scscf! clear-scscf! register! set-auth-pending! unregister! update-aka-seqn! set-scscf-reassignment-pending!]
  (let [avps (.getAvps r)
        public-identity (.getUTF8String (.getAvp avps 601 THREEGPP))
        auth-type (if-let [avp (.getAvp avps 623 THREEGPP)] (.getInteger32 avp) 0)
        known-public (get-public public-identity)]
    (cond
     (nil? known-public)
     (do (debug "Public ID not found")
         (create-3gpp-error-response r UNKNOWN))
     :else (do
             (debug "Request OK!")
             (if (= auth-type REGISTRATION_AND_CAPABILITIES)
               (set-scscf-reassignment-pending! known-public))
             (create-lir-response r
                                  auth-type
                                  (:mandatory-capabilities known-public)
                                  (:optional-capabilities known-public)
                                  (:scscf-sip-uri known-public))))))

(defn process-mar [^org.jdiameter.api.Request r get-public get-private update-scscf! clear-scscf! register! set-auth-pending! unregister! update-aka-seqn! set-scscf-reassignment-pending!]
  (let [avps (.getAvps r)
        user-name (.getUTF8String (.getAvp avps 1))
        public-identity (.getUTF8String (.getAvp avps 601 THREEGPP))
        server-name (.getUTF8String (.getAvp avps 602 THREEGPP))
        auth-scheme (-> avps
                        (.getAvp 612 THREEGPP)
                        (.getGrouped)
                        (.getAvp 608 THREEGPP)
                        (.getUTF8String))
        known-public (get-public public-identity)
        known-private (get-private user-name)]
    (cond
     (nil? known-private)
     (do (debug "Private ID not found")
         (create-3gpp-error-response r UNKNOWN))
     (nil? known-public)
     (do (debug "Public ID not found")
         (create-3gpp-error-response r UNKNOWN))
     (not (associated? known-public public-identity user-name))
     (do (debug "Public and private identies don't match")
         (create-3gpp-error-response r IDENTITIES_DONT_MATCH))
     (not (correct-auth-scheme? known-private auth-scheme))
     (do (debug "Auth scheme mismatch between" auth-scheme "and" (:auth-type known-private))
         (create-3gpp-error-response r BAD_AUTH_SCHEME))
     :else (do
             (debug "Request OK!")
             (update-scscf! known-public server-name nil nil)
             (create-mar-response r
                                  known-private
                                  user-name
                                  public-identity)))))

(defn make-cx-listener [get-public
                        get-private
                        update-scscf!
                        clear-scscf!
                        register!
                        set-auth-pending!
                        unregister!
                        update-aka-seqn!
                        set-scscf-reassignment-pending!]
  (proxy [NetworkReqListener] []
      (processRequest [^org.jdiameter.api.Request r]
        (debug "cx-listener called")
        (let [args [r get-public get-private update-scscf! clear-scscf! register! set-auth-pending! unregister! update-aka-seqn! set-scscf-reassignment-pending!]]
          (case (.getCommandCode r)
            300 (apply process-uar args)
            301 (apply process-sar args)
            302 (apply process-lir args)
            303 (apply process-mar args))))))


