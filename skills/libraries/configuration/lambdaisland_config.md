---
name: lambdaisland_config
description: |
  Manage application configuration from multiple sources with lambdaisland/config. 
  Use when managing environment-specific settings, loading config from EDN files, 
  environment variables, system properties, or when the user mentions configuration, 
  settings, env vars, config files, dev/prod environments, or deployment settings.
---

# lambdaisland/config - Configuration Management

## Quick Start

lambdaisland/config merges configuration from multiple sources with environment-specific overrides:

```clojure
(require '[lambdaisland.config :as config])

;; Create config with app prefix
(def cfg
  (config/create {:prefix "my-app"
                  :env :dev}))

;; Get config values
(config/get cfg :http/port)
;; => 8080

;; Know where a value came from
(config/source cfg :http/port)
;; => "my-app/dev.edn on the classpath"

;; Get all config entries
(config/entries cfg)
;; => {:http/port 8080, :db/url "jdbc:postgresql://localhost/mydb"}
```

**Key benefits:**
- **Multiple sources** - EDN files, env vars, system properties, command-line flags
- **Environment-specific** - Separate config for dev, test, staging, prod
- **Provenance tracking** - Know where each config value came from
- **Opinionated defaults** - Sensible conventions that work out of the box
- **Extensible** - Custom ConfigProvider protocol for secret stores, etc.

## Core Concepts

### Configuration Sources (Priority Order)

lambdaisland/config checks sources in this order (first match wins):

1. **Environment variables** - `$HTTP__PORT` or `$MY_APP__HTTP__PORT`
2. **Java system properties** - `my-app.http.port`
3. **Local config file** - `config.local.edn` (not checked in)
4. **XDG config** - `~/.config/my-app.edn`
5. **System config** - `/etc/my-app.edn`
6. **Environment-specific** - `resources/my-app/dev.edn` (or prod, test, etc.)
7. **Base config** - `resources/my-app/config.edn`

### The Config Map

The config object is a map with three keys:

```clojure
(def cfg (config/create {:prefix "my-app" :env :dev}))

;; Structure:
{:env :dev                    ; Current environment
 :providers [...]             ; List of ConfigProvider instances
 :values (atom {...})}        ; Cache of accessed values
```

### Environment Detection

If you don't specify `:env`, it's determined automatically:

```clojure
;; Checks in order:
;; 1. $MY_APP__ENV environment variable
;; 2. my-app.env system property
;; 3. If $CI=true, defaults to :test
;; 4. Otherwise defaults to :dev

(def cfg (config/create {:prefix "my-app"}))
;; Env determined automatically
```

### Aero Support

All EDN files are read with Aero, supporting reader macros:

```clojure
;; In resources/my-app/config.edn:
{:http/port #or [#env PORT 8080]
 :db/password #env DB_PASSWORD
 :debug? #profile {:dev true :prod false}}
```

## Common Workflows

### Workflow 1: Basic App Setup

Recommended file structure:

```clojure
;; 1. Create resources/my-app/config.edn (base config with defaults)
{:http/port 8080
 :http/host "0.0.0.0"
 :db/pool-size 10
 :log/level :info}

;; 2. Create resources/my-app/dev.edn (dev overrides)
{:http/port 3000
 :log/level :debug
 :db/url "jdbc:postgresql://localhost/myapp_dev"}

;; 3. Create resources/my-app/prod.edn (prod settings)
{:db/pool-size 50
 :log/level :warn}

;; 4. Create config.local.edn at project root (not checked in)
{:db/password "local-secret"}

;; 5. In your code:
(ns my-app.main
  (:require [lambdaisland.config :as config]))

(def cfg (config/create {:prefix "my-app"}))

(defn start []
  (let [port (config/get cfg :http/port)
        host (config/get cfg :http/host)]
    (println "Starting server on" host ":" port)
    ;; ...
    ))
```

### Workflow 2: Environment Variables

Map between env vars and config keys:

```clojure
;; Without prefix (default):
;; HTTP__PORT maps to :http/port
;; DB__URL maps to :db/url

;; With prefix:
(def cfg (config/create {:prefix "my-app" 
                         :prefix-env true}))
;; MY_APP__HTTP__PORT maps to :http/port

;; From shell:
;; $ export HTTP__PORT=9000
;; $ clojure -M -m my-app.main

(config/get cfg :http/port)
;; => 9000
```

**Naming convention:** Double underscores separate namespace/name, single underscores are part of the key.

### Workflow 3: Java System Properties

Override via command-line:

```clojure
;; Start with system property:
;; $ clojure -J-Dmy-app.http.port=9000 -M -m my-app.main

(def cfg (config/create {:prefix "my-app"}))

(config/get cfg :http/port)
;; => 9000

;; Docker example - bake in environment:
;; CMD ["java", "-Dmy-app.env=prod", "-jar", "app.jar"]
```

### Workflow 4: Provenance Tracking

Debug configuration by tracking sources:

