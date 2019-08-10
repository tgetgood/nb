(ns nb.core
  (:require [nb.server :as server]))

(defn main [& args]
  (server/init-socket-server! 8080 println))
