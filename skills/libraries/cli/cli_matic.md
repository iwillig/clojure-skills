---
name: cli_matic_command_line_interface
description: |
  Build command-line interfaces with data-driven command definitions. Use when 
  building CLI tools, parsing command-line arguments, multi-command applications, 
  subcommands, or when the user mentions CLI, command-line, args parsing, terminal 
  tools, command structure, or CLI applications.
---

# cli-matic

A library for parsing command-line arguments and building CLI applications in Clojure.

## Overview

cli-matic provides a data-driven approach to building command-line interfaces. Define your commands as data structures, and the library handles parsing, validation, and help generation.

## Core Concepts

**Command Definition**: Define CLI commands as data.

```clojure
(require '[cli-matic.core :refer [run-cli]])

(def cli-definition
  {:app-name "myapp"
   :version "1.0.0"
   :description "My CLI application"
   :commands
   [{:command "create"
     :description "Create a new user"
     :opts [{:as "Name" :long "name" :required true}
            {:as "Email" :long "email" :required true}]
     :runs create-user}
    {:command "list"
     :description "List all users"
     :runs list-users}]})
```

**Command Execution**: Parse arguments and run appropriate handler.

```clojure
(defn create-user [{:keys [name email]}]
  (println (str "Creating user: " name " (" email ")")))

(defn list-users [_]
  (println "Listing users..."))

; In main function
(defn -main [& args]
  (run-cli args cli-definition))
```

## Hierarchical Subcommands

cli-matic supports hierarchical subcommands for organizing complex CLI applications:

```clojure
(def cli-definition
  {:app-name "myapp"
   :version "1.0.0"
   :description "My CLI application"
   :subcommands
   [{:command "db"
     :description "Database operations"
     :subcommands
     [{:command "init"
       :description "Initialize database"
       :runs init-db}
      {:command "migrate"
       :description "Run migrations"
       :runs migrate-db}]}
    {:command "user"
     :description "User operations"
     :subcommands
     [{:command "create"
       :description "Create user"
       :opts [{:as "Name" :long "name" :required true}]
       :runs create-user}
      {:command "list"
       :description "List users"
       :runs list-users}]}]})
```

This creates a command structure like:
```bash
myapp db init
myapp db migrate
myapp user create --name "John"
myapp user list
```

## Key Features

- Data-driven command definition
- Automatic argument parsing
- Help generation
- Subcommand support
- Hierarchical command structure
- Validation of arguments
- Type conversion
- Exit codes

## When to Use

- Building CLI tools in Clojure
- Command-line argument parsing
- Multi-command applications
- Tools with structured commands
- Complex CLI hierarchies

## When NOT to Use

- Simple single-command scripts (plain args handling)
- Complex interactive CLIs (use alternative libraries)

## Common Patterns

```clojure
(require '[cli-matic.core :refer [run-cli]])

(defn main-command [{:keys [config verbose]}]
  (when verbose
    (println "Verbose mode enabled"))
  (println (str "Using config: " config)))

(defn process-file [{:keys [input output format]}]
  (println (str "Processing " input " -> " output " (format: " format ")")))

(def cli-definition
  {:app-name "processor"
   :version "1.0.0"
   :subcommands
   [{:command "process"
     :description "Process a file"
     :opts [{:as "Input file" :long "input" :required true}
            {:as "Output file" :long "output" :required true}
            {:as "Format" :long "format" :default "json"}]
     :runs process-file}
    {:command "config"
     :description "Configuration operations"
     :subcommands
     [{:command "show"
       :description "Show current configuration"
       :runs show-config}
      {:command "set"
       :description "Set configuration value"
       :opts [{:as "Key" :long "key" :required true}
              {:as "Value" :long "value" :required true}]
       :runs set-config}]}]})

(defn -main [& args]
  (run-cli args cli-definition))
```

## Related Libraries

- org.clojure/tools.cli - Lower-level CLI parsing
- babashka/babashka - Scripting with built-in CLI support

## Resources

- Official Documentation: https://github.com/l3nz/cli-matic
- API Documentation: https://cljdoc.org/d/cli-matic/cli-matic

## Notes

This project uses cli-matic for building command-line interfaces with hierarchical subcommands.
