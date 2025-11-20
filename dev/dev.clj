(ns dev
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.string :as str]
   [clojure.tools.namespace.repl :as repl]
   [kaocha.repl :as k]))

(defn refresh []
  (repl/refresh))

(defn lint
  "Lint the entire project (src and test directories)."
  []
  (-> (clj-kondo/run! {:lint ["src" "test"]})
      clj-kondo/print!))

(defn lint-summary
  "Lint the project and return summary statistics."
  []
  (-> (clj-kondo/run! {:lint ["src" "test"]})
      :summary))

(defn lint-file
  "Lint a specific file or directory.

  Example: (lint-file \"src/clojure_skills/main.clj\")"
  [path]
  (-> (clj-kondo/run! {:lint [path]})
      clj-kondo/print!))

(comment

  ;; Kaocha test functions
  (k/run-all)
  (k/run 'test-namespace)
  (k/run 'test-namespace/deftest-var)

  ;; clj-kondo linting functions
  (lint) ; Lint entire project
  (lint-summary) ; Get summary only
  (lint-file "src/clojure_skills/main.clj") ; Lint specific file
  (lint-ns 'clojure-skills.main) ; Lint specific namespace

  ;; Reload code after changes
  (refresh))
