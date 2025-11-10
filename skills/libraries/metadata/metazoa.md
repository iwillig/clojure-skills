---
name: metazoa
description: |
  View, test, search, and query Clojure metadata using an extensible provider API. Use when working with
  rich metadata (examples, documentation, function tables, tutorials), testing metadata validity, 
  searching code with Lucene queries, querying metadata with Datalog, or when the user mentions 
  metadata exploration, code search, metadata testing, or interactive documentation.
---

# Metazoa

## Quick Start

Metazoa provides tools for viewing, testing, searching, and querying Clojure metadata. It includes
built-in metadata providers for examples, function tables, documentation, and interactive tutorials.

```clojure
(require '[glossa.metazoa :as meta])

;; Start the interactive tutorial
(meta/help)

;; View metadata providers available on a namespace
(meta/providers 'clojure.core)
;; => (:glossa.metazoa/doc :glossa.metazoa/example)

;; View an example from a var
(meta/view #'clojure.core/name :glossa.metazoa/example)

;; Check that metadata examples are still valid
(meta/check #'clojure.core/max :glossa.metazoa/example)

;; Search metadata with Lucene queries
(meta/search "name:map*")

;; Query metadata with Datalog
(meta/query
 '[:find [?name ...]
   :where
   [?e :name ?name]
   [?e :macro true]])
```

**Add to deps.edn:**

```clojure
;; Git dependency
{dev.glossa/metazoa
 {:git/url "https://gitlab.com/glossa/metazoa.git"
  :git/tag "v0.2.298"
  :git/sha "d0c8ca2839854206d457c70652a940d02577ed09"}}

;; Maven dependency from Clojars
{dev.glossa/metazoa {:mvn/version "0.2.298"}}

;; Optional: Exclude dependencies if not using certain features
:exclusions [cljfmt/cljfmt 
             datascript/datascript 
             metosin/malli 
             org.apache.lucene/lucene-core 
             org.apache.lucene/lucene-queryparser]
```

## Core Concepts

### Metadata Provider API

Metazoa is built around an extensible **Metadata Provider API** defined in `glossa.metazoa.api`. 
A metadata provider is identified by a dispatch keyword and implements one or more multimethods:

- `meta.api/render-metadata` - Returns a value that can be printed
- `meta.api/view-metadata` - Provides custom viewing experience (optional)
- `meta.api/check-metadata` - Validates metadata
- `meta.api/index-for-search` - Customizes Lucene indexing (optional)

Built-in providers:
- `:glossa.metazoa/doc` - Structured documentation using Weave
- `:glossa.metazoa/example` - Executable code examples
- `:glossa.metazoa/fn-table` - Truth tables for functions
- `:glossa.metazoa/tutorial` - Interactive REPL tutorials

### IMeta - Objects with Metadata

Throughout Metazoa, **IMeta** refers to instances of `clojure.lang.IMeta` - values that can store
Clojure metadata. This includes:
- Vars (`#'my-namespace/my-var`)
- Namespaces (`(the-ns 'my-namespace)`)
- Symbols, keywords, collections with metadata

### Output Conventions

In examples:
- `;; [out]` - Output printed to `*out*`
- `;; [err]` - Output printed to `*err*`
- `#_=>` - Return value of expression

## Common Workflows

### Workflow 1: Viewing Metadata

Display metadata in readable formats at the REPL:

```clojure
(require '[glossa.metazoa :as meta])

;; What metadata providers are available on a namespace?
(meta/providers 'clojure.core)
;; => (:glossa.metazoa/doc :glossa.metazoa/example)

;; View a var's example
(meta/view #'clojure.core/name :glossa.metazoa/example)
;; [out] 
;; [out] ;; The `name` function converts symbols, keywords, strings to strings.
;; [out] (= (name 'alpha) (name :alpha) (name "alpha"))
;; [out] #_=> true
;; => #'clojure.core/name

;; View a function table
(meta/view #'clojure.core/max :glossa.metazoa/fn-table)
;; [out] 
;; [out]   OR   0   1  
;; [out] ----- --- ----
;; [out]    0   0   1  
;; [out]    1   1   1  
;; => #'clojure.core/max

;; View all metadata providers on a var
(meta/view #'my-namespace/my-function)

;; View a standalone metadata provider value (useful during development)
(meta/view
 (meta/example {:ns *ns*, :code '(+ 1 2)}))
;; [out] 
;; [out] (+ 1 2)
;; [out] #_=> 3
;; => []

;; Resolve symbols to vars or namespaces
(meta/view 'clojure.core/map)
;; Automatically resolves to #'clojure.core/map

;; Thread multiple view calls
(-> #'my-function
    (meta/view :glossa.metazoa/example)
    (meta/view :glossa.metazoa/doc))
```

