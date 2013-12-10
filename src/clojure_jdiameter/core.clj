(ns clojure-jdiameter.core
  (:require [taoensso.timbre :as timbre]
            [clojure-jdiameter.stack :as stack]
            [clojure-jdiameter.data :as data]))

(timbre/refer-timbre)

(set! *warn-on-reflection* true)

(defn -main []
  (let [[stack session-factory] (stack/make-stack data/get-public-data (constantly nil) data/get-private-data (constantly nil))]
    (Thread/sleep 60000)
    (comment (let [session (.getNewSession session-factory)
                   req (.createRequest session 303 (ApplicationId/createByAuthAppId 10415 167772151) "ims.hpintelco.org")
                   avps (.getAvps req)]
               (.addAvp avps 2 6 true true true)
               (.send session req nil)))
    (.destroy stack)))
