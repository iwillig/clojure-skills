(ns clojure-skills.db.prompt-fragments-test
  "Tests for prompt fragments database functions"
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure-skills.db.migrate :as migrate]
   [clojure-skills.db.prompt-fragments :as pf]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as sql]
   [clojure.java.io :as io]))

(defn create-test-db
  "Create a test database with all migrations applied"
  []
  (let [db-file (str "test-prompt-fragments-" (java.util.UUID/randomUUID) ".db")
        db-spec {:dbtype "sqlite" :dbname db-file}]
    ;; Apply all migrations
    (migrate/migrate-db db-spec)
    {:db-spec db-spec :db-file db-file}))

(defn cleanup-test-db
  "Clean up test database file"
  [db-file]
  (when (.exists (io/file db-file))
    (.delete (io/file db-file))))

(deftest test-prompt-fragment-crud
  (testing "Prompt fragment CRUD operations"
    (let [{:keys [db-spec db-file]} (create-test-db)
          db (jdbc/get-datasource db-spec)]
      (try
        ;; Test create
        (let [fragment (pf/create-prompt-fragment db {:name "test-fragment"
                                                      :title "Test Fragment"
                                                      :description "A test fragment"})]
          (is (some? (:prompt_fragments/id fragment)) "Fragment should have an ID")
          (is (= "test-fragment" (:prompt_fragments/name fragment)) "Fragment name should match")
          (is (= "Test Fragment" (:prompt_fragments/title fragment)) "Fragment title should match")
          (is (= "A test fragment" (:prompt_fragments/description fragment)) "Fragment description should match")

          ;; Test get by ID
          (let [retrieved (pf/get-prompt-fragment-by-id db (:prompt_fragments/id fragment))]
            (is (= (:prompt_fragments/id fragment) (:prompt_fragments/id retrieved)) "IDs should match")
            (is (= "test-fragment" (:prompt_fragments/name retrieved)) "Names should match"))

          ;; Test get by name
          (let [retrieved (pf/get-prompt-fragment-by-name db "test-fragment")]
            (is (= (:prompt_fragments/id fragment) (:prompt_fragments/id retrieved)) "IDs should match")
            (is (= "Test Fragment" (:prompt_fragments/title retrieved)) "Titles should match"))

          ;; Test update
          (let [updated (pf/update-prompt-fragment db (:prompt_fragments/id fragment)
                                                   {:title "Updated Fragment"
                                                    :description "An updated fragment"})]
            (is (= "Updated Fragment" (:prompt_fragments/title updated)) "Title should be updated")
            (is (= "An updated fragment" (:prompt_fragments/description updated)) "Description should be updated")

            ;; Verify update persisted
            (let [retrieved (pf/get-prompt-fragment-by-id db (:prompt_fragments/id fragment))]
              (is (= "Updated Fragment" (:prompt_fragments/title retrieved)) "Title should be updated in database")
              (is (= "An updated fragment" (:prompt_fragments/description retrieved)) "Description should be updated in database")))

          ;; Test list
          (let [fragments (pf/list-prompt-fragments db)]
            (is (= 1 (count fragments)) "Should have one fragment")
            (is (= "test-fragment" (:prompt_fragments/name (first fragments))) "Fragment name should match"))

          ;; Test delete
          (let [deleted (pf/delete-prompt-fragment db (:prompt_fragments/id fragment))]
            (is (some? deleted) "Delete should return the deleted record")

            ;; Verify deletion
            (let [retrieved (pf/get-prompt-fragment-by-id db (:prompt_fragments/id fragment))]
              (is (nil? retrieved) "Fragment should no longer exist"))))

        (finally
          (cleanup-test-db db-file))))))

(deftest test-prompt-fragment-skill-associations
  (testing "Prompt fragment skill associations"
    (let [{:keys [db-spec db-file]} (create-test-db)
          db (jdbc/get-datasource db-spec)]
      (try
        ;; Insert a test skill
        (sql/insert! db :skills {:path "test/test-skill.md"
                                 :category "test"
                                 :name "test-skill"
                                 :title "Test Skill"
                                 :content "Test content"
                                 :file_hash "test-hash"
                                 :size_bytes 100})

        ;; Get the skill ID
        (let [skill-id (-> (jdbc/execute! db ["SELECT id FROM skills LIMIT 1"])
                           first
                           :skills/id)
              ;; Create a prompt fragment
              fragment (pf/create-prompt-fragment db {:name "skill-fragment"
                                                      :title "Skill Fragment"
                                                      :description "A fragment with skills"})
              ;; Associate skill with fragment
              association (pf/associate-skill-with-fragment db {:fragment_id (:prompt_fragments/id fragment)
                                                                :skill_id skill-id
                                                                :position 1})]
          (is (some? association) "Association should be created")
          (is (= (:prompt_fragments/id fragment) (:prompt_fragment_skills/fragment_id association)) "Fragment ID should match")
          (is (= skill-id (:prompt_fragment_skills/skill_id association)) "Skill ID should match")
          (is (= 1 (:prompt_fragment_skills/position association)) "Position should match")

          ;; Test get skills for fragment
          (let [skills (pf/get-skills-for-fragment db (:prompt_fragments/id fragment))]
            (is (= 1 (count skills)) "Should have one skill")
            (is (= skill-id (:skills/id (first skills))) "Skill ID should match")
            (is (= 1 (:prompt_fragment_skills/position (first skills))) "Position should match"))

          ;; Test get fragments containing skill
          (let [fragments (pf/get-fragments-containing-skill db skill-id)]
            (is (= 1 (count fragments)) "Should have one fragment containing the skill")
            (is (= (:prompt_fragments/id fragment) (:prompt_fragments/id (first fragments))) "Fragment ID should match"))

          ;; Test remove skill from fragment
          (let [removed (pf/remove-skill-from-fragment db (:prompt_fragments/id fragment) skill-id)]
            (is (some? removed) "Removal should return the removed record")

            ;; Verify removal
            (let [skills (pf/get-skills-for-fragment db (:prompt_fragments/id fragment))]
              (is (= 0 (count skills)) "Should have no skills after removal"))))

        (finally
          (cleanup-test-db db-file))))))

