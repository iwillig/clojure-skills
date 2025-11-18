(ns clojure-skills.config-test
  "Tests for configuration loading and command filtering."
  (:require
   [clojure.test :refer [deftest testing is]]
   [clojure-skills.config :as config]
   [clojure.java.io :as io]
   [clojure.edn :as edn]))

(deftest command-filtering-test
  (testing "Command filtering with permissions"
    (let [test-commands
          [{:command "db"
            :description "Database operations"
            :subcommands
            [{:command "init" :description "Init DB"}
             {:command "reset" :description "Reset DB"}
             {:command "stats" :description "DB Stats"}]}
           {:command "plan"
            :description "Plan operations"
            :subcommands
            [{:command "create" :description "Create plan"}
             {:command "delete" :description "Delete plan"}]}
           {:command "skill"
            :description "Skill operations"
            :subcommands
            [{:command "search" :description "Search skills"}
             {:command "list" :description "List skills"}]}]

          test-permissions
          {:db {:reset false}
           :plan {:delete false}}]

      (testing "Disabled commands are filtered out"
        (let [filtered (config/filter-commands test-commands test-permissions [])]
          ;; db/reset should be removed
          (is (not (some #(= "reset" (:command %))
                         (->> filtered
                              (filter #(= "db" (:command %)))
                              first
                              :subcommands))))

          ;; plan/delete should be removed
          (is (not (some #(= "delete" (:command %))
                         (->> filtered
                              (filter #(= "plan" (:command %)))
                              first
                              :subcommands))))

          ;; Enabled commands should remain
          (is (some #(= "init" (:command %))
                    (->> filtered
                         (filter #(= "db" (:command %)))
                         first
                         :subcommands)))

          (is (some #(= "stats" (:command %))
                    (->> filtered
                         (filter #(= "db" (:command %)))
                         first
                         :subcommands)))

          (is (some #(= "create" (:command %))
                    (->> filtered
                         (filter #(= "plan" (:command %)))
                         first
                         :subcommands)))

          ;; Commands without specific permissions should remain
          (is (some #(= "skill" (:command %)) filtered))
          (is (some #(= "search" (:command %))
                    (->> filtered
                         (filter #(= "skill" (:command %)))
                         first
                         :subcommands)))
          (is (some #(= "list" (:command %))
                    (->> filtered
                         (filter #(= "skill" (:command %)))
                         first
                         :subcommands)))))))

  (testing "Command enabled check"
    (let [permissions {:db {:reset false :init true}
                       :plan {:delete false}}]

      (testing "Explicitly disabled commands return false"
        (is (false? (config/command-enabled? permissions [:db :reset])))
        (is (false? (config/command-enabled? permissions [:plan :delete]))))

      (testing "Explicitly enabled commands return true"
        (is (true? (config/command-enabled? permissions [:db :init]))))

      (testing "Commands without permissions return true by default"
        (is (true? (config/command-enabled? permissions [:db :stats])))
        (is (true? (config/command-enabled? permissions [:plan :create]))))

      (testing "Top-level commands without permissions return true by default"
        (is (true? (config/command-enabled? permissions [:skill])))))))

(deftest top-level-command-disabling-test
  (testing "Top-level command disabling"
    (let [permissions {:plan false
                       :db {:reset false}}]

      (testing "Top-level disabled command returns false for all subcommands"
        (is (false? (config/command-enabled? permissions [:plan])))
        (is (false? (config/command-enabled? permissions [:plan :create])))
        (is (false? (config/command-enabled? permissions [:plan :delete])))
        (is (false? (config/command-enabled? permissions [:plan :show]))))

      (testing "Nested disabled commands still work"
        (is (false? (config/command-enabled? permissions [:db :reset]))))

      (testing "Mixed configuration works correctly"
        (is (true? (config/command-enabled? permissions [:db])))
        (is (true? (config/command-enabled? permissions [:db :init])))
        (is (true? (config/command-enabled? permissions [:db :stats]))))

      (testing "Commands without any permissions return true by default"
        (is (true? (config/command-enabled? permissions [:skill])))
        (is (true? (config/command-enabled? permissions [:skill :search])))
        (is (true? (config/command-enabled? permissions [:task-list])))))))

(deftest command-filtering-with-top-level-disabling-test
  (testing "Command filtering with top-level permissions"
    (let [test-commands
          [{:command "db"
            :description "Database operations"
            :subcommands
            [{:command "init" :description "Init DB"}
             {:command "reset" :description "Reset DB"}
             {:command "stats" :description "DB Stats"}]}
           {:command "plan"
            :description "Plan operations"
            :subcommands
            [{:command "create" :description "Create plan"}
             {:command "delete" :description "Delete plan"}]}
           {:command "skill"
            :description "Skill operations"
            :subcommands
            [{:command "search" :description "Search skills"}
             {:command "list" :description "List skills"}]}]

          test-permissions
          {:plan false
           :db {:reset false}}]

      (testing "Top-level disabled commands are completely filtered out"
        (let [filtered (config/filter-commands test-commands test-permissions [])]
          ;; plan command should be completely removed
          (is (not (some #(= "plan" (:command %)) filtered)))))

      (testing "Nested disabled commands are filtered out"
        (let [filtered (config/filter-commands test-commands test-permissions [])]
          ;; db/reset should be removed
          (is (not (some #(= "reset" (:command %))
                         (->> filtered
                              (filter #(= "db" (:command %)))
                              first
                              :subcommands))))))

      (testing "Enabled commands remain"
        (let [filtered (config/filter-commands test-commands test-permissions [])]
          ;; db/init and db/stats should remain
          (is (some #(= "init" (:command %))
                    (->> filtered
                         (filter #(= "db" (:command %)))
                         first
                         :subcommands)))

          (is (some #(= "stats" (:command %))
                    (->> filtered
                         (filter #(= "db" (:command %)))
                         first
                         :subcommands)))

          ;; skill command and subcommands should remain
          (is (some #(= "skill" (:command %)) filtered))
          (is (some #(= "search" (:command %))
                    (->> filtered
                         (filter #(= "skill" (:command %)))
                         first
                         :subcommands)))
          (is (some #(= "list" (:command %))
                    (->> filtered
                         (filter #(= "skill" (:command %)))
                         first
                         :subcommands))))))))

(deftest project-root-detection-test
  (testing "Finding project root with markers"
    ;; Test that we can detect common project markers
    (let [current-dir (.getAbsolutePath (java.io.File. "."))]
      ;; Since we're running from the project root, we should find it
      (is (= current-dir (config/find-project-root))))))

(deftest project-config-loading-test
  (testing "Loading project configuration"
    ;; Create a temporary project config for testing
    (let [temp-dir (System/getProperty "java.io.tmpdir")
          project-dir (str temp-dir "/test-project-" (System/currentTimeMillis))
          config-dir (str project-dir "/.clojure-skills")
          config-file (str config-dir "/config.edn")
          test-config {:test-value "project-test"}]

      ;; Create test directory structure
      (.mkdirs (io/file config-dir))

      ;; Write test config
      (spit config-file (pr-str test-config))

      ;; Test loading config from project
      (with-redefs [config/find-project-root (fn [] project-dir)]
        (let [loaded-config (config/load-project-config)]
          (is (= test-config loaded-config))))

      ;; Clean up
      (io/delete-file config-file)
      (io/delete-file config-dir)
      (io/delete-file project-dir))))

(deftest config-priority-test
  (testing "Configuration loading priority"
    ;; Test that environment variables override project config
    (let [original-db-path (System/getenv "CLOJURE_SKILLS_DB_PATH")
          test-db-path "/tmp/test-db-from-env.db"]

      ;; Temporarily set environment variable
      (with-redefs [config/get-env-overrides (fn [] {:database {:path test-db-path}})]
        (let [test-project-config {:database {:path "/tmp/test-db-from-project.db"}}
              test-global-config {:database {:path "/tmp/test-db-from-global.db"}}]

          ;; Mock the config loading functions
          (with-redefs [config/load-config-file (fn [] test-global-config)
                        config/load-project-config (fn [] test-project-config)
                        config/default-config {:database {:path "/tmp/test-db-default.db" :auto-migrate true}}]

            (let [final-config (config/load-config)]
              ;; Environment variable should win
              (is (= test-db-path (get-in final-config [:database :path]))))))))))