(ns clojure-skills.db.schema
  "Database schema definitions for clojure-skills.
  
  Uses SQLite with FTS5 for full-text search."
  (:require [next.jdbc :as jdbc]))

(def schema-version 1)

(def migrations
  "Database migrations in order."
  [{:version 1
    :up
    ["CREATE TABLE IF NOT EXISTS schema_version (
        version INTEGER PRIMARY KEY,
        applied_at TEXT NOT NULL DEFAULT (datetime('now'))
      )"

     "CREATE TABLE IF NOT EXISTS skills (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        path TEXT NOT NULL UNIQUE,
        category TEXT NOT NULL,
        name TEXT NOT NULL,
        title TEXT,
        description TEXT,
        content TEXT NOT NULL,
        file_hash TEXT NOT NULL,
        size_bytes INTEGER NOT NULL,
        token_count INTEGER,
        created_at TEXT NOT NULL DEFAULT (datetime('now')),
        updated_at TEXT NOT NULL DEFAULT (datetime('now'))
      )"

     "CREATE INDEX idx_skills_category ON skills(category)"
     "CREATE INDEX idx_skills_name ON skills(name)"
     "CREATE INDEX idx_skills_hash ON skills(file_hash)"

     "CREATE TABLE IF NOT EXISTS prompts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        path TEXT NOT NULL UNIQUE,
        name TEXT NOT NULL,
        title TEXT,
        author TEXT,
        description TEXT,
        content TEXT NOT NULL,
        file_hash TEXT NOT NULL,
        size_bytes INTEGER NOT NULL,
        token_count INTEGER,
        created_at TEXT NOT NULL DEFAULT (datetime('now')),
        updated_at TEXT NOT NULL DEFAULT (datetime('now'))
      )"

     "CREATE INDEX idx_prompts_name ON prompts(name)"
     "CREATE INDEX idx_prompts_hash ON prompts(file_hash)"

     "CREATE TABLE IF NOT EXISTS prompt_skills (
        prompt_id INTEGER NOT NULL,
        skill_id INTEGER NOT NULL,
        position INTEGER NOT NULL,
        PRIMARY KEY (prompt_id, skill_id),
        FOREIGN KEY (prompt_id) REFERENCES prompts(id) ON DELETE CASCADE,
        FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
      )"

     "CREATE INDEX idx_prompt_skills_prompt ON prompt_skills(prompt_id)"
     "CREATE INDEX idx_prompt_skills_skill ON prompt_skills(skill_id)"

     ;; FTS5 full-text search for skills
     "CREATE VIRTUAL TABLE IF NOT EXISTS skills_fts USING fts5(
        path,
        category,
        name,
        title,
        description,
        content,
        content='skills',
        content_rowid='id'
      )"

     ;; Triggers to keep FTS index in sync with skills table
     "CREATE TRIGGER IF NOT EXISTS skills_ai AFTER INSERT ON skills BEGIN
        INSERT INTO skills_fts(rowid, path, category, name, title, description, content)
        VALUES (new.id, new.path, new.category, new.name, new.title, new.description, new.content);
      END"

     "CREATE TRIGGER IF NOT EXISTS skills_ad AFTER DELETE ON skills BEGIN
        INSERT INTO skills_fts(skills_fts, rowid, path, category, name, title, description, content)
        VALUES('delete', old.id, old.path, old.category, old.name, old.title, old.description, old.content);
      END"

     "CREATE TRIGGER IF NOT EXISTS skills_au AFTER UPDATE ON skills BEGIN
        INSERT INTO skills_fts(skills_fts, rowid, path, category, name, title, description, content)
        VALUES('delete', old.id, old.path, old.category, old.name, old.title, old.description, old.content);
        INSERT INTO skills_fts(rowid, path, category, name, title, description, content)
        VALUES (new.id, new.path, new.category, new.name, new.title, new.description, new.content);
      END"

     ;; FTS5 full-text search for prompts
     "CREATE VIRTUAL TABLE IF NOT EXISTS prompts_fts USING fts5(
        path,
        name,
        title,
        author,
        description,
        content,
        content='prompts',
        content_rowid='id'
      )"

     ;; Triggers to keep FTS index in sync with prompts table
     "CREATE TRIGGER IF NOT EXISTS prompts_ai AFTER INSERT ON prompts BEGIN
        INSERT INTO prompts_fts(rowid, path, name, title, author, description, content)
        VALUES (new.id, new.path, new.name, new.title, new.author, new.description, new.content);
      END"

     "CREATE TRIGGER IF NOT EXISTS prompts_ad AFTER DELETE ON prompts BEGIN
        INSERT INTO prompts_fts(prompts_fts, rowid, path, name, title, author, description, content)
        VALUES('delete', old.id, old.path, old.name, old.title, old.author, old.description, old.content);
      END"

     "CREATE TRIGGER IF NOT EXISTS prompts_au AFTER UPDATE ON prompts BEGIN
        INSERT INTO prompts_fts(prompts_fts, rowid, path, name, title, author, description, content)
        VALUES('delete', old.id, old.path, old.name, old.title, old.author, old.description, old.content);
        INSERT INTO prompts_fts(rowid, path, name, title, author, description, content)
        VALUES (new.id, new.path, new.name, new.title, new.author, new.description, new.content);
      END"]

    :down
    ["DROP TRIGGER IF EXISTS prompts_au"
     "DROP TRIGGER IF EXISTS prompts_ad"
     "DROP TRIGGER IF EXISTS prompts_ai"
     "DROP TABLE IF EXISTS prompts_fts"
     "DROP TRIGGER IF EXISTS skills_au"
     "DROP TRIGGER IF EXISTS skills_ad"
     "DROP TRIGGER IF EXISTS skills_ai"
     "DROP TABLE IF EXISTS skills_fts"
     "DROP INDEX IF EXISTS idx_prompt_skills_skill"
     "DROP INDEX IF EXISTS idx_prompt_skills_prompt"
     "DROP TABLE IF EXISTS prompt_skills"
     "DROP INDEX IF EXISTS idx_prompts_hash"
     "DROP INDEX IF EXISTS idx_prompts_name"
     "DROP TABLE IF EXISTS prompts"
     "DROP INDEX IF EXISTS idx_skills_hash"
     "DROP INDEX IF EXISTS idx_skills_name"
     "DROP INDEX IF EXISTS idx_skills_category"
     "DROP TABLE IF EXISTS skills"
     "DROP TABLE IF EXISTS schema_version"]}])

