(ns ^:figwheel-hooks nb.core
  (:require [nb.config :as config]
            [nb.events :as events]
            [nb.socket :as socket]
            [nb.views :as views]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn ^:after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
