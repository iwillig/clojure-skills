(ns clojure-skills.logging
  "Logging utilities using μ/log for structured event-based logging."
  (:require
   [com.brunobonacci.mulog :as μ]))


(defonce publisher
  (atom nil))


(defn start-logging!
  "Initialize μ/log publisher for console output."
  []
  (when-not @publisher
    (reset! publisher
            (μ/start-publisher! {:type :console :pretty? true}))))


(defn stop-logging!
  "Stop the μ/log publisher."
  []
  (when-let [p @publisher]
    (p)
    (reset! publisher nil)))


(defn set-global-context!
  "Set global context for all log events."
  []
  (μ/set-global-context!
   {:app-name "clojure-skills"
    :version "1.0.0"
    :host (.getHostName (java.net.InetAddress/getLocalHost))}))


(defn log-success
  "Log a success message."
  [message & {:as kv-data}]
  (μ/log ::success-message
         :message message
         :timestamp (System/currentTimeMillis)
         :level :success
         kv-data))


(defn log-error
  "Log an error message."
  [message & {:as kv-data}]
  (μ/log ::error-message
         :message message
         :timestamp (System/currentTimeMillis)
         :level :error
         kv-data))


(defn log-info
  "Log an info message."
  [message & {:as kv-data}]
  (μ/log ::info-message
         :message message
         :timestamp (System/currentTimeMillis)
         :level :info
         kv-data))


(defn log-warning
  "Log a warning message."
  [message & {:as kv-data}]
  (μ/log ::warning-message
         :message message
         :timestamp (System/currentTimeMillis)
         :level :warning
         kv-data))


(defn log-debug
  "Log a debug message."
  [message & {:as kv-data}]
  (μ/log ::debug-message
         :message message
         :timestamp (System/currentTimeMillis)
         :level :debug
         kv-data))


(defn log-exception
  "Log an exception with error details."
  [message exception & {:as kv-data}]
  (μ/log ::exception
         :message message
         :exception-type (class exception)
         :exception-message (.getMessage exception)
         :stack-trace (map str (.getStackTrace exception))
         :timestamp (System/currentTimeMillis)
         :level :error
         kv-data))


(defn log-sync-operation
  "Log a synchronization operation with timing."
  [operation-name duration-ms & {:as kv-data}]
  (μ/trace ::sync-operation
           [:operation operation-name
            :duration-ms duration-ms
            :timestamp (System/currentTimeMillis)]
           kv-data))


(defn log-db-operation
  "Log a database operation."
  [operation-name table-name & {:as kv-data}]
  (μ/log ::db-operation
         :operation operation-name
         :table table-name
         :timestamp (System/currentTimeMillis)
         kv-data))


(comment
  ;; Example usage:
  (start-logging!)
  (set-global-context!)

  (log-info "Application started")
  (log-success "Sync completed" :files-processed 42)
  (log-warning "Deprecated function used" :function-name "old-api")
  (log-error "Failed to process file" :file-path "/tmp/test.txt" :error-code 500)

  ;; Using with context
  (μ/with-context {:request-id "req-123" :user-id "user-456"}
    (log-info "Processing request")
    (log-db-operation "insert" "skills" :records 1))

  ;; Using trace for timing
  (log-sync-operation "file-processing" 1250 :files 10)

  (stop-logging!))