(deftest test-prompt-references
  (testing "Prompt references functionality"
    (let [{:keys [db-spec db-file]} (create-test-db)
          db (jdbc/get-datasource db-spec)]
      (try
        ;; Insert test data
        (sql/insert! db :skills {:path "test/test-skill.md"
                                 :category "test"
                                 :name "test-skill"
                                 :title "Test Skill"
                                 :content "Test content"
                                 :file_hash "test-hash"
                                 :size_bytes 100})

        (sql/insert! db :prompts {:path "test/source-prompt.md"
                                  :name "source-prompt"
                                  :title "Source Prompt"
                                  :author "Test Author"
                                  :description "Source prompt"
                                  :content "Source content"
                                  :file_hash "source-hash"
                                  :size_bytes 200})

        (let [source-prompt-id (-> (jdbc/execute! db ["SELECT id FROM prompts WHERE name = ?" "source-prompt"])
                                   first
                                   :prompts/id)
              ;; Create a prompt fragment and test references
              fragment (pf/create-prompt-fragment db {:name "reference-fragment"
                                                      :title "Reference Fragment"
                                                      :description "A fragment for references"})
              reference (pf/add-prompt-reference db {:source_prompt_id source-prompt-id
                                                     :target_fragment_id (:prompt_fragments/id fragment)
                                                     :reference_type "fragment"
                                                     :position 1})]
          (is (some? reference) "Reference should be created")
          (is (= source-prompt-id (:prompt_references/source_prompt_id reference)) "Source prompt ID should match")
          (is (= (:prompt_fragments/id fragment) (:prompt_references/target_fragment_id reference)) "Target fragment ID should match")
          (is (= "fragment" (:prompt_references/reference_type reference)) "Reference type should match")
          (is (= 1 (:prompt_references/position reference)) "Position should match")

          ;; Test get references for prompt
          (let [references (pf/get-references-for-prompt db source-prompt-id)]
            (is (= 1 (count references)) "Should have one reference")
            (is (= "fragment" (:prompt_references/reference_type (first references))) "Reference type should match"))

          ;; Test get prompt with fragment references
          (let [prompt-with-refs (pf/get-prompt-with-fragment-references db source-prompt-id)]
            (is (some? (:prompts/id prompt-with-refs)) "Should have prompt data")
            (is (= 1 (count (:fragment_references prompt-with-refs))) "Should have one fragment reference")
            (is (= "reference-fragment" (:prompt_references/name (first (:fragment_references prompt-with-refs)))) "Fragment name should match")))

        (finally
          (cleanup-test-db db-file))))))

(deftest test-prompt-reference-validation
  (testing "Prompt reference validation"
    (let [{:keys [db-spec db-file]} (create-test-db)
          db (jdbc/get-datasource db-spec)]
      (try
        ;; Insert test data
        (sql/insert! db :prompts {:path "test/validation-prompt.md"
                                  :name "validation-prompt"
                                  :title "Validation Prompt"
                                  :author "Test Author"
                                  :description "Validation prompt"
                                  :content "Validation content"
                                  :file_hash "validation-hash"
                                  :size_bytes 200})

        (let [prompt-id (-> (jdbc/execute! db ["SELECT id FROM prompts WHERE name = ?" "validation-prompt"])
                            first
                            :prompts/id)]

          ;; Test missing required fields
          (is (thrown? Exception
                       (pf/add-prompt-reference db {:source_prompt_id prompt-id
                                                    :reference_type "fragment"}))
              "Should throw exception when missing position")

          ;; Test invalid reference type
          (is (thrown? Exception
                       (pf/add-prompt-reference db {:source_prompt_id prompt-id
                                                    :reference_type "invalid"
                                                    :position 1}))
              "Should throw exception when reference_type is invalid")

          ;; Test missing targets
          (is (thrown? Exception
                       (pf/add-prompt-reference db {:source_prompt_id prompt-id
                                                    :reference_type "fragment"
                                                    :position 1}))
              "Should throw exception when no target is specified")

          ;; Test conflicting targets
          (is (thrown? Exception
                       (pf/add-prompt-reference db {:source_prompt_id prompt-id
                                                    :reference_type "fragment"
                                                    :position 1
                                                    :target_prompt_id 1
                                                    :target_fragment_id 1}))
              "Should throw exception when both targets are specified")

          ;; Test missing target for fragment type
          (is (thrown? Exception
                       (pf/add-prompt-reference db {:source_prompt_id prompt-id
                                                    :reference_type "fragment"
                                                    :position 1
                                                    :target_prompt_id 1}))
              "Should throw exception when fragment type but prompt target specified"))

        (finally
          (cleanup-test-db db-file))))))
