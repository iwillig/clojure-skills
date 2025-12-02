# How to Complete the Global JSON Flag Implementation

## Quick Start

```bash
# 1. Start nREPL
bb nrepl

# 2. In another terminal, connect and load dev
clj-nrepl-eval -p 7889 "(require '[dev :refer :all])"

# 3. Run tests to see current status
clj-nrepl-eval -p 7889 "(k/run 'clojure-skills.json-flag-test)"
```

## What You Need to Do

Update 5 CLI commands to use the new output system. Each takes ~6 minutes.

### Template for All Commands

```clojure
(defn cmd-show-SOMETHING
  [{:keys [_arguments json human]}]
  (let [id (first _arguments)]
    (validate-non-blank id "ID cannot be empty")
    (handle-command-errors
     "Show SOMETHING"
     (fn []
       (let [[config db] (load-config-and-db)  ; ← config not _config!
             args (v/coerce-and-validate! v/schema {:id id})
             result (get-something db (:id args))]
         (if result
           (let [format (output/get-output-format json human config)  ; ← NEW
                 data {:type "something"  ; ← NEW - matches multimethod
                       :data {;; Transform result fields here
                              :id (:something/id result)
                              :name (:something/name result)
                              ;; ... etc
                              }}]
             (output/output data format))  ; ← NEW - replaces all println
           (do
             (print-error (str "Something not found: " id))
             (*exit-fn* 1))))))))
```

## Step-by-Step for Each Command

### 1. cmd-show-task-list (Line 755)

**Current behavior:** Prints with `bling` and `println`

**What to change:**

<function_calls>
<invoke name="bash">
<parameter name="command">cat > /tmp/cmd-show-task-list-diff.md << 'EOF'
```diff
-       (let [[_config db] (load-config-and-db)
+       (let [[config db] (load-config-and-db)
```

Replace the entire `(if task-list (do ...` block with:

```clojure
         (if task-list
           (let [list-tasks (tasks/list-tasks-for-list db list-id-coerced)
                 format (output/get-output-format json human config)
                 data {:type "task-list"
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
             (output/output data format))
```
EOF
cat /tmp/cmd-show-task-list-diff.md
