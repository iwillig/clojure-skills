(ns clojure-skills.search
  "Full-text search using SQLite FTS5."
  (:require
   [next.jdbc :as jdbc]))


(defn search-skills
  "Search skills using FTS5 full-text search.
   
   Options:
   - :max-results - Maximum number of results (default 50)
   - :category - Filter by category"
  [db query & {:keys [max-results category] :or {max-results 50}}]
  (let [base-sql "SELECT s.*, 
                         snippet(skills_fts, 5, '[', ']', '...', 30) as snippet,
                         rank
                  FROM skills_fts
                  JOIN skills s ON skills_fts.rowid = s.id
                  WHERE skills_fts MATCH ?"
        sql (if category
              (str base-sql " AND s.category = ?")
              base-sql)
        sql (str sql " ORDER BY rank LIMIT ?")
        params (if category
                 [query category max-results]
                 [query max-results])]
    (jdbc/execute! db (into [sql] params))))


(defn search-prompts
  "Search prompts using FTS5 full-text search.
   
   Options:
   - :max-results - Maximum number of results (default 50)"
  [db query & {:keys [max-results] :or {max-results 50}}]
  (let [sql "SELECT p.*,
                    snippet(prompts_fts, 5, '[', ']', '...', 30) as snippet,
                    rank
             FROM prompts_fts
             JOIN prompts p ON prompts_fts.rowid = p.id
             WHERE prompts_fts MATCH ?
             ORDER BY rank
             LIMIT ?"
        params [query max-results]]
    (jdbc/execute! db (into [sql] params))))


(defn search-all
  "Search both skills and prompts.
   
   Returns a map with :skills and :prompts keys."
  [db query & {:keys [max-results category] :or {max-results 50}}]
  {:skills (apply search-skills db query
                  (concat [:max-results max-results]
                          (when category [:category category])))
   :prompts (search-prompts db query :max-results max-results)})


(defn list-categories
  "List all unique skill categories with counts."
  [db]
  (jdbc/execute! db ["SELECT category, COUNT(*) as count 
                      FROM skills 
                      GROUP BY category 
                      ORDER BY category"]))


(defn list-skills
  "List all skills, optionally filtered by category.
   
   Options:
   - :category - Filter by category
   - :limit - Maximum number of results
   - :offset - Offset for pagination"
  [db & {:keys [category limit offset] :or {limit 100 offset 0}}]
  (let [base-sql "SELECT * FROM skills"
        sql (if category
              (str base-sql " WHERE category = ?")
              base-sql)
        sql (str sql " ORDER BY category, name LIMIT ? OFFSET ?")
        params (if category
                 [category limit offset]
                 [limit offset])]
    (jdbc/execute! db (into [sql] params))))


(defn list-prompts
  "List all prompts.
   
   Options:
   - :limit - Maximum number of results
   - :offset - Offset for pagination"
  [db & {:keys [limit offset] :or {limit 100 offset 0}}]
  (jdbc/execute! db ["SELECT * FROM prompts ORDER BY name LIMIT ? OFFSET ?"
                     limit offset]))


(defn get-skill-by-name
  "Get a skill by its name and optionally category."
  [db name & {:keys [category]}]
  (if category
    (jdbc/execute-one! db ["SELECT * FROM skills WHERE name = ? AND category = ?"
                           name category])
    (jdbc/execute-one! db ["SELECT * FROM skills WHERE name = ?" name])))


(defn get-prompt-by-name
  "Get a prompt by its name."
  [db name]
  (jdbc/execute-one! db ["SELECT * FROM prompts WHERE name = ?" name]))


(defn get-stats
  "Get database statistics."
  [db]
  (let [skill-count (jdbc/execute-one! db ["SELECT COUNT(*) as count FROM skills"])
        prompt-count (jdbc/execute-one! db ["SELECT COUNT(*) as count FROM prompts"])
        total-size (jdbc/execute-one! db ["SELECT 
                                            SUM(size_bytes) as total_size,
                                            SUM(token_count) as total_tokens
                                           FROM (
                                             SELECT size_bytes, token_count FROM skills
                                             UNION ALL
                                             SELECT size_bytes, token_count FROM prompts
                                           )"])
        categories (list-categories db)]
    {:skills (:count skill-count)
     :prompts (:count prompt-count)
     :total-size-bytes (:total_size total-size)
     :total-tokens (:total_tokens total-size)
     :categories (count categories)
     :category-breakdown categories}))