**Key features:**
- Returns the IMeta for threading
- Prints with leading semicolons and narrow columns (REPL-friendly)
- Automatically resolves symbols to vars/namespaces

### Workflow 2: Testing Metadata Validity

Ensure your metadata examples remain accurate:

```clojure
;; Add :expected to your example metadata
(defn my-max
  "Returns the greatest number."
  {:glossa.metazoa/example
   {:code '(my-max 5 -5 10 0)
    :expected 10
    :ns *ns*}}
  [& args]
  (apply max args))

;; Check if the example still works
(meta/check #'my-max :glossa.metazoa/example)
;; => [{:code (my-max 5 -5 10 0),
;;      :expected 10,
;;      :actual-out "",
;;      :actual-err "",
;;      :actual 10}]

;; Check all metadata providers on an IMeta
(meta/check #'my-max)
;; Checks all providers that implement meta.api/check-metadata

;; Use with clojure.test
(require '[clojure.test :refer [deftest]])

(deftest test-my-max-metadata
  (meta/test-imeta #'my-max :glossa.metazoa/example))

;; Test all metadata on all IMetas in the classpath
(deftest test-all-metadata
  (meta/test-imetas))
```

**Testing workflow:**
1. `meta/check` returns data - functional validation
2. `meta/test-imeta` asserts validity - integrates with clojure.test
3. `meta/test-imetas` tests everything - comprehensive test suite

### Workflow 3: Searching Metadata with Lucene

Search your codebase's metadata using Lucene query syntax:

```clojure
;; Simple text search
(meta/search "map")
;; [out] Indexing metadata for full-text search...
;; => [#'clojure.core/map 
;;     #'clojure.core/mapv
;;     #'clojure.core/map-indexed
;;     ...]

;; How many results? (default limit is 30)
(count (meta/search "map"))
;; => 30

;; Get actual total hits
(:total-hits (meta (meta/search "map")))
;; => 127

;; Specify result limit
(meta/search {:query "map", :num-hits 10})
;; => [first 10 results...]

;; Or use :limit
(meta/search {:query "map", :limit 5})
;; => [first 5 results...]

;; Field-specific search
(meta/search "name:map*")
;; Only search the name field with wildcard

;; Exclude namespaces
(meta/search "name:reduce AND -ns:cider.*")

;; Find macros only
(meta/search "name:def* AND macro:true")

;; Find functions lacking docstrings
(meta/search "imeta-value-type:clojure.lang.AFunction AND -doc:*")

;; Search within namespace
(meta/search "ns:clojure.core")

;; Search for namespaces themselves
(meta/search "imeta-type:clojure.lang.Namespace AND id:clojure.*")
;; => [namespace objects...]

;; How many public vars in clojure.core?
(-> (meta/search "ns:clojure.core") meta :total-hits)
;; => 742

;; Complex queries
(meta/search "name:map* AND ns:clojure.core AND -macro:true")
;; Map functions in clojure.core that aren't macros
```

**Search indexing:**
- Only **public** vars indexed by default
- All metadata map entries indexed as fields
- Special fields: `:imeta-symbol`, `:imeta-type`, `:imeta-value-type`
- Fully-qualified idents have `/` replaced with `_`

**Customize indexing:**
```clojure
;; Index all vars (including private)
(meta/reset-search
 (meta.api/find-imetas (fn [ns] (conj ((comp vals ns-interns) ns) ns))))
```

### Workflow 4: Querying Metadata with Datalog

Query metadata using DataScript Datalog queries:

