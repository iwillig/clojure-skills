(ns clojure-skills.main
  "Main entry point for clojure-skills CLI."
  (:gen-class)
  (:require
   [clojure-skills.cli :as cli]))


(defn -main
  "Main entry point for the CLI."
  [& args]
  (cli/run-cli args))
