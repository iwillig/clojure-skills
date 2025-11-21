---
name: clj_commons_pretty
description: |
  Format output with ANSI colors, pretty exceptions, binary dumps, tables, and code annotations.
  Use when formatting exception stack traces, creating colored terminal output, displaying binary
  data, printing tables, or annotating code with error messages. Use when the user mentions
  "pretty print", "format exception", "colored output", "ANSI", "stack trace", "hexdump",
  "binary output", "table formatting", or "code annotations".
sources:
  - https://github.com/clj-commons/pretty
  - https://cljdoc.org/d/org.clj-commons/pretty
---

# clj-commons/pretty

Library for formatted output with ANSI colors, pretty exceptions, binary dumps, tables, and code annotations.

## Quick Start

Pretty provides several independent formatting capabilities. Each can be used standalone:

```clojure
(require '[clj-commons.ansi :as ansi]
         '[clj-commons.format.exceptions :as exceptions]
         '[clj-commons.format.binary :as binary]
         '[clj-commons.format.table :as table])

;; Colored output
(ansi/pout [:bold.red "ERROR:"] " Something went wrong")

;; Pretty exceptions
(try
  (throw (ex-info "Failed" {:user-id 123}))
  (catch Exception e
    (exceptions/print-exception e)))

;; Binary dumps
(binary/print-binary (.getBytes "Hello"))

;; Tables
(table/print-table [:id :name] [{:id 1 :name "Alice"}])
```

## Core Concepts

### ANSI Color Support

Pretty automatically detects if color output is appropriate:
- Enabled in REPLs (nREPL, Cursive, `clj`)
- Disabled if `NO_COLOR` environment variable is set
- Can be controlled via `clj-commons.ansi.enabled` system property

### Composed Strings

Most Pretty functions work with "composed strings" - Hiccup-like data structures that include formatting:

```clojure
[:red "error"]                          ; Simple colored text
[:bold.yellow "Warning"]                ; Multiple font characteristics
[:red "Error: " [:bold "critical"]]     ; Nested formatting
```

## Common Workflows

### Workflow 1: Colored Console Output

Use `ansi/compose` to build formatted strings, `ansi/pout` and `ansi/perr` to print them:

```clojure
(require '[clj-commons.ansi :as ansi])

;; Print to stdout
(ansi/pout [:green.bold "✓"] " Tests passed")

;; Print to stderr
(ansi/perr [:red.bold "✗"] " Tests failed")

;; Build without printing
(def message (ansi/compose [:yellow "Warning: " [:bold "check input"]]))
```

**Font characteristics** (combine with periods):
- Colors: `red`, `green`, `yellow`, `blue`, `magenta`, `cyan`, `white`, `black`
- Bright colors: `bright-red`, `bright-green`, etc.
- Background: `red-bg`, `green-bg`, `bright-blue-bg`, etc.
- Styles: `bold`, `faint`, `italic`, `underlined`, `inverse`, `crossed`
- Extended colors: `color-500` (RGB 5,0,0), `grey-0` through `grey-23`

```clojure
;; Complex formatting
(ansi/pout [:bold.bright-white.red-bg "CRITICAL"] 
           [:red " System overload at " 
            [:bold.yellow (java.time.LocalDateTime/now)]])
```

### Workflow 2: Pretty Exception Formatting

Format exceptions with readable stack traces, property display, and duplicate frame detection:

```clojure
(require '[clj-commons.format.exceptions :as exceptions])

;; Basic exception formatting
(try
  (/ 1 0)
  (catch Exception e
    (exceptions/print-exception e)))

;; Format with options
(try
  (throw (ex-info "Database error" 
                  {:query "SELECT * FROM users"
                   :connection-id 42}))
  (catch Exception e
    (exceptions/print-exception e
      {:frame-limit 10        ; Limit stack frames shown
       :properties true       ; Show exception properties (default)
       :traditional false}))) ; Modern ordering (default)
```

**Key features:**
- Stack frames in chronological order (shallow to deep)
- Clojure function names demangled
- File names and line numbers highlighted
- Exception properties pretty-printed
- Repeated frames collapsed

**Customizing frame filtering:**

```clojure
;; Set application namespaces for highlighting
(alter-var-root #'exceptions/*app-frame-names*
                (constantly #{"myapp" "mycompany"}))

;; Define custom frame filter
(defn my-filter [frame]
  (cond
    (re-find #"test" (:name frame)) :hide
    (= "clojure.core" (:package frame)) :omit
    :else :show))

(exceptions/print-exception e {:filter my-filter})
```

