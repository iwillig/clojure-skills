(ns clojure-skills.output
  "Output formatting for clojure-skills CLI.
  Provides JSON output for all data while keeping status messages as formatted text."
  (:require
   [clojure.data.json :as json]))

(defn output-data
  "Output data as formatted JSON to stdout.
  Converts Clojure data structures to JSON with pretty printing.
  
  Example:
    (output-data {:type :skill-list
                  :count 3
                  :skills [{:id 1 :name \"malli\"}
                           {:id 2 :name \"next_jdbc\"}]})"
  [data]
  (json/pprint data))

(defn json-output
  "Alias for output-data for backward compatibility."
  [data]
  (output-data data))
