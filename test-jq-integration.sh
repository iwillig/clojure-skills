#!/bin/bash
# Test script to verify all CLI commands output valid JSON that works with jq

echo "Testing clojure-skills CLI JSON output with jq"
echo "=============================================="
echo

echo "=== Basic Command Tests ==="
test_count=0
pass_count=0

test_jq() {
    local description="$1"
    local command="$2"
    local jq_query="$3"
    
    ((test_count++))
    echo -n "$test_count. $description... "
    
    if eval "$command" | jq -e "$jq_query" > /dev/null 2>&1; then
        echo "✓"
        ((pass_count++))
    else
        echo "✗ FAILED"
        return 1
    fi
}

# Skill commands
test_jq "skill list outputs valid JSON" \
    "bb main skill list" \
    '.type == "skill-list" and .count > 0 and (.skills | type) == "array"'

test_jq "skill search outputs valid JSON" \
    "bb main skill search database" \
    '.type == "skill-search-results" and .count >= 0'

test_jq "skill show outputs valid JSON" \
    "bb main skill show malli" \
    '.type == "skill" and .data.name == "malli"'

# Prompt commands
test_jq "prompt list outputs valid JSON" \
    "bb main prompt list" \
    '.type == "prompt-list" and .count > 0'

test_jq "prompt search outputs valid JSON" \
    "bb main prompt search agent" \
    '.type == "prompt-search-results"'

test_jq "prompt show outputs valid JSON" \
    "bb main prompt show clojure_build" \
    '.type == "prompt" and .data.name == "clojure_build"'

# Plan commands
test_jq "plan list outputs valid JSON" \
    "bb main plan list" \
    '.type == "plan-list" and .count > 0'

test_jq "plan show outputs valid JSON" \
    "bb main plan show 6" \
    '.type == "plan" and .data.name'

# Database commands
test_jq "db stats outputs valid JSON" \
    "bb main db stats" \
    '.type == "stats" and .database.skills > 0'

echo
echo "=== Complex jq Query Tests ==="

test_jq "Count all tasks in plan" \
    "bb main plan show 6" \
    '[.data."task-lists"[].tasks[]] | length >= 0'

test_jq "Count completed tasks" \
    "bb main plan show 6" \
    '[.data."task-lists"[].tasks[] | select(.completed == true)] | length >= 0'

test_jq "Filter skills by category" \
    "bb main skill list" \
    '[.skills[] | select(.category | startswith("libraries"))] | length > 0'

test_jq "Get skill names as array" \
    "bb main skill list" \
    '[.skills[].name] | type == "array"'

test_jq "Calculate total tokens" \
    "bb main skill list" \
    '[.skills[]."token-count"] | add > 0'

test_jq "Extract plan metadata" \
    "bb main plan show 6" \
    '.data | {name, status, created_at: ."created-at"} | .name'

test_jq "Filter prompt fragments" \
    "bb main prompt show clojure_build" \
    '.data."embedded-fragments" | type == "array"'

echo
echo "=============================================="
echo "Results: $pass_count/$test_count tests passed"
echo "=============================================="

if [[ $pass_count -eq $test_count ]]; then
    echo "✓ All jq integration tests passed!"
    exit 0
else
    echo "✗ Some tests failed"
    exit 1
fi