```clojure
(require '[clojure.pprint :as pp])

;; Print all config entries with sources
(pp/print-table
  (map (fn [[k v]]
         {:key k
          :value v
          :source (config/source cfg k)})
       (config/entries cfg)))

;; Output:
;; | :key        | :value                          | :source                          |
;; |-------------+---------------------------------+----------------------------------|
;; | :http/port  | 3000                            | my-app/dev.edn on the classpath  |
;; | :db/url     | jdbc:postgresql://localhost/... | config.local.edn                 |
;; | :db/password| secret123                       | $DB__PASSWORD environment var    |
```

### Workflow 5: Custom ConfigProvider

Add custom config sources (e.g., secret store):

```clojure
(require '[lambdaisland.config :as config])

;; Implement ConfigProvider protocol
(defrecord VaultProvider [vault-client]
  config/ConfigProvider
  (-value [this k]
    ;; Fetch from Vault
    (get-secret vault-client k))
  
  (-source [this k]
    (str "Hashicorp Vault: " k))
  
  (-reload [this]
    ;; Refresh connection if needed
    nil))

;; Add to config
(def cfg
  (-> (config/create {:prefix "my-app"})
      (update :providers conj (->VaultProvider vault-client))))

;; Now config/get will check Vault too
(config/get cfg :db/password)
```

### Workflow 6: Integration with lambdaisland/cli

Combine with command-line parsing:

```clojure
(require '[lambdaisland.cli :as cli]
         '[lambdaisland.config :as config]
         '[lambdaisland.config.cli :as config-cli])

;; Add CLI provider to config
(def cfg
  (-> (config/create {:prefix "my-app"})
      config-cli/add-provider))

;; Define CLI spec
(def cmdspec
  {:name "my-app"
   :doc "My application"
   :commands
   ["start" #'start-cmd]
   :flags
   ["--port <port>" {:key :http/port
                     :doc "HTTP port"}]
   ["--env <env>" {:key :env
                   :parse-fn keyword
                   :doc "Environment (dev/prod)"}]})

(defn start-cmd [opts]
  (println "Port:" (config/get cfg :http/port))
  (println "Source:" (config/source cfg :http/port)))

(defn -main [& argv]
  (cli/dispatch* cmdspec argv))

;; Usage:
;; $ clojure -M -m my-app start --port 9000
;; Port: 9000
;; Source: --port command line flag
```

### Workflow 7: Reloading Configuration

Support dynamic config updates:

```clojure
;; Reload all providers
(config/reload cfg)

;; Clear cached values
(reset! (:values cfg) {})

;; Next access will re-read from sources
(config/get cfg :http/port)
```

## Decision Guide: Configuration Approaches

| Approach | When to Use | Example |
|----------|-------------|---------|
| **EDN files** | Default config, environment-specific settings | Base defaults in `config.edn`, dev overrides in `dev.edn` |
| **Environment variables** | Cloud deployments, Docker, Kubernetes | `$DATABASE_URL` for Heroku/AWS |
| **System properties** | JVM-specific overrides, Docker CMD | `-Dapp.env=prod` baked into container |
| **config.local.edn** | Local development secrets, personal settings | Database passwords, API keys (not checked in) |
| **XDG config** | User-specific settings for CLI tools | `~/.config/my-app.edn` for global tools |
| **Command-line flags** | Runtime overrides, one-off changes | `--port 9000` to test on different port |
| **Custom provider** | Secret stores, remote config | Vault, AWS Secrets Manager, Consul |

## Best Practices

### Organize Configuration Files

```clojure
;; Directory structure:
resources/
  my-app/
    config.edn       ; Base config with all keys and sensible defaults
    dev.edn          ; Development overrides
    test.edn         ; Test environment settings
    staging.edn      ; Staging environment
    prod.edn         ; Production settings

;; At project root (gitignored):
config.local.edn     ; Local secrets and overrides
```

### Use Namespaced Keywords

```clojure
;; Good - clear organization
{:http/port 8080
 :http/host "0.0.0.0"
 :db/url "jdbc:..."
 :db/pool-size 10}

;; Avoid - flat namespace
{:port 8080
 :host "0.0.0.0"
 :url "jdbc:..."
 :pool-size 10}
```

### Document Config Keys

```clojure
;; In config.edn - add comments for each key
{;; HTTP server configuration
 :http/port 8080              ; Port to listen on (default: 8080)
 :http/host "0.0.0.0"         ; Host to bind to (0.0.0.0 = all interfaces)
 
 ;; Database configuration
 :db/url "jdbc:postgresql://localhost/mydb"  ; JDBC connection string
 :db/pool-size 10             ; Connection pool size (default: 10)
 :db/timeout 5000}            ; Query timeout in milliseconds
```

### Log Configuration at Startup

```clojure
(defn -main [& args]
  (let [cfg (config/create {:prefix "my-app"})]
    ;; Log where config came from
    (println "Configuration loaded:")
    (doseq [[k v] (config/entries cfg)]
      (println (format "  %s = %s [from: %s]"
                       k v (config/source cfg k))))
    
    ;; Start app
    (start-server cfg)))
```

