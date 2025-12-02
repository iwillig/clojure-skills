# Test-Ready Status

## What Works Right Now

### ✅ Global Flags Are Present
```bash
bb main --help
# Shows:
#   -j, --[no-]json   Output as JSON (default)
#   -H, --[no-]human  Output in human-readable format
```

### ✅ Infrastructure Complete
- `output/get-output-format` function correctly determines format
- All multimethod implementations (`format-json` and `format-human`) work
- Tests confirm infrastructure works: `(k/run 'clojure-skills.output-test)` passes

### ✅ Test Suite Ready
- 20+ tests in `test/clojure_skills/json_flag_test.clj`
- Tests specify exactly what behavior we expect
- Tests currently fail because commands don't use the infrastructure

## What Doesn't Work Yet

### ❌ Commands Don't Use The Flags

**Current behavior:**
```bash
bb main task-list show 1
# Outputs: Human-readable with ANSI colors

bb main --json task-list show 1
# Outputs: STILL human-readable with ANSI colors (flag ignored!)

bb main --human task-list show 1
# Outputs: Human-readable with ANSI colors (works, but by accident)
```

**Expected behavior:**
```bash
bb main task-list show 1
# Should output: JSON (default)

bb main --json task-list show 1
# Should output: JSON

bb main --human task-list show 1
# Should output: Human-readable with ANSI colors
```

## The Problem

Commands receive `json` and `human` parameters but ignore them:

```clojure
(defn cmd-show-task-list
  [{:keys [_arguments json human]}]  ; ← Receives parameters
  (let [list-id (first _arguments)]
    (handle-command-errors
     "Show task list"
     (fn []
       (let [[_config db] (load-config-and-db)
             ;; ... 
             task-list (tasks/get-task-list-by-id db list-id-coerced)]
         (if task-list
           (do
             (println)  ; ← Ignores json/human, always uses println
             (println (bling/bling [:bold (:task_lists/name task-list)]))
             ;; ... more println ...
```

## The Solution (For Each Command)

```clojure
(defn cmd-show-task-list
  [{:keys [_arguments json human]}]  ; ← Receives parameters
  (let [list-id (first _arguments)]
    (handle-command-errors
     "Show task list"
     (fn []
       (let [[config db] (load-config-and-db)  ; ← Changed _config to config
             ;; ...
             task-list (tasks/get-task-list-by-id db list-id-coerced)]
         (if task-list
           (let [format (output/get-output-format json human config)  ; ← NEW: Use the flags!
                 data {:type "task-list"  ; ← NEW: Prepare data
                       :data {;; ... transform task-list fields ...}}]
             (output/output data format))  ; ← NEW: Use multimethod output
```

## How to Verify After Updating

### Test Infrastructure (Already Passes)
```bash
clj-nrepl-eval -p 7889 "(k/run 'clojure-skills.output-test)"
# ✅ 13 tests, 60 assertions, 0 failures
```

### Test Flag Functionality (Currently Fails)
```bash
clj-nrepl-eval -p 7889 "(k/run 'clojure-skills.json-flag-test)"
# ❌ 20+ tests fail because commands don't use infrastructure
```

### Manual Test
```bash
# After updating cmd-show-task-list:
bb main task-list show 1 | jq .
# Should show valid JSON

bb main --human task-list show 1
# Should show human-readable with colors

bb main task-list show 1 > output.json
cat output.json | jq .type
# Should show: "task-list"
```

## Commands That Need Updating

All 5 commands are in `src/clojure_skills/cli.clj`:

| Command | Line | Status |
|---------|------|--------|
| cmd-show-task-list | 755 | ❌ Not updated |
| cmd-show-task | Find with `rg 'defn cmd-show-task[^-]'` | ❌ Not updated |
| cmd-show-plan-result | Find with `rg 'defn cmd-show-plan-result'` | ❌ Not updated |
| cmd-list-plan-skills | Find with `rg 'defn cmd-list-plan-skills'` | ❌ Not updated |
| cmd-search-plan-results | Find with `rg 'defn cmd-search-plan-results'` | ❌ Not updated |

## Time Estimate

- Per command: 6 minutes (mechanical change)
- Total for 5 commands: 30 minutes
- Testing and verification: 15 minutes
- **Total: 45 minutes**

## References

- Step-by-step guide: `NEXT_STEPS.md`
- Full status: `IMPLEMENTATION_STATUS.md`
- Plan: `bb main plan show global-json-flag`
- Tests: `test/clojure_skills/json_flag_test.clj`
