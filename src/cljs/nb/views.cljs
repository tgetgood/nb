(ns nb.views
  (:require
   [re-frame.core :as re-frame]
   [nb.subs :as subs]
   ))

;;; TODO: interpreter definition
;; So I really have two phases planned. The first is to render static content in
;; the browser, where by static content, I mean the standard content of a
;; polyglot notebook, the languages involved can't all run in the browser and
;; even if they could the computational laod might be unreasonable for someone
;; who just wants to view your work.
;;;;
;; The second phase is to augments the notebook editor to write itself. Or, more
;; importantly, to enable the author to write arbitrary UI code and customise
;; the way readers experience their work.
;;;;
;; Any code written in phase 2 style will have to be in a language that runs in
;; the browser. In fact, for now it should all be cljs or js. Always focus on at
;; least two.

;;;;; Design
;;
;; The basic unit of authored code is the block. A block consists of an
;; interpreter definition and the code to be executed. The definition of an
;; interpreter is the language plus all dependencies. Basically each block is
;; sufficiently well defined to be pure. That is given the same inputs it will
;; produce the same output every time.
;;
;; N.B.: blocks have to be pure. They only take information as inputs and return
;; a value without any side effects. I'm thinking that blocks should be executed
;; in containers with no disk and no internet access to fascilitate catching
;; departures from purity. That might be excessive though.
(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:h1 "Hello from " @name]
     ]))
