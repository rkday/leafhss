(ns org.leafhss.hss.stack
  (:require [taoensso.timbre :as timbre]
            [org.leafhss.hss.cx :as cx])
  (:import org.jdiameter.server.impl.StackImpl
           org.jdiameter.server.impl.helpers.XMLConfiguration
           org.jdiameter.client.impl.parser.AvpSetImpl
           org.jdiameter.api.NetworkReqListener
           org.jdiameter.api.Network
           org.jdiameter.api.ApplicationId))

(defn make-stack [get-public
                  get-private
                  update-scscf!
                  clear-scscf!
                  register!
                  set-auth-pending!
                  unregister!
                  update-aka-seqn!
                  set-scscf-reassignment-pending!]
  (let [stack  (new ^org.jdiameter.server.impl.StackImpl StackImpl)
        config (new XMLConfiguration "resources/config.xml")
        session-factory (.init stack config)
        nw (.unwrap ^org.jdiameter.server.impl.StackImpl stack Network)
        apps [(ApplicationId/createByAuthAppId 10415 16777216)
              (ApplicationId/createByAuthAppId 0 16777216)]]
    (.addNetworkReqListener
     ^org.jdiameter.server.impl.NetworkImpl nw
     ^NetworkReqListener (cx/make-cx-listener get-public
                                              get-private
                                              update-scscf!
                                              clear-scscf!
                                              register!
                                              set-auth-pending!
                                              unregister!
                                              update-aka-seqn!
                                              set-scscf-reassignment-pending!)
     (into-array ApplicationId apps))
    (.start stack)
    [stack session-factory]))

