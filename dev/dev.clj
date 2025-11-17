(ns dev
  (:require
   [clj-reload.core :as reload]))

(defn refresh
  []
  (reload/reload))