```clojure
;; Find all functions added in Clojure 1.4
(meta/query
 '[:find [?name ...]
   :in $ ?ns ?added
   :where
   [?e :ns ?ns]
   [?e :name ?name]
   [?e :added ?added]]
 (the-ns 'clojure.core)
 "1.4")
;; => [symbol1 symbol2 ...]

;; Count vars without :doc
(meta/query
 '[:find [(count ?e)]
   :where
   [?e :ns]
   (not [?e :doc])])
;; => [42]

;; Count functions without :doc
(meta/query
 '[:find [(count ?e)]
   :where
   [?e :ns]
   [?e :imeta/value ?value]
   [(clojure.core/fn? ?value)]
   (not [?e :doc])])
;; => [15]

;; Find all macros in clojure.core
(meta/query
 '[:find [?name ...]
   :in $ ?ns
   :where
   [?e :ns ?ns]
   [?e :name ?name]
   [?e :macro true]]
 (the-ns 'clojure.core))
;; => [defn defmacro let if ...]

;; Find vars with custom metadata key
(meta/query
 '[:find ?imeta ?value
   :where
   [?e :my-custom-meta ?value]
   [?e :imeta/this ?imeta]])
;; => [[#'my-ns/my-var "custom-value"] ...]

;; Use input parameters and predicates
(meta/query
 '[:find [?name ...]
   :in $ ?prefix
   :where
   [?e :name ?name]
   [(clojure.string/starts-with? (str ?name) ?prefix)]]
 "map")
;; => [map mapv map-indexed mapcat ...]

;; Complex queries with joins
(meta/query
 '[:find ?ns-name ?count
   :where
   [?e :ns ?ns-obj]
   [?ns-obj :name ?ns-name]
   [_ :ns ?ns-obj]
   [(count ?e) ?count]])
;; => [["clojure.core" 742] ["clojure.string" 42] ...]
```

**DataScript schema:**
- Each IMeta's metadata map → one entity
- Special attributes: `:imeta/this`, `:imeta/symbol`, `:imeta/type`
- For vars: `:imeta/value`, `:imeta/value-type`
- All metadata map entries → entity attributes

### Workflow 5: Creating Custom Metadata Providers

Extend Metazoa with your own metadata providers:

```clojure
(require '[glossa.metazoa.api :as meta.api])

;; Define a custom provider - just use metadata
(defn my-function
  "Does something cool."
  {:my.app/performance-notes
   {:time-complexity "O(n log n)"
    :space-complexity "O(n)"
    :benchmarks {:small-input "5ms"
                 :large-input "500ms"}}}
  [data]
  (sort data))

;; Implement rendering for your provider
(defmethod meta.api/render-metadata :my.app/performance-notes
  [imeta k]
  (let [{:keys [time-complexity space-complexity benchmarks]} (k (meta imeta))]
    (str "Performance:\n"
         "  Time: " time-complexity "\n"
         "  Space: " space-complexity "\n"
         "  Benchmarks:\n"
         (clojure.string/join "\n"
           (map (fn [[k v]] (str "    " k ": " v)) benchmarks)))))

;; View your custom metadata
(meta/view #'my-function :my.app/performance-notes)
;; [out] Performance:
;; [out]   Time: O(n log n)
;; [out]   Space: O(n)
;; [out]   Benchmarks:
;; [out]     small-input: 5ms
;; [out]     large-input: 500ms

;; Implement checking for your provider
(defmethod meta.api/check-metadata :my.app/performance-notes
  [imeta k]
  (let [perf (k (meta imeta))
        required-keys [:time-complexity :space-complexity]]
    {:valid? (every? perf required-keys)
     :missing-keys (remove perf required-keys)}))

;; Customize search indexing
(defmethod meta.api/index-for-search :my.app/performance-notes
  [imeta k]
  (let [{:keys [time-complexity space-complexity]} (k (meta imeta))]
    {:lucene
     {:field :text-field
      :stored? false
      :value (str time-complexity " " space-complexity)}}))
```

## When to Use Each Function

**Use `meta/view` when:**
- Exploring metadata at the REPL
- Reviewing examples before testing
- Understanding how a function works
- Developing new metadata providers
- Creating documentation

**Use `meta/check` when:**
- Validating metadata examples are still correct
- Getting data about metadata validity
- Building custom test frameworks
- Debugging metadata issues

**Use `meta/test-imeta` and `meta/test-imetas` when:**
- Integrating metadata testing into clojure.test suite
- Running CI/CD checks on metadata
- Ensuring documentation stays up-to-date
- Preventing regressions in examples

