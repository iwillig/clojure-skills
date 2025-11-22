# Global JSON Flag Implementation Status

## Summary

Phase 1 (Analysis and Design) and Phase 2 (Implementation) are COMPLETE.
Phase 3 (Testing and Validation) is IN PROGRESS.

## What's Done

### Phase 1: Analysis and Design ✅
- Identified all commands with non-JSON output
- Designed output format structure for human-readable output  
- Designed global --json flag mechanism

### Phase 2: Implementation ✅
- ✅ Added --json and --human global options to cli-config
- ✅ Created human-readable output formatters in output.clj (multimethods)
- ✅ Updated `get-output-format` function with correct signature
- ✅ All multimethod implementations for data types:
  - task-list
  - task
  - plan-result
  - plan-skills-list
  - plan-results-search
  - skill-search-results
  - skill-list
  - skill
  - prompt-search-results
  - prompt-list
  - prompt
  - stats
  - plan-list
  - plan
  - search-results

### Phase 3: Testing and Validation ⚠️ IN PROGRESS
- ✅ Written comprehensive tests for human-readable output formatters (output_test.clj)
- ✅ Written comprehensive tests for --json flag functionality (json_flag_test.clj)
- ❌ Need to update CLI commands to use new output system
- ❌ Need to run full test suite and fix any failures

## What Remains

### Commands That Need Updating

The following commands are receiving `json` and `human` parameters but are NOT using them:

1. **`cmd-show-task-list`** (line 755 in cli.clj)
   - Currently: Uses `println` and `bling` directly
   - Needs: Call `output/output` with proper data structure

2. **`cmd-show-task`** (needs to be found and updated)
   - Currently: Uses `println` and `bling` directly
   - Needs: Call `output/output` with proper data structure

3. **`cmd-show-plan-result`** (needs to be found and updated)
   - Currently: Uses `println` and `bling` directly
   - Needs: Call `output/output` with proper data structure

4. **`cmd-list-plan-skills`** (needs to be found and updated)
   - Currently: Uses `println` and `table/print-table` directly
   - Needs: Call `output/output` with proper data structure

5. **`cmd-search-plan-results`** (needs to be found and updated)
   - Currently: Uses `println` and `bling` directly
   - Needs: Call `output/output` with proper data structure

### Implementation Pattern

Each command needs to follow this pattern:

```clojure
(defn cmd-show-SOMETHING
  [{:keys [_arguments json human]}]
  (let [id (first _arguments)]
    (validate-non-blank id "ID cannot be empty")
    (handle-command-errors
     "Show SOMETHING"
     (fn []
       (let [[config db] (load-config-and-db)  ; Note: config not _config
             ;; ... validation and data retrieval ...
             result (get-something db id)]
         (if result
           (let [;; Determine output format
                 format (output/get-output-format json human config)
                 ;; Prepare data with :type key
                 data {:type "something"
                       :data {;; ... transform result to data structure ...}}]
             ;; Output using multimethod dispatcher
             (output/output data format))
           (do
             (print-error (str "Something not found: " id))
             (*exit-fn* 1))))))))
```

Key changes from current implementations:
1. Destructure `config` (not `_config`) from `load-config-and-db`
2. Call `output/get-output-format` with `json`, `human`, and `config`
3. Prepare data with proper `:type` key matching multimethod dispatch
4. Call `output/output` instead of `println`/`bling`/`table/print-table`

### Test Status

**Passing Tests:**
- ✅ `clojure-skills.output-test` (13 tests, 60 assertions)
  - All multimethod implementations tested
  - Format dispatching tested
  - Default behaviors tested

**Failing Tests:**
- ❌ `clojure-skills.json-flag-test` (20 tests)
  - All tests fail because commands don't use new output system yet
  - Tests are correct - implementation needs to catch up
  - Error: JSON parsing fails because commands output ANSI-colored text

### Next Steps

1. **Update cmd-show-task-list** (highest priority)
   - Location: `src/clojure_skills/cli.clj:755`
   - Follow implementation pattern above
   - Test with: `(k/run 'clojure-skills.json-flag-test/task-list-show-with-json-flag)`

2. **Find and update cmd-show-task**
   - Search: `rg "defn cmd-show-task[^-]" src/`
   - Follow same pattern
   - Test with: `(k/run 'clojure-skills.json-flag-test/task-show-with-json-flag)`

3. **Find and update cmd-show-plan-result**
   - Search: `rg "defn cmd-show-plan-result" src/`
   - Follow same pattern
   - Test with: `(k/run 'clojure-skills.json-flag-test/plan-result-show-with-json-flag)`

4. **Find and update cmd-list-plan-skills**
   - Search: `rg "defn cmd-list-plan-skills" src/`
   - Follow same pattern
   - Test with: `(k/run 'clojure-skills.json-flag-test/plan-skills-list-with-json-flag)`

5. **Find and update cmd-search-plan-results**
   - Search: `rg "defn cmd-search-plan-results" src/`
   - Follow same pattern
   - Test with: `(k/run 'clojure-skills.json-flag-test/plan-results-search-with-json-flag)`

6. **Run full test suite**
   - `bb test` or `(k/run-all)`
   - Fix any breakages from signature changes

7. **Run linting**
   - `bb lint` or `(lint)`
   - Fix any issues

8. **Manual testing**
   - Test each command with `--json` flag
   - Test each command with `--human` flag
   - Test with no flags (should default to JSON)
   - Test that config file settings work

## Files Modified

### Source Files
- `src/clojure_skills/output.clj`
  - Updated `get-output-format` signature to accept both `json` and `human` flags
  - All multimethod implementations already in place

### Test Files
- `test/clojure_skills/output_test.clj`
  - Updated tests to use new `get-output-format` signature
  - All tests passing (13 tests, 60 assertions)

- `test/clojure_skills/json_flag_test.clj` (NEW FILE)
  - Comprehensive tests for --json and --human flags
  - Tests for all 5 commands that need updating
  - Tests for flag precedence over config
  - Currently failing because commands not updated yet

## Current Test Results

```bash
# Passing
clojure-skills.output-test: 13 tests, 60 assertions, 0 failures ✅

# Failing (expected - implementation incomplete)
clojure-skills.json-flag-test: ~20 tests failing ❌
  - All failures due to commands not using new output system
  - JSON parsing errors (char 27 = ESC from ANSI colors)
  - Tests are correct, implementation needs to catch up
```

## Time Estimate

Remaining work: ~1-2 hours
- Update 5 commands: ~30 minutes (6 minutes each)
- Test and debug: ~30 minutes
- Full test suite run and fixes: ~30 minutes

## References

- Plan document: `clojure-skills plan show global-json-flag`
- Output implementation: `src/clojure_skills/output.clj`
- Test implementation: `test/clojure_skills/json_flag_test.clj`
- CLI implementation: `src/clojure_skills/cli.clj`
