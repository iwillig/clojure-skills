(ns clojure-skills.config
  "Configuration management with XDG Base Directory compliance.
  
  Configuration priority:
  1. Environment variables
  2. ~/.config/clojure-skills/config.edn
  3. Built-in defaults"
  (:require
   [clojure-skills.logging :as log]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))


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


(def default-config
  "Default configuration values."
  {:database
   {:path "~/.config/clojure-skills/clojure-skills.db"
    :auto-migrate true}

   :project
   {:root nil
    :skills-dir "skills"
    :prompts-dir "prompts"
    :build-dir "_build"}

   :search
   {:max-results 50
    :context-lines 3}

   :output
   {:format :table
    :color true}})


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
   2. Config file
   3. Defaults"
  []
  (let [file-config (load-config-file)
        env-config (get-env-overrides)]
    (deep-merge default-config file-config env-config)))


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


(defn init-config
  "Initialize configuration if it doesn't exist."
  []
  (ensure-config-dir)
  (when-not (.exists (io/file (get-config-file-path)))
    (save-config default-config)))