### Workflow 3: Binary Data Visualization

Display byte sequences with color-coded hex dumps and optional ASCII view:

```clojure
(require '[clj-commons.format.binary :as binary])

;; Basic hex dump
(def data (.getBytes "Choose immutability"))
(binary/print-binary data)
; 0000: 43 68 6F 6F 73 65 20 69 6D 6D 75 74 61 62 69 6C
; 0010: 69 74 79

;; With ASCII sidebar (16 bytes per line)
(binary/print-binary data {:ascii true})
; 0000: 43 68 6F 6F 73 65 20 69 6D 6D 75 74 61 62 69 6C │Choose immutabil│
; 0010: 69 74 79                                        │ity             │

;; Custom line width (default 32, or 16 with :ascii)
(binary/print-binary data {:line-bytes 8})

;; Compare two byte sequences
(def expected (.getBytes "Hello World"))
(def actual (.getBytes "Hello Clojure"))
(binary/print-binary-delta expected actual)
; Differences highlighted in green (expected) and red (actual)
```

**Byte color coding:**
- ASCII printable: cyan
- Whitespace: green
- Control characters: red
- Extended ASCII: yellow

### Workflow 4: Table Formatting

Print data as formatted tables with borders, alignment, and custom styling:

```clojure
(require '[clj-commons.format.table :as table])

(def users
  [{:id 1 :name "Alice" :role :admin}
   {:id 2 :name "Bob" :role :user}
   {:id 3 :name "Charlie" :role :user}])

;; Simple table
(table/print-table [:id :name :role] users)
; ┌──┬───────┬─────┐
; │Id│  Name │Role │
; ├──┼───────┼─────┤
; │ 1│  Alice│:admin│
; │ 2│    Bob│:user │
; │ 3│Charlie│:user │
; └──┴───────┴─────┘

;; Custom column configuration
(table/print-table
  [{:key :id :title "ID" :align :right}
   {:key :name :title "User Name" :width 15}
   {:key :role 
    :title "Role"
    :formatter #(if (= :admin %) "Administrator" "User")
    :decorator (fn [idx val] (when (= :admin val) :bold.green))}]
  users)

;; Different table styles
(table/print-table
  {:columns [:id :name :role]
   :style table/skinny-style}
  users)
; ID | Name    | Role
; ---+---------+------
;  1 | Alice   | :admin
;  2 | Bob     | :user
;  3 | Charlie | :user

(table/print-table
  {:columns [:id :name :role]
   :style table/minimal-style}
  users)
; ID  Name     Role
;  1  Alice    :admin
;  2  Bob      :user
;  3  Charlie  :user
```

**Column options:**
- `:key` - Keyword or function to extract value (required)
- `:title` - Column header (defaults to capitalized key)
- `:width` - Fixed width or auto-calculated
- `:align` - `:left`, `:right` (default), or `:center`
- `:title-align` - Alignment for title (default `:center`)
- `:formatter` - Function to format cell value
- `:decorator` - Function returning font declaration for cell

**Table options:**
- `:columns` - Vector of columns
- `:style` - `default-style`, `skinny-style`, or `minimal-style`
- `:row-decorator` - Function to style entire rows
- `:row-annotator` - Function to add notes after rows

### Workflow 5: Code Annotations

Annotate source code with error markers and messages:

```clojure
(require '[clj-commons.pretty.annotations :as ann]
         '[clj-commons.ansi :as ansi])

;; Annotate a single line
(def source "SELECT DATE, AMT FROM PAYMENTS")
(ansi/perr source)
(run! ansi/perr
  (ann/callouts [{:offset 7 
                  :length 4 
                  :message "Invalid column name"}]))
; SELECT DATE, AMT FROM PAYMENTS
;        ▲▲▲▲
;        │
;        └╴ Invalid column name

;; Multiple annotations on one line
(run! ansi/perr
  (ann/callouts [{:offset 7 :length 4 :message "Invalid column"}
                 {:offset 17 :length 8 :message "Unknown table"}]))
; SELECT DATE, AMT FROM PAYMENTS
;        ▲▲▲▲         ▲▲▲▲▲▲▲▲
;        │            │
;        │            └╴ Unknown table
;        └╴ Invalid column

;; Annotate multiple lines
(def lines
  [{:line "SELECT DATE, AMT"
    :annotations [{:offset 7 :length 4 :message "Invalid column"}]}
   {:line "FROM PAYMENTS WHERE AMT > 10000"
    :annotations [{:offset 13 :length 5 :message "Unknown keyword"}]}])

(run! ansi/perr (ann/annotate-lines lines))
; 1: SELECT DATE, AMT
;           ▲▲▲▲
;           │
;           └╴ Invalid column
; 2: FROM PAYMENTS WHERE AMT > 10000
;                  ▲▲▲▲▲
;                  │
;                  └╴ Unknown keyword

;; Custom styling
(run! ansi/perr
  (ann/callouts 
    {:font :red.bold
     :marker "^"
     :spacing :minimal}
    [{:offset 7 :length 4 :message "Error here"}]))
```

