# Next Steps for Global JSON Flag Implementation

## Current Status
- ✅ Phase 1: Analysis and Design (COMPLETE)
- ✅ Phase 2: Infrastructure (COMPLETE)  
- ⚠️ Phase 3: Testing (PARTIAL - tests written, waiting for implementation)
- ❌ Phase 4: Command Updates (NOT STARTED - 5 commands need updating)

## What's Done
- All multimethod implementations (`format-json` and `format-human`) are complete
- `get-output-format` function updated with correct signature
- Comprehensive test suite written (20+ tests in `json_flag_test.clj`)
- All output tests passing (13 tests, 60 assertions)

## What Remains (Estimated 30-45 minutes)

### 5 Commands Need Simple Mechanical Updates

Each command follows the same pattern. Here's the recipe:

#### Recipe for Each Command

1. **Change `_config` to `config`**
   ```diff
   -       (let [[_config db] (load-config-and-db)
   +       (let [[config db] (load-config-and-db)
   ```

2. **Add format determination**
   ```clojure
   format (output/get-output-format json human config)
   ```

3. **Prepare data with :type key**
   ```clojure
   data {:type "command-type"  ; matches multimethod
         :data {;; ... transformed fields ...}}
   ```

4. **Replace all println/bling/table calls**
   ```clojure
   (output/output data format)
   ```

### Commands to Update

| Command | Location | Estimated Time |
|---------|----------|----------------|
| `cmd-show-task-list` | Line 755 in cli.clj | 6 min |
| `cmd-show-task` | Find with: `rg 'defn cmd-show-task[^-]' src/` | 6 min |
| `cmd-show-plan-result` | Find with: `rg 'defn cmd-show-plan-result' src/` | 6 min |
| `cmd-list-plan-skills` | Find with: `rg 'defn cmd-list-plan-skills' src/` | 6 min |
| `cmd-search-plan-results` | Find with: `rg 'defn cmd-search-plan-results' src/` | 6 min |

### Quick Start

```bash
# 1. Start nREPL
bb nrepl

# 2. Connect and load dev namespace
clj-nrepl-eval -p 7889 "(require '[dev :refer :all])"

# 3. See current test failures (expected)
clj-nrepl-eval -p 7889 "(k/run 'clojure-skills.json-flag-test)"
# All 20+ tests fail because commands not updated yet

# 4. Update first command (cmd-show-task-list)
# Edit src/clojure_skills/cli.clj line 755

# 5. Test just that command
clj-nrepl-eval -p 7889 "(refresh)"
clj-nrepl-eval -p 7889 "(k/run 'clojure-skills.json-flag-test/task-list-show-with-json-flag)"

# 6. Repeat for other 4 commands

# 7. Run full test suite
clj-nrepl-eval -p 7889 "(k/run-all)"

# 8. Complete the plan
clojure-skills task complete 159  # cmd-show-task-list
clojure-skills task complete 160  # cmd-show-task
clojure-skills task complete 161  # cmd-show-plan-result
clojure-skills task complete 162  # cmd-list-plan-skills
clojure-skills task complete 163  # cmd-search-plan-results
clojure-skills task complete 164  # testing
clojure-skills task complete 156  # full test suite
clojure-skills plan complete 10
```

## Detailed Example: cmd-show-task-list

### Current Implementation (line 755)
```clojure
(defn cmd-show-task-list
  [{:keys [_arguments json human]}]
  (let [list-id (first _arguments)]
    (handle-command-errors
     "Show task list"
     (fn []
       (let [[_config db] (load-config-and-db)  ; ← Change to config
             args (v/coerce-and-validate! v/show-task-list-args-schema {:id list-id})
             list-id-coerced (:id args)
             task-list (tasks/get-task-list-by-id db list-id-coerced)]
         (if task-list
           (do
             (println)  ; ← Remove all of this
             (println (bling/bling [:bold (:task_lists/name task-list)]))
             (println (str "ID: " (:task_lists/id task-list)))
             ;; ... lots more println ...
             (let [list-tasks (tasks/list-tasks-for-list db list-id-coerced)]
               (when (seq list-tasks)
                 (doseq [task list-tasks]
                   (println (str "  " ...))))))  ; ← Remove all of this
           (do
             (print-error ...)
             (*exit-fn* 1))))))))
```