**Use `meta/search` when:**
- Finding functions by partial name
- Locating all usages of custom metadata
- Discovering functions with specific characteristics
- Exploring unfamiliar codebases
- Building code navigation tools

**Use `meta/query` when:**
- Performing complex metadata analysis
- Finding patterns across your codebase
- Generating reports on code characteristics
- Building tools that analyze metadata structure
- Need precise control over query logic

## Best Practices

**Do:**
- Start with `(meta/help)` to learn interactively
- Add `:expected` values to examples for testing
- Use `meta/check` before `meta/test-imeta` for debugging
- Leverage search and query together - search for discovery, query for analysis
- Create custom metadata providers for domain-specific documentation
- Thread `meta/view` calls to see multiple providers
- Use `:num-hits` or `:limit` in search to control result size
- Document metadata provider schemas (see `glossa.metazoa.provider` namespaces)

**Don't:**
- Forget to add `:ns` to example metadata (required for evaluation)
- Index private vars unless you need them (use custom `meta/reset-search`)
- Assume all optional dependencies are available - handle exceptions gracefully
- Make examples too complex - keep them focused and testable
- Forget that `meta/view` returns the IMeta, not nil

## Common Issues

### Issue: "No metadata found"

```clojure
(meta/view #'my-function :glossa.metazoa/example)
;; Nothing prints
```

**Cause:** The function doesn't have that metadata key.

**Solution:**
```clojure
;; Check what providers are available
(meta/providers #'my-function)
;; => (:glossa.metazoa/doc)

;; Add example metadata
(alter-meta! #'my-function assoc :glossa.metazoa/example
  {:code '(my-function "test")
   :expected "result"
   :ns *ns*})
```

### Issue: "Search indexing takes too long"

**Cause:** Indexing includes dependencies by default.

**Solution:** Reset search to index only your project:
```clojure
;; Index only your project namespaces
(meta/reset-search
 (meta.api/find-imetas 
  (fn [ns] 
    (when (clojure.string/starts-with? (str (ns-name ns)) "my.project")
      (conj ((comp vals ns-publics) ns) ns)))))
```

### Issue: "Optional dependency not found"

```clojure
(meta/search "test")
;; ExceptionInfo: Lucene dependencies not available
```

**Cause:** You excluded optional dependencies.

**Solution:** Add them back or don't use those features:
```clojure
;; Add back Lucene for search
{dev.glossa/metazoa {:mvn/version "0.2.298"}
 org.apache.lucene/lucene-core {:mvn/version "8.9.0"}
 org.apache.lucene/lucene-queryparser {:mvn/version "8.9.0"}}
```

### Issue: "Example check fails unexpectedly"

```clojure
(meta/check #'my-function :glossa.metazoa/example)
;; => [{:expected 10, :actual 11}]
```

**Cause:** Code or behavior changed, example is outdated.

**Solution:** Update the metadata:
```clojure
;; Option 1: Fix the code
;; Option 2: Update the example
(alter-meta! #'my-function update :glossa.metazoa/example
  assoc :expected 11)

;; Option 3: Check actual output to understand difference
(meta/check #'my-function :glossa.metazoa/example)
;; Look at :actual, :actual-out, :actual-err fields
```

### Issue: "Query returns empty results"

```clojure
(meta/query
 '[:find [?name ...]
   :where
   [?e :name ?name]
   [?e :added "1.4"]])
;; => []
```

**Cause:** The `:added` attribute might not exist or value differs.

**Solution:** Inspect what's available:
```clojure
;; Find a sample entity first
(meta/query
 '[:find (pull ?e [*]) .
   :where
   [?e :name 'map]])
;; => {:name map, :ns #object[...], :arglists ([f] [f coll]), ...}

;; Adjust query based on actual schema
```

## Advanced Usage

### Batch Testing All Metadata

```clojure
(ns my-project.metadata-test
  (:require [clojure.test :refer [deftest]]
            [glossa.metazoa :as meta]))

(deftest all-metadata-valid
  "Ensures all metadata providers remain valid."
  (meta/test-imetas))
```

### Custom Search Fields

