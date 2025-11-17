(ns clojure-skills.sync-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure-skills.sync :as sync]
            [clojure-skills.db.core :as db]
            [clojure-skills.db.migrate :as migrate]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(deftest test-compute-hash
  (testing "compute-hash generates consistent SHA-256 hash"
    (let [content "test content"
          hash1 (sync/compute-hash content)
          hash2 (sync/compute-hash content)]
      (is (= hash1 hash2))
      (is (= 64 (count hash1))) ; SHA-256 produces 64 hex characters
      (is (re-matches #"[0-9a-f]{64}" hash1))))

  (testing "compute-hash produces different hashes for different content"
    (let [hash1 (sync/compute-hash "content1")
          hash2 (sync/compute-hash "content2")]
      (is (not= hash1 hash2)))))

(deftest test-extract-frontmatter
  (testing "extract-frontmatter parses YAML frontmatter"
    (let [content "---\ntitle: Test\nauthor: Alice\n---\n# Content here"
          [frontmatter remaining] (sync/extract-frontmatter content)]
      (is (map? frontmatter))
      (is (= "Test" (:title frontmatter)))
      (is (= "Alice" (:author frontmatter)))
      (is (str/starts-with? remaining "# Content"))))

  (testing "extract-frontmatter handles content without frontmatter"
    (let [content "# Just markdown content"
          [frontmatter remaining] (sync/extract-frontmatter content)]
      (is (nil? frontmatter))
      (is (= content remaining))))

  (testing "extract-frontmatter handles empty frontmatter"
    (let [content "---\n---\n# Content"
          [frontmatter remaining] (sync/extract-frontmatter content)]
      (is (or (nil? frontmatter) (empty? frontmatter)))
      (is (str/starts-with? remaining "# Content")))))

(deftest test-estimate-tokens
  (testing "estimate-tokens approximates token count"
    (is (= 25 (sync/estimate-tokens (apply str (repeat 100 "x")))))
    (is (= 0 (sync/estimate-tokens "")))
    (is (pos? (sync/estimate-tokens "Some text here")))))

(deftest test-parse-skill-path
  (testing "parse-skill-path extracts category and name"
    (let [result (sync/parse-skill-path "skills/language/clojure_intro.md")]
      (is (= "language" (:category result)))
      (is (= "clojure_intro" (:name result)))))

  (testing "parse-skill-path handles nested categories"
    (let [result (sync/parse-skill-path "skills/libraries/data_validation/malli.md")]
      (is (= "libraries/data_validation" (:category result)))
      (is (= "malli" (:name result)))))

  (testing "parse-skill-path handles uncategorized skills"
    (let [result (sync/parse-skill-path "other/skill.md")]
      (is (= "uncategorized" (:category result)))
      (is (= "skill" (:name result))))))

(deftest test-scan-skill-files
  (testing "scan-skill-files returns markdown files"
    (let [skills-dir "skills"
          files (sync/scan-skill-files skills-dir)]
      (when files ; Only test if skills directory exists
        (is (coll? files))
        (is (every? string? files))
        (is (every? #(str/ends-with? % ".md") files))))))

(deftest test-scan-prompt-files
  (testing "scan-prompt-files returns markdown files"
    (let [prompts-dir "prompts"
          files (sync/scan-prompt-files prompts-dir)]
      (when files ; Only test if prompts directory exists
        (is (coll? files))
        (is (every? string? files))
        (is (every? #(str/ends-with? % ".md") files))))))

(deftest test-parse-skill-file
  (testing "parse-skill-file extracts metadata from skill file"
    (let [test-file "skills/language/clojure_intro.md"]
      (when (.exists (io/file test-file))
        (let [result (sync/parse-skill-file test-file)]
          (is (map? result))
          (is (= test-file (:path result)))
          (is (string? (:category result)))
          (is (string? (:name result)))
          (is (string? (:content result)))
          (is (string? (:file_hash result)))
          (is (pos? (:size_bytes result)))
          (is (pos? (:token_count result))))))))

(deftest test-parse-prompt-file
  (testing "parse-prompt-file extracts metadata from prompt file"
    (let [test-file "prompts/clojure_build.md"]
      (when (.exists (io/file test-file))
        (let [result (sync/parse-prompt-file test-file)]
          (is (map? result))
          (is (= test-file (:path result)))
          (is (string? (:name result)))
          (is (string? (:content result)))
          (is (string? (:file_hash result)))
          (is (pos? (:size_bytes result)))
          (is (pos? (:token_count result)))
          (is (coll? (:sections result))))))))

(deftest test-sync-skill-detects-changes
  (testing "sync-skill correctly detects when file has changed"
    ;; Create a temporary test database
    (let [test-db-path "test-sync-db.db"
          test-config {:database {:path test-db-path}
                       :project {:root (System/getProperty "user.dir")
                                 :skills-dir "skills"
                                 :prompts-dir "prompts"}}]
      (try
        ;; Clean up any existing test database
        (when (.exists (io/file test-db-path))
          (io/delete-file test-db-path))

        ;; Initialize test database using Ragtime migrations
        (let [test-db (db/get-db test-config)]
          (migrate/migrate-db test-db)

          ;; Create a temporary test skill file
          (let [test-skill-dir "test-skills"
                test-skill-path (str test-skill-dir "/test_skill.md")
                initial-content "---\ntitle: Test Skill\ndescription: A test skill\n---\n# Test Content\n\nInitial content here."]

            (try
              ;; Create test directory and file
              (.mkdirs (io/file test-skill-dir))
              (spit test-skill-path initial-content)

              ;; First sync - should insert the skill
              (testing "First sync inserts the skill"
                (sync/sync-skill test-db test-skill-path)
                (let [retrieved (sync/get-skill-by-path test-db test-skill-path)]
                  (is (some? retrieved) "Skill should be inserted")
                  (is (= (:skills/file_hash retrieved)
                         (sync/compute-hash initial-content))
                      "File hash should match initial content")))

              ;; Second sync with same content - should skip
              (testing "Second sync with unchanged content should skip"
                (let [initial-retrieved (sync/get-skill-by-path test-db test-skill-path)
                      initial-updated-at (:skills/updated_at initial-retrieved)]
                  ;; Small delay to ensure timestamp would change if updated
                  (Thread/sleep 10)
                  (sync/sync-skill test-db test-skill-path)
                  (let [after-sync (sync/get-skill-by-path test-db test-skill-path)]
                    (is (= (:skills/updated_at after-sync) initial-updated-at)
                        "updated_at should not change when content unchanged"))))

              ;; Modify the file - should detect change
              (testing "Sync detects when file content changes"
                (let [modified-content "---\ntitle: Test Skill\ndescription: A test skill\n---\n# Test Content\n\nModified content here!"
                      initial-retrieved (sync/get-skill-by-path test-db test-skill-path)
                      initial-hash (:skills/file_hash initial-retrieved)]
                  (spit test-skill-path modified-content)
                  ;; Small delay to ensure timestamp changes
                  (Thread/sleep 10)
                  (sync/sync-skill test-db test-skill-path)
                  (let [updated-retrieved (sync/get-skill-by-path test-db test-skill-path)
                        new-hash (:skills/file_hash updated-retrieved)]
                    (is (not= initial-hash new-hash)
                        "File hash should change when content changes")
                    (is (= new-hash (sync/compute-hash modified-content))
                        "New hash should match modified content"))))

              (finally
                ;; Clean up test skill file and directory
                (when (.exists (io/file test-skill-path))
                  (io/delete-file test-skill-path))
                (when (.exists (io/file test-skill-dir))
                  (.delete (io/file test-skill-dir)))))))

        (finally
          ;; Clean up test database
          (when (.exists (io/file test-db-path))
            (io/delete-file test-db-path)))))))
