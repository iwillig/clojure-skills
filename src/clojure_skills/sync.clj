(ns clojure-skills.sync
  "Sync markdown files to SQLite database."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-yaml.core :as yaml]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [clojure-skills.config :as config]
            [clojure-skills.db.core])
  (:import [java.security MessageDigest]
           [java.io File]))

(defn compute-hash
  "Compute SHA-256 hash of file content."
  [content]
  (let [digest (MessageDigest/getInstance "SHA-256")
        bytes (.digest digest (.getBytes content "UTF-8"))]
    (apply str (map #(format "%02x" %) bytes))))

(defn extract-frontmatter
  "Extract YAML frontmatter from markdown content.
   Returns [frontmatter content-without-frontmatter]."
  [content]
  (let [lines (str/split-lines content)]
    (if (and (seq lines)
             (= "---" (first lines)))
      (let [end-idx (some #(when (= "---" (nth lines %)) %)
                          (range 1 (count lines)))]
        (if end-idx
          (let [frontmatter-lines (subvec (vec lines) 1 end-idx)
                frontmatter-text (str/join "\n" frontmatter-lines)
                content-lines (subvec (vec lines) (inc end-idx))
                content-text (str/join "\n" content-lines)]
            (try
              [(yaml/parse-string frontmatter-text) content-text]
              (catch Exception e
                (println "Warning: Failed to parse frontmatter:" (.getMessage e))
                [nil content])))
          [nil content]))
      [nil content])))

(defn estimate-tokens
  "Estimate token count (roughly 4 characters per token)."
  [text]
  (int (/ (count text) 4)))

(defn parse-skill-path
  "Parse skill file path to extract category and name.
   Example: skills/language/clojure_intro.md -> {:category language :name clojure_intro}"
  [path]
  (let [parts (str/split path #"/")
        filename (last parts)
        name (str/replace filename #"\.md$" "")
        ;; Find index of "skills" directory
        skills-idx (or (some #(when (= "skills" (nth parts %)) %)
                             (range (count parts)))
                       -1)
        ;; Extract parts after "skills" but before filename
        category-parts (if (> skills-idx -1)
                         (subvec (vec parts) (inc skills-idx) (dec (count parts)))
                         [])
        category (if (seq category-parts)
                   (str/join "/" category-parts)
                   "uncategorized")]
    {:category category
     :name name}))

(defn scan-skill-files
  "Scan skills directory and return list of skill file paths."
  [skills-dir]
  (let [dir (io/file skills-dir)]
    (when (.exists dir)
      (->> (file-seq dir)
           (filter #(and (.isFile ^File %)
                         (str/ends-with? (.getName ^File %) ".md")))
           (map #(str (.getPath ^File %)))
           (sort)))))

(defn scan-prompt-files
  "Scan prompts directory and return list of prompt file paths."
  [prompts-dir]
  (let [dir (io/file prompts-dir)]
    (when (.exists dir)
      (->> (file-seq dir)
           (filter #(and (.isFile %)
                         (str/ends-with? (.getName ^File %) ".md")))
           (map #(str (.getPath ^File %)))
           (sort)))))

(defn parse-skill-file
  "Parse a skill markdown file and extract metadata."
  [path]
  (let [content (slurp path)
        [frontmatter _content-without-frontmatter] (extract-frontmatter content)
        {:keys [category name]} (parse-skill-path path)
        file-hash (compute-hash content)
        size-bytes (.length (io/file path))
        token-count (estimate-tokens content)]
    {:path path
     :category category
     :name name
     :title (get frontmatter "title")
     :description (get frontmatter "description")
     :content content
     :file_hash file-hash ; Using snake_case for SQL compatibility
     :size_bytes size-bytes
     :token_count token-count}))

(defn parse-prompt-file
  "Parse a prompt markdown file and extract metadata."
  [path]
  (let [content (slurp path)
        [frontmatter _content-without-frontmatter] (extract-frontmatter content)
        filename (last (str/split path #"/"))
        name (str/replace filename #"\.md$" "")
        file-hash (compute-hash content)
        size-bytes (.length (io/file path))
        token-count (estimate-tokens content)]
    {:path path
     :name name
     :title (get frontmatter "title")
     :author (get frontmatter "author")
     :description (get frontmatter "description")
     :sections (get frontmatter "sections" [])
     :content content
     :file_hash file-hash ; Using snake_case for SQL compatibility
     :size_bytes size-bytes
     :token_count token-count}))

(defn get-skill-by-path
  "Get skill from database by path."
  [db path]
  (jdbc/execute-one! db ["SELECT * FROM skills WHERE path = ?" path]))

(defn get-prompt-by-path
  "Get prompt from database by path."
  [db path]
  (jdbc/execute-one! db ["SELECT * FROM prompts WHERE path = ?" path]))

(defn upsert-skill
  "Insert or update skill in database."
  [db skill]
  (let [existing (get-skill-by-path db (:path skill))]
    (if existing
      ;; Update
      (sql/update! db :skills
                   (dissoc skill :path)
                   {:path (:path skill)})
      ;; Insert
      (sql/insert! db :skills skill))))

(defn upsert-prompt
  "Insert or update prompt in database."
  [db prompt]
  (let [existing (get-prompt-by-path db (:path prompt))]
    (if existing
      ;; Update
      (sql/update! db :prompts
                   (dissoc prompt :path :sections)
                   {:path (:path prompt)})
      ;; Insert
      (sql/insert! db :prompts (dissoc prompt :sections)))))

(defn sync-skill
  "Sync a single skill file to database."
  [db skill-path]
  (try
    (let [skill-data (parse-skill-file skill-path)
          existing (get-skill-by-path db skill-path)]
      (if (and existing
               (= (:file-hash existing) (:file-hash skill-data)))
        (println "  Skipped (unchanged):" skill-path)
        (do
          (upsert-skill db skill-data)
          (println "  Synced:" skill-path))))
    (catch Exception e
      (println "  ERROR syncing" skill-path ":" (.getMessage e)))))

(defn sync-prompt
  "Sync a single prompt file to database."
  [db prompt-path]
  (try
    (let [prompt-data (parse-prompt-file prompt-path)
          existing (get-prompt-by-path db prompt-path)]
      (if (and existing
               (= (:file-hash existing) (:file-hash prompt-data)))
        (println "  Skipped (unchanged):" prompt-path)
        (do
          (upsert-prompt db prompt-data)
          (println "  Synced:" prompt-path))))
    (catch Exception e
      (println "  ERROR syncing" prompt-path ":" (.getMessage e)))))

(defn sync-all-skills
  "Sync all skills from skills directory to database."
  [db config]
  (let [project-root (or (get-in config [:project :root])
                         (System/getProperty "user.dir"))
        skills-dir (str project-root "/" (get-in config [:project :skills-dir]))
        skill-files (scan-skill-files skills-dir)]
    (println (format "Syncing %d skills from %s..." (count skill-files) skills-dir))
    (doseq [skill-file skill-files]
      (sync-skill db skill-file))
    (println "Skills sync complete.")))

(defn sync-all-prompts
  "Sync all prompts from prompts directory to database."
  [db config]
  (let [project-root (or (get-in config [:project :root])
                         (System/getProperty "user.dir"))
        prompts-dir (str project-root "/" (get-in config [:project :prompts-dir]))
        prompt-files (scan-prompt-files prompts-dir)]
    (println (format "Syncing %d prompts from %s..." (count prompt-files) prompts-dir))
    (doseq [prompt-file prompt-files]
      (sync-prompt db prompt-file))
    (println "Prompts sync complete.")))

(defn sync-all
  "Sync all skills and prompts to database."
  ([db config]
   (sync-all-skills db config)
   (sync-all-prompts db config))
  ([]
   (let [config (config/load-config)
         db (clojure-skills.db.core/get-db config)]
     (sync-all db config))))