### Updated Implementation
```clojure
(defn cmd-show-task-list
  [{:keys [_arguments json human]}]
  (let [list-id (first _arguments)]
    (handle-command-errors
     "Show task list"
     (fn []
       (let [[config db] (load-config-and-db)  ; ← Changed
             args (v/coerce-and-validate! v/show-task-list-args-schema {:id list-id})
             list-id-coerced (:id args)
             task-list (tasks/get-task-list-by-id db list-id-coerced)]
         (if task-list
           (let [list-tasks (tasks/list-tasks-for-list db list-id-coerced)
                 format (output/get-output-format json human config)  ; ← NEW
                 data {:type "task-list"  ; ← NEW - matches multimethod
                       :data {:id (:task_lists/id task-list)
                              :name (:task_lists/name task-list)
                              :plan_id (:task_lists/plan_id task-list)
                              :description (:task_lists/description task-list)
                              :position (:task_lists/position task-list)
                              :created-at (:task_lists/created_at task-list)
                              :updated-at (:task_lists/updated_at task-list)
                              :tasks (map (fn [task]
                                            {:id (:tasks/id task)
                                             :name (:tasks/name task)
                                             :description (:tasks/description task)
                                             :completed (= 1 (:tasks/completed task))
                                             :assigned-to (:tasks/assigned_to task)})
                                          list-tasks)}}]
             (output/output data format))  ; ← NEW - replaces all println
           (do
             (print-error ...)
             (*exit-fn* 1))))))))
```

## Type Keys for Each Command

These match the multimethod dispatch values (already implemented):

| Command | Type Key | Multimethod Location |
|---------|----------|---------------------|
| cmd-show-task-list | `"task-list"` | output.clj:38, 41 |
| cmd-show-task | `"task"` | output.clj:72, 75 |
| cmd-show-plan-result | `"plan-result"` | output.clj:105, 108 |
| cmd-list-plan-skills | `"plan-skills-list"` | output.clj:147, 150 |
| cmd-search-plan-results | `"plan-results-search"` | output.clj:172, 175 |

## Field Transformations

### task-list
```clojure
{:id (:task_lists/id task-list)
 :name (:task_lists/name task-list)
 :plan_id (:task_lists/plan_id task-list)
 :description (:task_lists/description task-list)
 :position (:task_lists/position task-list)
 :created-at (:task_lists/created_at task-list)
 :updated-at (:task_lists/updated_at task-list)
 :tasks (map transform-task list-tasks)}
```

### task
```clojure
{:id (:tasks/id task)
 :name (:tasks/name task)
 :list_id (:tasks/list_id task)
 :completed (= 1 (:tasks/completed task))
 :description (:tasks/description task)
 :assigned-to (:tasks/assigned_to task)
 :position (:tasks/position task)
 :created-at (:tasks/created_at task)
 :updated-at (:tasks/updated_at task)
 :completed-at (:tasks/completed_at task)}
```

### plan-result
```clojure
{:id (:plan_results/id result)
 :plan_id (:plan_results/plan_id result)
 :outcome (:plan_results/outcome result)
 :summary (:plan_results/summary result)
 :challenges (:plan_results/challenges result)
 :solutions (:plan_results/solutions result)
 :lessons_learned (:plan_results/lessons_learned result)
 :metrics (:plan_results/metrics result)
 :created_at (:plan_results/created_at result)
 :updated_at (:plan_results/updated_at result)}
```

### plan-skills-list
```clojure
{:type "plan-skills-list"
 :plan-id plan-id
 :skills (map (fn [skill]
                {:position (:plan_skills/position skill)
                 :category (:skills/category skill)
                 :name (:skills/name skill)
                 :title (:skills/title skill)})
              skills)}
```

### plan-results-search
```clojure
{:type "plan-results-search"
 :count (count results)
 :results (map (fn [result]
                 {:plan_id (:plan_id result)
                  :outcome (:outcome result)
                  :snippet (:snippet result)
                  :rank (:rank result)})
               results)}
```

## Verification

After updating all 5 commands:

```bash
# All tests should pass
clj-nrepl-eval -p 7889 "(k/run-all)"

# Lint should pass
clj-nrepl-eval -p 7889 "(lint)"

# Manual verification
bb main task-list show 1 --json     # Should output JSON
bb main task-list show 1 --human    # Should output human-readable
bb main task-list show 1            # Should output JSON (default)
```

## References

- Full status: `IMPLEMENTATION_STATUS.md`
- Plan: `clojure-skills plan show global-json-flag`
- Tests: `test/clojure_skills/json_flag_test.clj`
- Output implementation: `src/clojure_skills/output.clj`
- Commands: `src/clojure_skills/cli.clj`
