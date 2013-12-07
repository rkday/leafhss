(ns clojure-jdiameter.core
  (:require [taoensso.timbre :as timbre])
  (:import org.jdiameter.server.impl.StackImpl
           org.jdiameter.server.impl.helpers.XMLConfiguration
           org.jdiameter.client.impl.parser.AvpSetImpl
           org.jdiameter.api.NetworkReqListener
           org.jdiameter.server.impl.NetworkImpl
           org.jdiameter.api.OverloadManager
           org.jdiameter.api.Network
           org.jdiameter.server.impl.app.cxdx.CxDxServerSessionImpl
           org.jdiameter.api.ApplicationId))

(timbre/refer-timbre)

(def THREEGPP 10415)
(def UNKNOWN 5001)
(def IDENTITIES_DONT_MATCH 5002)
(def BAD_AUTH_SCHEME 5006)


(set! *warn-on-reflection* true)

(def x (atom nil))

(defn registered? [pub]
  (= :registered (:state pub)))

(defn set-scscf-flags [name pub priv]
  (let [new (-> pub
                (#(if (or (not (registered? %)) (not= name (:scscf %)))
                    (assoc % :auth-pending (conj (:auth-pending %) priv))))
                (assoc :scscf name))]
    (debug "New value is" (str new))
    new))

(defn create-mar-response [^org.jdiameter.api.Request r priv ^String user-name ^String public-identity]
  (let [resp (.createAnswer r 2001)
        ^org.jdiameter.client.impl.parser.AvpSetImpl avps (.getAvps resp)
        sip-auth-data (.addGroupedAvp avps 612 10415 true false)
        sip-digest-auth (.addGroupedAvp sip-auth-data 635 10415 true false)]
    (.addAvp avps (int 1) user-name false) ;; SIP-Number-Auth-items
    (.addAvp avps (int 601) public-identity (long 10415) true false false) ;; SIP-Number-Auth-items
    (.addAvp avps (int 607) (long 1) (long 10415) true false true) ;; SIP-Number-Auth-items
    (.addAvp sip-auth-data 608 "SIP-Digest" 10415 true false false) ;; SIP-Number-Auth-items
    (.addAvp sip-digest-auth 104 ^String (:digest-realm priv) false) ;; SIP-Number-Auth-items
    (.addAvp sip-digest-auth 111 "MD5" false) ;; SIP-Number-Auth-items
    (.addAvp sip-digest-auth 110 "auth" false) ;; SIP-Number-Auth-items
    (.addAvp sip-digest-auth 121 ^String (:digest-ha1 priv) false) ;; SIP-Number-Auth-items
    resp))

(defn create-3gpp-error-response [^org.jdiameter.api.Request r code]
  (let [resp (.createAnswer r code THREEGPP)]
    resp))

(defn process-mar [^org.jdiameter.api.Request r]
  (let [avps (.getAvps r)
        user-name (.getUTF8String (.getAvp avps 1))
        public-identity (.getUTF8String (.getAvp avps 601 THREEGPP))
        server-name (.getUTF8String (.getAvp avps 602 THREEGPP))
        auth-scheme (-> avps
                        (.getAvp 612 THREEGPP)
                        (.getGrouped)
                        (.getAvp 608 THREEGPP)
                        (.getUTF8String))
        known-public {:scscf nil
                      :auth-pending #{}
                      :state :unregistered
                      :private #{"rkd"}}
        known-private {:scheme "SIP Digest"
                      :digest-ha1 "secret"
                      :digest-realm "example.com"}]
    (cond
     (nil? known-private)
     (do (debug "Private ID not found")
         (create-3gpp-error-response r UNKNOWN))
     (nil? known-public)
     (do (debug "Public ID not found")
         (create-3gpp-error-response r UNKNOWN))
     (not (contains? (:private known-public) user-name))
     (do (debug "Public and private identies don't match")
         (create-3gpp-error-response r IDENTITIES_DONT_MATCH))
     (and (= "Unknown" auth-scheme)
          (not= "SIP-Digest"
                (:scheme known-private)))
     (do (debug "Auth scheme mismatch between" auth-scheme "and" (:scheme known-private))
         (create-3gpp-error-response r BAD_AUTH_SCHEME))
     (and (not= "Unknown" auth-scheme)
          (not= auth-scheme
                (:scheme known-private)))
     (do (debug "Auth scheme mismatch between" auth-scheme "and" (:scheme known-private))

         (create-3gpp-error-response r BAD_AUTH_SCHEME))
     :else (do
             (debug "Request OK!")
             (set-scscf-flags server-name known-public user-name)
             (create-mar-response r
                                  known-private
                                  user-name
                                  public-identity)))))

(def cx-listener
  (proxy [NetworkReqListener] []
    (processRequest [^org.jdiameter.api.Request r]
      (debug "cx-listener called")
      (reset! x r)
      (process-mar r))))

(defn make-stack []
  (let [stack  (new ^org.jdiameter.server.impl.StackImpl StackImpl)
        config (new XMLConfiguration "resources/config.xml")
        session-factory (.init stack config)
        nw (.unwrap ^org.jdiameter.server.impl.StackImpl stack Network)
        apps [(ApplicationId/createByAuthAppId 10415 16777216)
              (ApplicationId/createByAccAppId 10415 16777216)
              (ApplicationId/createByAuthAppId 4419 16777216)
              (ApplicationId/createByAccAppId 4419 16777216)
              (ApplicationId/createByAuthAppId 13019 16777216)
              (ApplicationId/createByAccAppId 13019 16777216)
              (ApplicationId/createByAuthAppId 0 16777216)
              (ApplicationId/createByAccAppId 0 16777216)]]
    (.addNetworkReqListener
     ^org.jdiameter.server.impl.NetworkImpl nw
     ^NetworkReqListener cx-listener
      (into-array ApplicationId apps))
    (.start stack)
    [stack session-factory]))

(comment (defonce diameter-instances (make-stack))

         (defonce diameter-stack (diameter-instances 0)))

(defn -main []
  (let [[stack session-factory] (make-stack)]
    (Thread/sleep 60000)
    (comment (let [session (.getNewSession session-factory)
                   req (.createRequest session 303 (ApplicationId/createByAuthAppId 10415 167772151) "ims.hpintelco.org")
                   avps (.getAvps req)]
               (.addAvp avps 2 6 true true true)
               (.send session req nil)))
    (.destroy stack)))
