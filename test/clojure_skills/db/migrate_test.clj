(ns clojure-skills.db.migrate-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure-skills.db.migrate :as migrate]
            [next.jdbc :as jdbc]
            [clojure.java.io :as io]))

(def test-db-path "test-migrate-db.db")

(defn test-db-fixture [f]
  ;; Clean up any existing test database
  (when (.exists (io/file test-db-path))
    (.delete (io/file test-db-path)))
  ;; Run the test
  (f)
  ;; Clean up after test
  (when (.exists (io/file test-db-path))
    (.delete (io/file test-db-path))))

(use-fixtures :each test-db-fixture)

(deftest test-migrate-db
  (testing "migrate-db creates schema on new database"
    (let [db {:dbtype "sqlite" :dbname test-db-path}]
      (migrate/migrate-db db)

      ;; Check that ragtime_migrations table exists
      (let [result (jdbc/execute-one! db ["SELECT name FROM sqlite_master WHERE type='table' AND name='ragtime_migrations'"])]
        (is (some? result) "ragtime_migrations table should exist"))

      ;; Check that skills table exists
      (let [result (jdbc/execute-one! db ["SELECT name FROM sqlite_master WHERE type='table' AND name='skills'"])]
        (is (some? result) "skills table should exist"))

      ;; Check that prompts table exists
      (let [result (jdbc/execute-one! db ["SELECT name FROM sqlite_master WHERE type='table' AND name='prompts'"])]
        (is (some? result) "prompts table should exist"))

      ;; Check that prompt_skills table exists
      (let [result (jdbc/execute-one! db ["SELECT name FROM sqlite_master WHERE type='table' AND name='prompt_skills'"])]
        (is (some? result) "prompt_skills table should exist"))

      ;; Check that FTS tables exist
      (let [result (jdbc/execute-one! db ["SELECT name FROM sqlite_master WHERE type='table' AND name='skills_fts'"])]
        (is (some? result) "skills_fts table should exist"))

      (let [result (jdbc/execute-one! db ["SELECT name FROM sqlite_master WHERE type='table' AND name='prompts_fts'"])]
        (is (some? result) "prompts_fts table should exist"))

      ;; Check that at least one migration was applied
      (let [migrations (jdbc/execute! db ["SELECT * FROM ragtime_migrations"])]
        (is (pos? (count migrations)) "At least one migration should be applied")))))

(deftest test-migrate-db-idempotent
  (testing "migrate-db is idempotent - running twice doesn't fail"
    (let [db {:dbtype "sqlite" :dbname test-db-path}]
      ;; First migration
      (migrate/migrate-db db)
      (let [migrations1 (jdbc/execute! db ["SELECT * FROM ragtime_migrations"])]
        ;; Second migration
        (migrate/migrate-db db)
        (let [migrations2 (jdbc/execute! db ["SELECT * FROM ragtime_migrations"])]
          ;; Should have same number of migrations
          (is (= (count migrations1) (count migrations2))
              "Running migrate twice should not apply migrations again"))))))

(deftest test-schema-structure
  (testing "migrated schema has correct table structure"
    (let [db {:dbtype "sqlite" :dbname test-db-path}]
      (migrate/migrate-db db)

      ;; Test that we can insert into skills table
      (jdbc/execute! db ["INSERT INTO skills (path, category, name, content, file_hash, size_bytes, token_count) 
                          VALUES (?, ?, ?, ?, ?, ?, ?)"
                         "test/path.md" "test" "test" "content" "hash123" 100 25])

      (let [skill (jdbc/execute-one! db ["SELECT * FROM skills WHERE path = ?" "test/path.md"])]
        (is (some? skill))
        (is (= "test/path.md" (:skills/path skill)))
        (is (= "test" (:skills/category skill)))
        (is (some? (:skills/created_at skill)))
        (is (some? (:skills/updated_at skill))))

      ;; Test that we can insert into prompts table
      (jdbc/execute! db ["INSERT INTO prompts (path, name, content, file_hash, size_bytes, token_count) 
                          VALUES (?, ?, ?, ?, ?, ?)"
                         "prompts/test.md" "test_prompt" "prompt content" "hash456" 200 50])

      (let [prompt (jdbc/execute-one! db ["SELECT * FROM prompts WHERE path = ?" "prompts/test.md"])]
        (is (some? prompt))
        (is (= "prompts/test.md" (:prompts/path prompt)))
        (is (= "test_prompt" (:prompts/name prompt)))
        (is (some? (:prompts/created_at prompt)))
        (is (some? (:prompts/updated_at prompt)))))))
