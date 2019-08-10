(ns nb.socket
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async]
            [goog.net.XhrIo]
            [re-frame.core :as re-frame]
            [taoensso.sente :as sente]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; WS router
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti ch-handler
  "Dispatch events from server."
  :id)

(defmethod ch-handler :default
  [e]
  (println "Unknown server event:" e))

(defmethod ch-handler :chsk/state
  [_]
  "Connection state change.")

(defmethod ch-handler :chsk/handshake
  [_]
  "WS handshake.")

(defmethod ch-handler :chsk/recv
  [e]
  (re-frame/dispatch [::server-message e]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; connect to server
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 ::server-connection
 (fn [{:keys [db]} [_ chsk]]
   (let [pending (:request-queue db)]
     (merge {:db (assoc db :chsk chsk :request-queue [] :connecting? false)}
            (when (seq pending)
              {:dispatch-n (mapv (fn [ev] [::try-send ev]) pending)})))))

(let [connecting? (atom false)]
  (re-frame/reg-fx
   ::connect-chsk!
   (fn [_]
     (when-not @connecting?
       (reset! connecting? true)
       (println "Connecting to server...")
       (let [csrf-ch (async/promise-chan)]
         (goog.net.XhrIo/send "/elmyr"
                              (fn [e]
                                (->> e
                                     .-target
                                     .getResponseText
                                     (async/put! csrf-ch))))
         ;; TODO: timeout, retry, backoff.
         (go
           (let [token (async/<! csrf-ch)
                 chsk  (sente/make-channel-socket-client!
                        "/chsk" token {:type :auto})]
             ;; Wait for a message so that we know the channel is open.
             (async/<! (:ch-recv chsk))
             (reset! connecting? false)
             (sente/start-client-chsk-router! (:ch-recv chsk) ch-handler)
             ;; Use sync to prevent connection races.
             (re-frame/dispatch-sync [::server-connection chsk]))))))))
