(ns user)


(defn dev
  []
  (require 'dev)
  (in-ns 'dev))


(def fast-dev #'dev)