```clojure
(require '[glossa.metazoa.api :as meta.api])

(defmethod meta.api/index-for-search :my.app/complexity
  [imeta k]
  (let [{:keys [time-complexity space-complexity]} (k (meta imeta))]
    {:lucene
     {:index-fn
      (fn [doc]
        (let [TextField org.apache.lucene.document.TextField
              Field$Store org.apache.lucene.document.Field$Store]
          (.add doc (TextField. "time-complexity" 
                                (str time-complexity) 
                                Field$Store/NO))
          (.add doc (TextField. "space-complexity" 
                                (str space-complexity) 
                                Field$Store/NO))))}}))

;; Search by custom fields
(meta/search "time-complexity:O(n)")
```

### Programmatic Metadata Analysis

```clojure
;; Find all functions without docstrings in your project
(defn undocumented-functions []
  (meta/query
   '[:find [?sym ...]
     :in $ package
     :where
     [?e :ns ?ns]
     [(package ?ns)]
     [?e :imeta/symbol ?sym]
     [?e :imeta/value ?value]
     [(clojure.core/fn? ?value)]
     (not [?e :doc])]
   (fn package [ns] 
     (clojure.string/starts-with? (str (ns-name ns)) "my.project"))))

;; Generate a report
(defn metadata-coverage-report []
  (let [all-fns (meta/query
                 '[:find [(count ?e)]
                   :in $ package
                   :where
                   [?e :ns ?ns]
                   [(package ?ns)]
                   [?e :imeta/value ?v]
                   [(clojure.core/fn? ?v)]]
                 (fn [ns] (clojure.string/starts-with? (str (ns-name ns)) "my.project")))
        with-examples (meta/query
                       '[:find [(count ?e)]
                         :in $ package
                         :where
                         [?e :ns ?ns]
                         [(package ?ns)]
                         [?e :glossa.metazoa/example]]
                       (fn [ns] (clojure.string/starts-with? (str (ns-name ns)) "my.project")))]
    {:total-functions (first all-fns)
     :with-examples (first with-examples)
     :coverage-percent (* 100.0 (/ (first with-examples) (first all-fns)))}))
```

### Integrating with Documentation Generation

```clojure
;; Extract all examples for documentation
(defn extract-examples [namespace-sym]
  (meta/query
   '[:find ?sym ?example
     :in $ ?ns
     :where
     [?e :ns ?ns]
     [?e :imeta/symbol ?sym]
     [?e :glossa.metazoa/example ?example]]
   (the-ns namespace-sym)))

;; Use with codox or other doc generators
```

## Related Skills and Tools

### Related Skills

- **[Clojure REPL](../../language/clojure_repl.md)**: Essential for interactive metadata exploration. Use `clj-mcp.repl-tools` for namespace/symbol discovery alongside Metazoa's metadata viewing.
- **[Malli](../data_validation/malli.md)**: Schema validation library used by Metazoa for validating metadata provider schemas.

### Related Tools

- **[Weave](https://gitlab.com/glossa/weave)**: Document format used by `:glossa.metazoa/doc` provider
- **DataScript**: Powers `meta/query` Datalog queries
- **Apache Lucene**: Powers `meta/search` full-text search
- **cljfmt**: Used for code formatting in metadata examples
- **clojure.test**: Integrates with `meta/test-imeta` and `meta/test-imetas`

## Optional Dependencies

Metazoa includes these dependencies by default (can be excluded):

```clojure
:exclusions [cljfmt/cljfmt                           ; Code formatting
             datascript/datascript                   ; Datalog queries
             metosin/malli                           ; Schema validation
             org.apache.lucene/lucene-core           ; Search indexing
             org.apache.lucene/lucene-queryparser]   ; Search queries
```

**Feature dependencies:**
- `meta/search` requires Lucene (will throw exception if missing)
- `meta/query` requires DataScript (will throw exception if missing)
- Code formatting and schema validation skip silently if dependencies missing

## External Resources

- [GitLab Repository](https://gitlab.com/glossa/metazoa)
- [Introductory Video](https://www.youtube.com/watch?v=gSSh9srEE78)
- [Motivation and Background Article](https://www.danielgregoire.dev/posts/2021-10-15-clojure-src-test-meta/)
- [Clojars Package](https://clojars.org/dev.glossa/metazoa)
