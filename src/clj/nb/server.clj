(ns nb.server
  (:require [clojure.core.async :as async]
            [compojure.core :as c]
            [compojure.route :as route]
            [org.httpkit.server :as http]
            ring.middleware.anti-forgery
            ring.middleware.defaults
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :as sente-http-kit]))

(defonce socket
  (sente/make-channel-socket! (sente-http-kit/get-sch-adapter)
                              {:user-id-fn (fn [req]
                                             (:user (:event req)))}))

(c/defroutes routes
  (c/GET "/elmyr" req (force ring.middleware.anti-forgery/*anti-forgery-token*))
  (c/GET "/chsk" req ((:ajax-get-or-ws-handshake-fn socket) req))
  (c/POST "/chsk" req ((:ajax-post-fn socket) req))
  (route/not-found "This is not a page."))

(def app
  (ring.middleware.defaults/wrap-defaults
   routes
   ring.middleware.defaults/site-defaults))

(defonce ^:private stop-server-fn (atom nil))
(defonce ^:private stop-router-fn (atom nil))

(defn start-router! [dispatch]
  (when (fn? @stop-router-fn)
    (@stop-router-fn))
  (reset! stop-router-fn
          (sente/start-server-chsk-router!
           (:ch-recv socket)
           (fn [msg]
             (dispatch (dissoc msg :ring-req :ch-recv))))))

(defn start-server! [port]
  (when (fn? @stop-server-fn)
    (@stop-server-fn))
  (reset! stop-server-fn (http/run-server #'app {:port port}))
  (println "Server listening on port:" port))

(defn init-socket-server! [port router-dispatch-fn]
  (start-server! port)
  (start-router! router-dispatch-fn))

(defn stop-socket-server! []
  (when @stop-router-fn
    (@stop-router-fn))
  (when @stop-server-fn
    (@stop-server-fn)))
