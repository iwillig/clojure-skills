(ns dev
  (:require
   [clj-commons.pretty.repl :as pretty.repl]
   [clj-kondo.core :as clj-kondo]
   [clj-reload.core :as reload]
   [kaocha.repl :as k]))

(reload/init
  {:dirs ["src" "dev" "test"]})

(pretty.repl/install-pretty-exceptions)

(defn refresh []
  (reload/reload))

(defn lint
  "Lint the entire project (src and test directories)."
  []
  (-> (clj-kondo/run! {:lint ["src" "test"]})
      (clj-kondo/print!)))

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
      (clj-kondo/print!)))

(defn run-all
  []
  (k/run-all))

(comment

  ;; Kaocha test functions
  (k/run-all)
  (k/run 'test-namespace)
  (k/run 'test-namespace/deftest-var)

  ;; clj-kondo linting functions
  (lint) ; Lint entire project
  (lint-summary) ; Get summary only
  (lint-file "src/clojure_skills/main.clj") ; Lint specific file

  ;; Reload code after changes
  (refresh))
