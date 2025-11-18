(ns clojure-skills.config
  "Configuration management with XDG Base Directory compliance and project-specific overrides.

  Configuration priority:
  1. Environment variables
  2. Project config (.clojure-skills/config.edn)
  3. ~/.config/clojure-skills/config.edn
  4. Built-in defaults"
  (:require
   [clojure-skills.logging :as log]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import (java.io File)))

(set! *warn-on-reflection* true)

(defn get-home-dir
  "Get user's home directory."
  []
  (System/getProperty "user.home"))

(defn get-xdg-config-home
  "Get XDG config directory, defaulting to ~/.config"
  []
  (or (System/getenv "XDG_CONFIG_HOME")
      (str (get-home-dir) "/.config")))

(defn get-config-dir
  "Get clojure-skills config directory."
  []
  (str (get-xdg-config-home) "/clojure-skills"))

(defn expand-path
  "Expand ~ in paths to home directory."
  [path]
  (if (str/starts-with? path "~")
    (str/replace-first path "~" (get-home-dir))
    path))

(defn find-project-root
  "Find the project root directory by walking up from the current directory
  looking for project markers. Returns the path to the project root or nil
  if no project markers are found."
  ([]
   (find-project-root (.getAbsolutePath (java.io.File. "."))))
  ([current-path]
   (let [markers [".clojure-skills" "deps.edn" ".git"]]
     (loop [path (io/file current-path)]
       (when path
         (let [files (set (map #(.getName ^File %) (.listFiles ^File path)))]
           (if (some files markers)
             (.getAbsolutePath path)
             (recur (.getParentFile path)))))))))

(defn get-project-config-file-path
  "Get path to project config file if within a project."
  []
  (when-let [project-root (find-project-root)]
    (str project-root "/.clojure-skills/config.edn")))

(defn load-project-config
  "Load project configuration from file if it exists."
  []
  (let [config-path (get-project-config-file-path)]
    (when (and config-path (.exists (io/file config-path)))
      (try
        (edn/read-string (slurp config-path))
        (catch Exception e
          (log/log-warning "Failed to load project config file" :path config-path :error (.getMessage e))
          nil)))))

(defn ensure-project-config-dir
  "Ensure project config directory exists."
  []
  (when-let [project-root (find-project-root)]
    (let [config-dir (str project-root "/.clojure-skills")]
      (when-not (.exists (io/file config-dir))
        (.mkdirs (io/file config-dir))))))

(defn save-project-config
  "Save configuration to project config file."
  [config]
  (ensure-project-config-dir)
  (when-let [config-path (get-project-config-file-path)]
    (spit config-path (pr-str config))))

(def default-config
  "Default configuration values."
  {:database
   {:path         "~/.config/clojure-skills/clojure-skills.db"
    :auto-migrate true}

   :project
   {:root        nil
    :skills-dir  "skills"
    :prompts-dir "prompts"
    :build-dir   "_build"}

   :search
   {:max-results   50
    :context-lines 3}

   :output
   {:format :table
    :color  true}

   :permissions
   {:plan      false
    :task      false
    :task-list false
    :prompt    false}})

(defn get-config-file-path
  "Get path to config.edn file."
  []
  (str (get-config-dir) "/config.edn"))

(defn load-config-file
  "Load configuration from file if it exists."
  []
  (let [config-path (get-config-file-path)]
    (when (.exists (io/file config-path))
      (try
        (edn/read-string (slurp config-path))
        (catch Exception e
          (log/log-warning "Failed to load config file" :path config-path :error (.getMessage e))
          nil)))))

(defn get-env-overrides
  "Get configuration overrides from environment variables."
  []
  (cond-> {}
    (System/getenv "CLOJURE_SKILLS_DB_PATH")
    (assoc-in [:database :path] (System/getenv "CLOJURE_SKILLS_DB_PATH"))

    (System/getenv "CLOJURE_SKILLS_PROJECT_ROOT")
    (assoc-in [:project :root] (System/getenv "CLOJURE_SKILLS_PROJECT_ROOT"))))

(defn deep-merge
  "Recursively merge maps."
  [& maps]
  (apply merge-with
         (fn [v1 v2]
           (if (and (map? v1) (map? v2))
             (deep-merge v1 v2)
             v2))
         maps))

(defn load-config
  "Load configuration with priority:
   1. Environment variables
   2. Project config (.clojure-skills/config.edn)
   3. ~/.config/clojure-skills/config.edn
   4. Built-in defaults"
  []
  (let [file-config (load-config-file)
        project-config (load-project-config)
        env-config (get-env-overrides)]
    (deep-merge default-config file-config project-config env-config)))

(defn get-db-path
  "Get database path with expansion."
  [config]
  (expand-path (get-in config [:database :path])))

(defn ensure-config-dir
  "Ensure config directory exists."
  []
  (let [config-dir (get-config-dir)]
    (when-not (.exists (io/file config-dir))
      (.mkdirs (io/file config-dir)))))

(defn save-config
  "Save configuration to file."
  [config]
  (ensure-config-dir)
  (spit (get-config-file-path) (pr-str config)))

(defn command-enabled?
  "Check if a command is enabled based on permissions configuration.
   Permissions is a nested map where false means disabled and true/missing means enabled.
   Path is a vector of keywords representing the command hierarchy.

   Supports both top-level disabling (e.g., {:plan false}) and nested disabling (e.g., {:plan {:delete false}})

   Example permissions structure:
   {:db {:reset false :init true}
    :plan false  ; Disables entire plan command tree
    :task-list {:delete false}} ; Disables only task-list delete"
  [permissions path]
  (if (empty? path)
    true
    (let [top-level-key (first path)]
      (if-let [top-level-setting (find permissions top-level-key)]
        ;; Top-level key exists in permissions
        (let [value (val top-level-setting)]
          (if (false? value)
            ;; If top-level command is explicitly disabled, entire tree is disabled
            false
            ;; If top-level setting is not false, check nested path
            (let [enabled (get-in permissions path true)]
              (if (boolean? enabled) enabled true))))
        ;; Top-level key doesn't exist, check nested path from root
        (let [enabled (get-in permissions path true)]
          (if (boolean? enabled) enabled true))))))

(defn filter-commands
  "Filter CLI commands based on permissions configuration.
   Commands with subcommands are recursively filtered.
   Path represents the current command hierarchy as a vector of keywords."
  [commands permissions path]
  (->> commands
       (filter
        (fn [cmd]
          (let [command-key (keyword (:command cmd))
                new-path (conj path command-key)]
            (if (:subcommands cmd)
               ;; For commands with subcommands, recursively filter them
              (let [filtered-subcommands (filter-commands (:subcommands cmd) permissions new-path)]
                (seq filtered-subcommands)) ; Only keep if there are subcommands left
               ;; For leaf commands, check permissions
              (command-enabled? permissions new-path)))))
       (mapv
        (fn [cmd]
          (let [command-key (keyword (:command cmd))
                new-path (conj path command-key)]
            (if (:subcommands cmd)
               ;; For commands with subcommands, recursively filter them
              (let [filtered-subcommands (filter-commands (:subcommands cmd) permissions new-path)]
                (assoc cmd :subcommands filtered-subcommands))
               ;; For leaf commands, keep as is
              cmd))))))

(defn init-config
  "Initialize configuration if it doesn't exist."
  []
  (ensure-config-dir)
  (when-not (.exists (io/file (get-config-file-path)))
    (save-config default-config)))