### Handle Missing Required Config

```clojure
(defn require-config [cfg k]
  (or (config/get cfg k)
      (throw (ex-info (str "Missing required config: " k)
                      {:key k
                       :available-keys (keys (config/entries cfg))}))))

;; Usage:
(def db-url (require-config cfg :db/url))
```

### Use Aero Reader Macros

```clojure
;; In config.edn:
{:http/port #or [#env PORT 8080]          ; Env var with fallback
 :db/password #env DB_PASSWORD            ; Required env var
 :debug? #profile {:dev true :prod false} ; Environment-specific
 :log/level #profile {:dev :debug         ; Different per env
                      :test :warn
                      :prod :info}}
```

## Common Issues

### Issue 1: Config Value Not Found

**Problem:** `(config/get cfg :my/key)` returns `nil`

**Cause:** Key doesn't exist in any config source

```clojure
;; Debug - check all available keys
(keys (config/entries cfg))
;; => (:http/port :db/url ...)

;; Debug - check specific sources
(config/source cfg :my/key)
;; => nil (not found in any source)

;; Solution: Add to config.edn with default
{:my/key "default-value"}
```

### Issue 2: Wrong Config Source Taking Precedence

**Problem:** Getting value from wrong source

```clojure
;; Expected dev.edn value but getting config.local.edn
(config/get cfg :http/port)
;; => 9000 (from config.local.edn)
;; Expected: 3000 (from dev.edn)

;; Solution: Check source priority
(config/source cfg :http/port)
;; => "config.local.edn"

;; Fix: Remove from higher-priority source or accept precedence
;; config.local.edn overrides everything by design
```

### Issue 3: Environment Variable Not Working

**Problem:** Env var not being read

```clojure
;; Shell:
;; $ export HTTP_PORT=9000  # Wrong - single underscore
;; $ clojure -M -m my-app.main

(config/get cfg :http/port)
;; => 8080 (default, not 9000)

;; Solution: Use double underscores
;; $ export HTTP__PORT=9000  # Correct
;; $ clojure -M -m my-app.main

(config/get cfg :http/port)
;; => 9000

;; Or with prefix:
;; $ export MY_APP__HTTP__PORT=9000
(def cfg (config/create {:prefix "my-app" :prefix-env true}))
```

### Issue 4: Aero Reader Macro Errors

**Problem:** EDN parsing fails with Aero macros

```clojure
;; In config.edn:
{:db/password #env DB_PASSWORD}

;; Error: Could not read config: No value for #env DB_PASSWORD

;; Solution 1: Provide the env var
;; $ export DB_PASSWORD=secret
;; $ clojure -M -m my-app.main

;; Solution 2: Use #or for optional env vars
{:db/password #or [#env DB_PASSWORD "dev-password"]}

;; Solution 3: Only require in specific environments
{:db/password #profile {:dev "dev-password"
                        :prod #env DB_PASSWORD}}
```

### Issue 5: Config Not Reloading

**Problem:** Changes to config files not picked up

```clojure
;; Config created once
(def cfg (config/create {:prefix "my-app"}))

;; Change config.edn but still getting old value
(config/get cfg :http/port)
;; => 8080 (cached)

;; Solution: Reload config
(config/reload cfg)
(config/get cfg :http/port)
;; => 3000 (new value)

;; Or clear cache and re-read
(reset! (:values cfg) {})
(config/get cfg :http/port)
```

## Advanced Topics

### ConfigProvider Protocol

```clojure
(defprotocol ConfigProvider
  (-value [this k]
    "Return the value for key k, or nil if not found")
  
  (-source [this k]
    "Return a human-readable string describing where k came from")
  
  (-reload [this]
    "Reload config from the source, return nil"))
```

### Creating Atomic Config Updates

```clojure
;; For apps that need consistent config snapshots
(defn snapshot-config [cfg]
  (into {} (config/entries cfg)))

(def config-snapshot (snapshot-config cfg))

;; Use snapshot instead of live config
(:http/port config-snapshot)
```

### Testing with Custom Config

```clojure
;; In tests - provide static config map
(require '[lambdaisland.config :as config])

(deftest my-test
  (let [test-cfg (config/create {:prefix "my-app"
                                 :env :test
                                 :providers [(config/static-provider
                                              {:http/port 9999
                                               :db/url "jdbc:h2:mem:test"})]})]
    (is (= 9999 (config/get test-cfg :http/port)))))
```

## Related Resources

- [GitHub Repository](https://github.com/lambdaisland/config)
- [lambdaisland/cli](https://github.com/lambdaisland/cli) - Command-line parsing
- [Aero](https://github.com/juxt/aero) - EDN reader macros
- [XDG Base Directory Spec](https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html)

## Summary

lambdaisland/config provides a flexible, opinionated way to manage configuration across multiple sources with environment-specific overrides. It follows a sensible convention that works out of the box but remains highly customizable through the ConfigProvider protocol. Key features include provenance tracking, Aero support, and integration with multiple config sources from environment variables to custom secret stores.