**Annotation options:**
- `:offset` - Column position (0-based, required)
- `:length` - Characters to mark (default 1)
- `:message` - Error message (composed string)
- `:font` - Override style font for this annotation
- `:marker` - Override marker character(s)

**Style options:**
- `:font` - Default font (default `:yellow`)
- `:spacing` - `:tall`, `:compact` (default), or `:minimal`
- `:marker` - Marker string or function (default "▲")
- `:bar` - Vertical bar character (default "│")
- `:nib` - Connection before message (default "└╴ ")

### Workflow 6: Enable Pretty Exceptions in REPL

Install pretty exception printing for your development environment:

```clojure
;; In user.clj or at REPL startup
(require '[clj-commons.pretty.repl :as pretty-repl])
(pretty-repl/install-pretty-exceptions)

;; Now all REPL exceptions use pretty formatting
(/ 1 0)
; Pretty formatted ArithmeticException output
```

**Project integration:**

For Leiningen (`~/.lein/profiles.d/debug.clj`):
```clojure
{:dependencies [[org.clj-commons/pretty "3.6.7"]]
 :injections [(require '[clj-commons.pretty.repl :as repl])
              (repl/install-pretty-exceptions)]}
```

For deps.edn (`:debug` alias):
```clojure
{:aliases
 {:debug
  {:extra-deps {org.clj-commons/pretty {:mvn/version "3.6.7"}}
   :exec-fn clj-commons.pretty.repl/main}}}
```

For nREPL middleware:
```clojure
;; Add to .nrepl.edn or nrepl config
{:middleware [clj-commons.pretty.nrepl/wrap-pretty]}
```

## When to Use Each Feature

**Use ANSI colors when:**
- Creating CLI tools with colored output
- Highlighting important information in logs
- Building developer tools and REPLs
- Formatting success/error/warning messages

**Use exception formatting when:**
- Debugging complex stack traces
- Building error reporting tools
- Creating developer-friendly error messages
- Need to understand nested exceptions

**Use binary formatting when:**
- Debugging binary protocols
- Comparing byte sequences
- Analyzing file formats
- Inspecting serialized data

**Use table formatting when:**
- Displaying query results
- Showing configuration data
- Comparing multiple items
- Creating CLI reports

**Use code annotations when:**
- Building error reporters for parsers
- Highlighting syntax errors
- Creating educational tools
- Showing code issues in tooling

## Best Practices

**Do:**
- Check if color is enabled before complex formatting: `(ansi/when-color-enabled ...)`
- Use composed strings for flexibility
- Set `*app-frame-names*` to highlight your code in stack traces
- Provide custom exception dispatch for complex types
- Use appropriate table styles for your use case
- Keep annotation messages concise (no line breaks)

**Don't:**
- Generate ANSI codes manually - use `compose`
- Mix Pretty's exception formatting with manual `.printStackTrace`
- Forget that composed strings need `compose` to become actual strings
- Create deeply nested font definitions - keep formatting simple
- Use `:ascii` mode for binary output wider than 16 bytes per line
- Overlap annotation ranges on the same line

## Common Issues

### "ANSI codes appear in output"

Colors disabled but codes still showing:

```clojure
;; Check if colors are enabled
ansi/*color-enabled*  ; => false

;; Colors are explicitly disabled
;; Either: NO_COLOR env var is set
;; Or: clj-commons.ansi.enabled system property is "false"
;; Or: No console/REPL detected

;; Force enable (for testing)
(alter-var-root #'ansi/*color-enabled* (constantly true))
```

### "Stack trace still looks like Java output"

Pretty exceptions not installed:

