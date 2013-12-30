(ns org.leafhss.hss.common-test
  (:require [clojure.test :refer :all]
            [org.leafhss.hss.cx :refer :all]
            [org.leafhss.hss.data :as data]
            [org.leafhss.hss.testdata :refer :all]
            [cljito.core :refer :all])
  (:import org.jdiameter.api.Request
           org.jdiameter.api.Answer
           org.jdiameter.api.AvpSet
           org.jdiameter.api.Avp))

(def private-not-found-cx-listener (make-cx-listener
                  (get-public-with-scscf nil)
                  (constantly nil)
                  update-scscf!
                  clear-scscf!
                  register!
                  set-auth-pending!
                  unregister!
                  update-aka-seqn!
                  set-scscf-reassignment-pending!))

(defn cx-listener-from-public [public]
  (make-cx-listener
   (constantly public)
   get-private
   update-scscf!
   clear-scscf!
   register!
   set-auth-pending!
   unregister!
   update-aka-seqn!
   set-scscf-reassignment-pending!))

(def cx-listener (cx-listener-from-public example-public))
(def public-not-found-cx-listener (cx-listener-from-public nil))
(def all-barred-cx-listener (cx-listener-from-public example-public-all-barred))
(def one-barred-cx-listener (cx-listener-from-public example-public-one-barred))
(def optional-capabilities-cx-listener (cx-listener-from-public example-public-optional-capabilities))
(def no-capabilities-cx-listener (cx-listener-from-public example-public-no-capabilities))
(def unregistered-cx-listener (cx-listener-from-public example-public-unregistered))

(defn create-mock-answer []
  (let [mockset1 (when->
                 (mock AvpSet)
                 (.addGroupedAvp (any-int) (any-int) (any-boolean) (any-boolean))
                 (.thenReturn (mock AvpSet)))
        mockset (when->
                 (mock AvpSet)
                 (.addGroupedAvp (any-int) (any-int) (any-boolean) (any-boolean))
                 (.thenReturn mockset1))
        ans (mock Answer)]
    (when->
     ans
     (.getAvps)
     (.thenReturn mockset))
    ans))

(def mock-ok-answer (create-mock-answer))
(def mock-rejected-answer (create-mock-answer))
(def mock-cant-comply-answer (create-mock-answer))

(def mock-first-reg-answer (create-mock-answer))
(def mock-subsequent-reg-answer (create-mock-answer))
(def mock-unregistered-service-answer (create-mock-answer))
(def mock-server-name-not-stored-answer (create-mock-answer))

(def mock-unknown-answer (create-mock-answer))
(def mock-disassoc-answer (create-mock-answer))
(def mock-unregistered-answer (create-mock-answer))
(def mock-roaming-not-allowed-answer (create-mock-answer))
(def mock-already-registered-answer (create-mock-answer))
(def mock-bad-auth-answer (create-mock-answer))

(defn make-mock [code]
  (let [mocked (mock Request)]
    (when->
     mocked
     (.getCommandCode)
     (.thenReturn (int code)))
    (when-> mocked (.createAnswer 2001) (.thenReturn mock-ok-answer))
    (when-> mocked (.createAnswer 4001) (.thenReturn mock-rejected-answer))
    (when-> mocked (.createAnswer 5012) (.thenReturn mock-cant-comply-answer))

    (when-> mocked (.createAnswer 10415 2001) (.thenReturn mock-ok-answer))
    (when-> mocked (.createAnswer 10415 2002) (.thenReturn mock-ok-answer))
    (when-> mocked (.createAnswer 10415 2003) (.thenReturn mock-unregistered-service-answer))
    (when-> mocked (.createAnswer 10415 2004) (.thenReturn mock-server-name-not-stored-answer))

    (when-> mocked (.createAnswer 10415 5001) (.thenReturn mock-unknown-answer))
    (when-> mocked (.createAnswer 10415 5002) (.thenReturn mock-disassoc-answer))
    (when-> mocked (.createAnswer 10415 5003) (.thenReturn mock-unregistered-answer))
    (when-> mocked (.createAnswer 10415 5004) (.thenReturn mock-roaming-not-allowed-answer))
    (when-> mocked (.createAnswer 10415 5005) (.thenReturn mock-already-registered-answer))
    (when-> mocked (.createAnswer 10415 5006) (.thenReturn mock-bad-auth-answer))))



