(ns clojure-jdiameter.core
  (:import org.jdiameter.server.impl.StackImpl
           org.jdiameter.server.impl.helpers.XMLConfiguration
           org.jdiameter.api.NetworkReqListener
           org.jdiameter.server.impl.NetworkImpl
           org.jdiameter.api.OverloadManager
           org.jdiameter.api.Network
           org.jdiameter.server.impl.app.cxdx.CxDxServerSessionImpl
           org.jdiameter.api.ApplicationId))

(set! *warn-on-reflection* true)

(def x (atom nil))

(def print-listener
  (proxy [NetworkReqListener] []
    (processRequest [^org.jdiameter.api.Request r]
      (let [resp (.createAnswer r 2001)
            avps (.getAvps resp)]
        (.addAvp avps 606 "hello world" true)
        resp))))

(defn foo
  "I don't do a whole lot."
  [x]
  (let [stack  (new ^org.jdiameter.server.impl.StackImpl StackImpl)
        config (new XMLConfiguration "resources/config.xml")
        session-factory (.init stack config)
        session (.getNewSession session-factory)
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
     nw
     print-listener
     (into-array ApplicationId apps))
    (.start stack)
    (Thread/sleep 30000)
    (.destroy stack)))

{:type :response
 :app-id 16777216
 :avps [{:code 606 :value "hello world" :type :octetstring}
        {:code 616 :value [{:code 606 :value "hello world" :type :octetstring}] :type :grouped}]}

(comment (defn loop-through-avps [m avpset]
           (let [{:keys [code value type]} (first m)]
             (if (map? value)
               (.addAvp (loop-through-avps value ))
               (.addAvp avpset (create-avp code value type))))
           (if (empty? m)
             nil
             (recur (rest m)))))