(defn get-current-version
  "Get current schema version from database."
  [db]
  (try
    (let [result (jdbc/execute-one! db ["SELECT MAX(version) as version FROM schema_version"])]
      (or (:version result) 0))
    (catch Exception _
      0)))

(defn apply-migration
  "Apply a single migration."
  [db migration]
  (let [{:keys [version up]} migration]
    (println (format "Applying migration v%d..." version))
    (jdbc/with-transaction [tx db]
      (doseq [statement up]
        (jdbc/execute! tx [statement]))
      (jdbc/execute! tx ["INSERT INTO schema_version (version) VALUES (?)" version]))))

(defn migrate
  "Run all pending migrations."
  [db]
  (let [current-version (get-current-version db)
        pending (filter #(> (:version %) current-version) migrations)]
    (if (empty? pending)
      (println "Database is up to date.")
      (do
        (println (format "Running %d migration(s)..." (count pending)))
        (doseq [migration pending]
          (apply-migration db migration))
        (println "Migrations complete.")))))

(defn reset-database
  "Drop all tables and re-run migrations. USE WITH CAUTION!"
  [db]
  (println "WARNING: Resetting database...")
  (jdbc/with-transaction [tx db]
    ;; Apply down migrations in reverse order
    (doseq [migration (reverse migrations)]
      (doseq [statement (:down migration)]
        (try
          (jdbc/execute! tx [statement])
          (catch Exception e
            ;; Ignore errors for non-existent objects
            (when-not (re-find #"no such table|no such trigger|no such index" (.getMessage e))
              (throw e)))))))
  (migrate db))