```clojure
;; Install pretty exceptions
(require '[clj-commons.pretty.repl :as pretty-repl])
(pretty-repl/install-pretty-exceptions)

;; Or check if already installed
(pretty-repl/install-pretty-exceptions)  ; Safe to call multiple times
```

### "Binary output shows wrong width"

Line width calculation doesn't account for tabs/ANSI codes:

```clojure
;; Specify explicit line width
(binary/print-binary data {:line-bytes 16})

;; For ASCII mode, always use 16 bytes per line
(binary/print-binary data {:ascii true :line-bytes 16})
```

### "Table columns too wide/narrow"

Width auto-calculated from data:

```clojure
;; Specify explicit width
(table/print-table
  [{:key :name :width 20}
   {:key :description :width 50}]
  data)

;; Or use formatters to control content
(table/print-table
  [{:key :name}
   {:key :description
    :formatter #(subs % 0 (min 50 (count %)))}]
  data)
```

### "Annotations overlap"

Multiple annotations with overlapping ranges:

```clojure
;; Bad: overlapping ranges
[{:offset 5 :length 10}
 {:offset 8 :length 5}]  ; Overlaps with first

;; Good: non-overlapping
[{:offset 5 :length 3}
 {:offset 10 :length 5}]

;; Annotations automatically sorted by offset
;; But overlaps still cause visual issues
```

## Advanced Topics

### Custom Exception Dispatch

Control how specific types appear in exception output:

```clojure
(import 'com.stuartsierra.component.SystemMap)

(defmethod exceptions/exception-dispatch SystemMap 
  [system-map]
  (print "#<SystemMap>"))

;; Now SystemMap instances show as "#<SystemMap>" instead of full structure
```

### Custom Table Decorators

Add visual styling to table rows and cells:

```clojure
(table/print-table
  {:columns [:status :message]
   :row-decorator (fn [idx row]
                    (case (:status row)
                      :error :red
                      :warning :yellow
                      :success :green
                      nil))}
  [{:status :error :message "Failed"}
   {:status :success :message "OK"}])
```

### Custom Annotation Markers

Create custom marker functions:

```clojure
(defn wave-marker [length]
  (apply str (repeat length "~")))

(run! ansi/perr
  (ann/callouts 
    {:marker wave-marker}
    [{:offset 5 :length 10 :message "Issue here"}]))
; Some text here with a problem
;      ~~~~~~~~~~
;      │
;      └╴ Issue here
```

### Parsing Exception Text

Convert text-based exception output back to Pretty's format:

```clojure
;; Useful for processing exception logs
(def exception-text 
  "java.lang.ArithmeticException: Divide by zero
    at clojure.lang.Numbers.divide(Numbers.java:188)
    at user$eval123.invokeStatic(REPL:1)")

(def parsed (exceptions/parse-exception exception-text {}))
(exceptions/print-exception* parsed {})
```

## Performance Considerations

- **ANSI composition** is fast; overhead is minimal
- **Exception formatting** processes entire stack trace; use `:frame-limit` for very deep stacks
- **Binary formatting** with `:ascii` doubles memory usage (hex + ASCII)
- **Table formatting** calculates widths from all data; slow for huge datasets
- **Annotations** are fast; multiple annotations per line add minimal overhead

For large datasets or performance-critical paths, consider:
- Format once, cache the result
- Use `:frame-limit` to reduce exception output
- Limit table row count for display
- Use streaming approaches for binary data

## Related Libraries

- `clojure.pprint` - Basic pretty printing (Pretty extends this)
- `puget` - Alternative pretty printer with color support
- `fipp` - Fast pretty printer
- `bling` - Terminal UI components and coloring

## Resources

- [GitHub Repository](https://github.com/clj-commons/pretty)
- [API Documentation](https://cljdoc.org/d/org.clj-commons/pretty)
- [CLJ Commons](https://clj-commons.org/)

## Summary

`clj-commons/pretty` provides five independent formatting capabilities:

1. **ANSI colors** (`clj-commons.ansi`) - Colored terminal output
2. **Exception formatting** (`clj-commons.format.exceptions`) - Readable stack traces
3. **Binary visualization** (`clj-commons.format.binary`) - Hex dumps and deltas
4. **Table formatting** (`clj-commons.format.table`) - Pretty tabular output
5. **Code annotations** (`clj-commons.pretty.annotations`) - Error markers on source

Each can be used independently or combined for comprehensive formatted output.
