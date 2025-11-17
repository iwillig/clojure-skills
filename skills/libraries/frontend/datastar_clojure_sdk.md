---
name: datastar-clojure-sdk
description: |
  Server-side SDK for building Datastar applications in Clojure with SSE streaming,
  element patching, and signal management. Use when implementing Datastar backends,
  sending server-sent events, streaming HTML updates, managing SSE connections,
  implementing real-time features, or when the user mentions Datastar SDK, SSE generators,
  patch-elements, write profiles, http-kit SSE, ring SSE, or backend Datastar integration.
---

# Datastar Clojure SDK

Server-side SDK for building Datastar applications with Server-Sent Events (SSE), HTML streaming, and reactive signal management in Clojure.

## Quick Start

```clojure
;; Add dependencies to deps.edn
{:deps {dev.data-star.clojure/sdk {:mvn/version "1.0.0-RC4"}
        dev.data-star.clojure/http-kit {:mvn/version "1.0.0-RC4"}}}

;; Require the API and adapter
(require '[starfederation.datastar.clojure.api :as d*]
         '[starfederation.datastar.clojure.adapter.http-kit :as hk]
         '[org.httpkit.server :as http-kit])

;; Simple SSE handler
(defn sse-handler [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       ;; Send HTML update
       (d*/patch-elements! sse-gen "<div id='message'>Hello from server!</div>")
       ;; Close connection
       (d*/close-sse! sse-gen))}))

;; Start server
(http-kit/run-server sse-handler {:port 3000})
```

**Key benefits:**
- Multiple adapter implementations (http-kit, ring)
- Built-in compression support (gzip, brotli)
- Flexible buffering strategies via write profiles
- Protocol-based extensibility
- Automatic SSE event formatting
- Connection lifecycle management
- Thread-safe event sending

## Core Concepts

### SSE Generators

SSE generators are the core abstraction - they represent an open SSE connection and provide methods to send events:

```clojure
;; SSE generators implement the SSEGenerator protocol
(defprotocol SSEGenerator
  (send-event! [this event-type data-lines opts])
  (close! [this])
  (closed? [this]))

;; Created by adapter's ->sse-response function
(hk/->sse-response request
  {hk/on-open
   (fn [sse-gen]
     ;; sse-gen is the SSEGenerator
     (d*/patch-elements! sse-gen "<div>...</div>"))})
```

### Adapters

Adapters bridge the SSE abstraction with specific web servers:

```clojure
;; http-kit adapter (async channels)
(require '[starfederation.datastar.clojure.adapter.http-kit :as hk])

(hk/->sse-response request {...})

;; Ring adapter (StreamableResponseBody)
(require '[starfederation.datastar.clojure.adapter.ring :as ring])

(ring/->sse-response request {...})
```

**Adapter differences:**
- http-kit: Uses AsyncChannel, detects disconnects automatically
- ring: Uses StreamableResponseBody, works with ring-compliant servers

### Write Profiles

Write profiles control buffering and compression:

```clojure
(require '[starfederation.datastar.clojure.adapter.common :as ac])

;; Built-in profiles
hk/basic-profile                    ; Temp buffers, no compression
hk/buffered-writer-profile          ; Permanent BufferedWriter
hk/gzip-profile                     ; Temp buffers + gzip
hk/gzip-buffered-writer-profile     ; BufferedWriter + gzip

;; Use a profile
(hk/->sse-response request
  {hk/write-profile hk/gzip-profile
   hk/on-open (fn [sse] ...)})

;; Custom profile
(def my-profile
  {ac/wrap-output-stream (fn [os] (-> os ac/->gzip-os ac/->os-writer))
   ac/write! (ac/->write-with-temp-buffer!)
   ac/content-encoding ac/gzip-content-encoding})
```

### Lifecycle Callbacks

Control connection lifecycle with callbacks:

```clojure
(hk/->sse-response request
  {;; Called when connection is ready
   hk/on-open
   (fn [sse-gen]
     (println "Connection opened")
     (d*/patch-elements! sse-gen "<div>Connected</div>"))
   
   ;; Called when connection closes
   hk/on-close
   (fn [sse-gen status]
     (println "Connection closed with status:" status))
   
   ;; Called when send error occurs
   hk/on-exception
   (fn [sse-gen e ctx]
     (println "Error sending:" (.getMessage e))
     ;; Return true to close connection
     false)})
```

## Common Workflows

### Workflow 1: Simple Request-Response SSE

Single update then close:

```clojure
(require '[starfederation.datastar.clojure.api :as d*]
         '[starfederation.datastar.clojure.adapter.http-kit :as hk])

(defn fetch-data-handler [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       ;; Send data
       (d*/patch-elements! sse-gen
         "<div id='data'>
            <h2>User Data</h2>
            <p>Name: Alice</p>
            <p>Email: alice@example.com</p>
          </div>")
       
       ;; Close immediately
       (d*/close-sse! sse-gen))}))

;; Or use with-open-sse macro
(defn fetch-data-handler-v2 [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       (d*/with-open-sse sse-gen
         (d*/patch-elements! sse-gen "<div id='data'>...</div>")))}))
```

### Workflow 2: Multi-Step Progress Updates

Stream multiple updates:

```clojure
(defn process-task-handler [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       (d*/with-open-sse sse-gen
         ;; Step 1: Initialize
         (d*/patch-elements! sse-gen
           "<div id='progress'>0%</div>
            <div id='status'>Starting...</div>")
         
         (Thread/sleep 1000)
         
         ;; Step 2: Processing
         (d*/patch-elements! sse-gen
           "<div id='progress'>33%</div>
            <div id='status'>Processing data...</div>")
         
         (Thread/sleep 1000)
         
         ;; Step 3: Finalizing
         (d*/patch-elements! sse-gen
           "<div id='progress'>66%</div>
            <div id='status'>Finalizing...</div>")
         
         (Thread/sleep 1000)
         
         ;; Step 4: Complete
         (d*/patch-elements! sse-gen
           "<div id='progress'>100%</div>
            <div id='status' style='color: green;'>Complete!</div>")))}))
```

### Workflow 3: Long-Lived Connections (Broadcasting)

Keep connections open and broadcast to multiple clients:

```clojure
(def connections (atom #{}))

(defn events-handler [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       ;; Add to connection pool
       (swap! connections conj sse-gen)
       
       ;; Send initial message
       (d*/patch-elements! sse-gen
         "<div id='status'>Connected to events stream</div>"))
     
     hk/on-close
     (fn [sse-gen status]
       ;; Remove from pool when closed
       (swap! connections disj sse-gen))}))

;; Broadcast to all connected clients
(defn broadcast-event! [html]
  (doseq [conn @connections]
    (when-not (d*/closed? conn)
      (d*/patch-elements! conn html))))

;; Trigger broadcast from anywhere
(defn user-updated [user-id]
  (broadcast-event!
    (str "<div id='user-" user-id "'>
            <span>User " user-id " updated</span>
          </div>")))
```

### Workflow 4: Patching Multiple Elements

Send multiple element updates in one event:

```clojure
(defn update-dashboard-handler [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       (d*/with-open-sse sse-gen
         ;; Multiple elements in one patch
         (d*/patch-elements! sse-gen
           "<div id='user-count'>42 users online</div>
            <div id='message-count'>127 new messages</div>
            <div id='alert'>System status: OK</div>")))}))

;; Or use patch-elements-seq! for sequences
(defn update-items-handler [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       (let [items (fetch-items)
             html-items (map #(str "<div id='item-" (:id %) "'>
                                      " (:name %) "
                                    </div>")
                             items)]
         (d*/with-open-sse sse-gen
           (d*/patch-elements-seq! sse-gen html-items))))}))
```

### Workflow 5: Signal Management

Update frontend reactive state from backend:

```clojure
(require '[cheshire.core :as json])

(defn update-counter-handler [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       (d*/with-open-sse sse-gen
         ;; Update signals (frontend state)
         (d*/patch-signals! sse-gen
           (json/generate-string
             {:count 42
              :status "updated"
              :timestamp (System/currentTimeMillis)}))
         
         ;; Signals can also be sent as JavaScript object literal
         (d*/patch-signals! sse-gen
           "{count: 42, status: 'updated'}")))}))

;; Only update if signal doesn't exist
(defn init-signals-handler [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       (d*/with-open-sse sse-gen
         (d*/patch-signals! sse-gen
           "{count: 0, status: 'initialized'}"
           {d*/only-if-missing true})))}))
```

### Workflow 6: Executing Scripts

Send JavaScript to execute in browser:

```clojure
(defn notify-handler [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       (d*/with-open-sse sse-gen
         ;; Execute JavaScript
         (d*/execute-script! sse-gen
           "alert('Task completed successfully!');")
         
         ;; Console logging helpers
         (d*/console-log! sse-gen "Processing complete")
         (d*/console-error! sse-gen "Error occurred in processing")))}))

;; Redirect to another page
(defn redirect-handler [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       (d*/with-open-sse sse-gen
         (Thread/sleep 2000)  ; Show loading for 2s
         (d*/redirect! sse-gen "/dashboard")))}))
```

### Workflow 7: Advanced Patching Options

Use patch modes, selectors, and view transitions:

```clojure
(defn advanced-patch-handler [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       (d*/with-open-sse sse-gen
         ;; Morph mode (default) - smart DOM merge
         (d*/patch-elements! sse-gen
           "<div id='content'>New content</div>")
         
         ;; Append to body
         (d*/patch-elements! sse-gen
           "<div class='notification'>New notification</div>"
           {d*/patch-mode d*/pm-append
            d*/selector "body"})
         
         ;; Prepend to list
         (d*/patch-elements! sse-gen
           "<li>New item at top</li>"
           {d*/patch-mode d*/pm-prepend
            d*/selector "#item-list"})
         
         ;; Replace outer HTML
         (d*/patch-elements! sse-gen
           "<section id='main'>Completely replaced</section>"
           {d*/patch-mode d*/pm-outer})
         
         ;; Inner HTML only
         (d*/patch-elements! sse-gen
           "<p>Just the inner content</p>"
           {d*/patch-mode d*/pm-inner
            d*/selector "#container"})
         
         ;; Remove element
         (d*/remove-element! sse-gen "#old-banner")
         
         ;; Use view transitions (smooth animations)
         (d*/patch-elements! sse-gen
           "<div id='content'>Animated update</div>"
           {d*/use-view-transition true})))}))

;; Available patch modes:
;; d*/pm-outer    - Replace outer HTML (default morph)
;; d*/pm-inner    - Replace inner HTML only
;; d*/pm-prepend  - Prepend to element
;; d*/pm-append   - Append to element
;; d*/pm-before   - Insert before element
;; d*/pm-after    - Insert after element
;; d*/pm-replace  - Replace without morphing
;; d*/pm-remove   - Remove element
```

### Workflow 8: Using Compression

Enable gzip compression for large payloads:

```clojure
(require '[starfederation.datastar.clojure.adapter.http-kit :as hk])

(defn large-data-handler [request]
  (hk/->sse-response request
    {;; Use gzip compression
     hk/write-profile hk/gzip-profile
     
     hk/on-open
     (fn [sse-gen]
       (d*/with-open-sse sse-gen
         ;; Send large HTML payload
         (let [large-html (generate-large-table)]
           (d*/patch-elements! sse-gen large-html))))}))

;; Custom buffer sizes
(require '[starfederation.datastar.clojure.adapter.common :as ac])

(def my-gzip-profile
  {ac/wrap-output-stream
   (fn [os] (-> os
                (ac/->gzip-os 2048)  ; 2KB gzip buffer
                ac/->os-writer))
   
   ac/write! (ac/->write-with-temp-buffer! 16384)  ; 16KB temp buffer
   ac/content-encoding ac/gzip-content-encoding})

(defn optimized-handler [request]
  (hk/->sse-response request
    {hk/write-profile my-gzip-profile
     hk/on-open (fn [sse] ...)}))
```

### Workflow 9: Extracting Datastar Signals from Request

Parse frontend signals from incoming requests:

```clojure
(require '[cheshire.core :as json])

(defn submit-form-handler [request]
  ;; Check if request is from Datastar
  (if (d*/datastar-request? request)
    (let [;; Extract signals (returns string or InputStream)
          signals-data (d*/get-signals request)
          
          ;; Parse JSON to Clojure data
          data (if (string? signals-data)
                 (json/parse-string signals-data true)
                 (json/parse-stream (io/reader signals-data) true))
          
          {:keys [name email message]} data]
      
      ;; Process the data
      (save-contact! name email message)
      
      ;; Send response
      (hk/->sse-response request
        {hk/on-open
         (fn [sse-gen]
           (d*/with-open-sse sse-gen
             (d*/patch-elements! sse-gen
               (str "<div id='result' style='color: green;'>
                       Thanks " name "! We received your message.
                     </div>"))))}))
    
    {:status 400 :body "Not a Datastar request"}))
```

### Workflow 10: Thread-Safe Broadcasting

Use locking for concurrent event sending:

```clojure
(def connections (atom #{}))

(defn broadcast-safe! [html]
  (doseq [conn @connections]
    (when-not (d*/closed? conn)
      ;; Lock SSE generator while sending
      (d*/lock-sse! conn
        (d*/patch-elements! conn html)
        (d*/patch-signals! conn "{lastUpdate: Date.now()}")))))

;; Or lock multiple operations together
(defn update-client [sse-gen user-data]
  (d*/lock-sse! sse-gen
    ;; These send atomically
    (d*/patch-elements! sse-gen
      (render-user-html user-data))
    (d*/patch-signals! sse-gen
      (json/generate-string {:user user-data}))
    (d*/console-log! sse-gen "User data updated")))
```

### Workflow 11: Error Handling

Handle exceptions during SSE sending:

```clojure
(defn robust-handler [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       (try
         (d*/with-open-sse sse-gen
           ;; Risky operation
           (let [data (fetch-external-data)]
             (d*/patch-elements! sse-gen (render-data data))))
         (catch Exception e
           (println "Error in SSE handler:" (.getMessage e))
           ;; Send error to client if connection still open
           (when-not (d*/closed? sse-gen)
             (d*/patch-elements! sse-gen
               "<div id='error' style='color: red;'>
                  Error loading data. Please try again.
                </div>")))))
     
     ;; Custom exception handler for send errors
     hk/on-exception
     (fn [sse-gen e ctx]
       (println "Send error:" (.getMessage e))
       (println "Event type:" (:event-type ctx))
       ;; Return true to close connection
       (instance? java.io.IOException e))}))
```

### Workflow 12: SSE with Ring Adapter

Using ring-compliant servers:

```clojure
(require '[starfederation.datastar.clojure.adapter.ring :as ring])

;; Works with Jetty, Immutant, etc.
(defn ring-sse-handler [request]
  (ring/->sse-response request
    {ring/on-open
     (fn [sse-gen]
       (d*/with-open-sse sse-gen
         (d*/patch-elements! sse-gen
           "<div id='message'>Hello from Ring!</div>")))}))

;; With compression
(defn ring-compressed-handler [request]
  (ring/->sse-response request
    {ring/write-profile ring/gzip-profile
     ring/on-open
     (fn [sse-gen]
       (d*/with-open-sse sse-gen
         (d*/patch-elements! sse-gen large-html)))}))
```

## When to Use Each Approach

**Use basic-profile when:**
- Small payloads
- Local development
- Minimal latency required
- No compression overhead needed

**Use gzip-profile when:**
- Large HTML payloads
- Production deployments
- Network bandwidth limited
- Payload size > 1KB

**Use buffered-writer-profile when:**
- Very frequent small updates
- Want to reuse buffers
- Memory allocation is concern

**Use http-kit adapter when:**
- Using http-kit server
- Need automatic disconnect detection
- Building async applications
- Want minimal dependencies

**Use ring adapter when:**
- Using Jetty, Immutant, etc.
- Need ring middleware compatibility
- Building standard ring apps
- Want maximum portability

## Best Practices

**Do:**
- Always close SSE connections when done
- Use `with-open-sse` macro for automatic cleanup
- Include element IDs in patched HTML
- Use compression for payloads > 1KB
- Handle exceptions in on-open callback
- Remove closed connections from broadcast pools
- Use `lock-sse!` for concurrent sends to same generator
- Check `datastar-request?` before extracting signals
- Return proper HTTP status codes

```clojure
;; Good: Auto-cleanup with macro
(d*/with-open-sse sse-gen
  (d*/patch-elements! sse-gen html))

;; Good: Check before extracting
(when (d*/datastar-request? request)
  (d*/get-signals request))

;; Good: Remove dead connections
(swap! connections #(set (filter (comp not d*/closed?) %)))
```

**Don't:**
- Forget to close connections
- Send to closed generators
- Block indefinitely in on-open (ties up threads)
- Ignore IOExceptions (connection broken)
- Mix adapters (use one per handler)
- Share SSE generators across threads without locking
- Send malformed HTML (must be valid)

```clojure
;; Bad: No cleanup
(hk/->sse-response request
  {hk/on-open
   (fn [sse] (d*/patch-elements! sse html))})  ; Never closes!

;; Good: Explicit close or macro
(hk/->sse-response request
  {hk/on-open
   (fn [sse]
     (d*/with-open-sse sse
       (d*/patch-elements! sse html)))})

;; Bad: Blocking forever
(hk/on-open
 (fn [sse]
   (loop []
     (Thread/sleep 1000)
     (d*/patch-elements! sse (get-data))
     (recur))))  ; Ties up thread!
```

## Common Issues

### Issue: Events Not Reaching Client

**Problem:** Sending events but client not receiving them.

```clojure
(hk/->sse-response request
  {hk/on-open
   (fn [sse-gen]
     (d*/patch-elements! sse-gen html)
     ;; Forgot to close!
   )})
```

**Solution:** Always close connection or keep it open intentionally:

```clojure
;; Option 1: Close explicitly
(hk/->sse-response request
  {hk/on-open
   (fn [sse-gen]
     (d*/patch-elements! sse-gen html)
     (d*/close-sse! sse-gen))})

;; Option 2: Use macro
(hk/->sse-response request
  {hk/on-open
   (fn [sse-gen]
     (d*/with-open-sse sse-gen
       (d*/patch-elements! sse-gen html)))})
```

### Issue: Connection Pool Memory Leak

**Problem:** Closed connections accumulate in pool.

```clojure
(def connections (atom #{}))

(hk/on-open
 (fn [sse] (swap! connections conj sse)))

;; Never removed when closed!
```

**Solution:** Remove on close callback:

```clojure
(hk/->sse-response request
  {hk/on-open
   (fn [sse] (swap! connections conj sse))
   
   hk/on-close
   (fn [sse status]
     (swap! connections disj sse))})

;; Or periodic cleanup
(defn cleanup-dead-connections! []
  (swap! connections
         #(set (filter (comp not d*/closed?) %))))
```

### Issue: Elements Not Updating in Browser

**Problem:** Sending HTML but DOM not updating.

```clojure
;; Missing ID on element
(d*/patch-elements! sse-gen
  "<div>New content</div>")
```

**Solution:** Include ID matching frontend element:

```clojure
;; Frontend has: <div id="message">...</div>
(d*/patch-elements! sse-gen
  "<div id='message'>New content</div>")
```

### Issue: Compression Not Working

**Problem:** Large payloads still slow despite gzip profile.

```clojure
(hk/->sse-response request
  {hk/write-profile hk/gzip-profile
   ;; Forgot Content-Encoding header
   hk/on-open (fn [sse] ...)})
```

**Solution:** Write profiles include headers automatically, but ensure browser supports:

```clojure
;; Profiles set Content-Encoding automatically
;; Check browser receives: Content-Encoding: gzip

;; If using custom profile, include encoding:
(def my-profile
  {ac/wrap-output-stream (fn [os] ...)
   ac/write! (fn [writer data] ...)
   ac/content-encoding ac/gzip-content-encoding})  ; Important!
```

### Issue: Signal Extraction Fails

**Problem:** Cannot parse signals from request.

```clojure
(let [signals (d*/get-signals request)
      data (json/parse-string signals true)]  ; Error if InputStream!
  ...)
```

**Solution:** Handle both string and InputStream:

```clojure
(let [signals-data (d*/get-signals request)
      data (if (string? signals-data)
             (json/parse-string signals-data true)
             (json/parse-stream (io/reader signals-data) true))]
  ...)
```

### Issue: Race Conditions in Broadcasting

**Problem:** Concurrent sends to same generator cause issues.

```clojure
;; Thread 1 and 2 both send to same sse-gen
(future (d*/patch-elements! sse-gen html1))
(future (d*/patch-elements! sse-gen html2))
```

**Solution:** Use locking:

```clojure
(d*/lock-sse! sse-gen
  (d*/patch-elements! sse-gen html1)
  (d*/patch-signals! sse-gen signals1))
```

## Advanced Topics

### Custom Write Profiles

Create custom buffering strategies:

```clojure
(require '[starfederation.datastar.clojure.adapter.common :as ac])

(def custom-profile
  {;; Wrap output stream with custom compression
   ac/wrap-output-stream
   (fn [os]
     (-> os
         (my-compression-stream)
         (java.io.OutputStreamWriter. "UTF-8")))
   
   ;; Custom write function
   ac/write!
   (fn [writer data]
     (doto writer
       (.write data)
       (.flush)))
   
   ;; Custom encoding header
   ac/content-encoding "my-encoding"})
```

### SSE Event Options

Control SSE event behavior:

```clojure
(d*/patch-elements! sse-gen html
  {;; Event ID for replay
   d*/id "event-123"
   
   ;; Retry duration (milliseconds)
   d*/retry-duration 5000
   
   ;; Patch options
   d*/selector "#target"
   d*/patch-mode d*/pm-append})
```

### Generating Datastar Actions

Helper functions for generating frontend actions:

```clojure
;; Generate @get(...) action strings
(d*/sse-get "/api/data")
;; => "@get('/api/data')"

(d*/sse-get "/api/data" "useViewTransition:true")
;; => "@get('/api/data', useViewTransition:true)"

;; Other HTTP methods
(d*/sse-post "/api/submit")
(d*/sse-put "/api/update")
(d*/sse-patch "/api/partial")
(d*/sse-delete "/api/remove")

;; Use in generated HTML
(html [:button {:data-on:click (d*/sse-get "/load-data")} "Load"])
```

### CDN URLs

Access latest Datastar frontend library:

```clojure
;; Latest version URL
d*/CDN-url
;; => "https://cdn.jsdelivr.net/gh/starfederation/datastar@1.0.0-RC6/bundles/datastar.js"

;; Source map URL
d*/CDN-map-url
;; => "https://cdn.jsdelivr.net/gh/starfederation/datastar@1.0.0-RC6/bundles/datastar.js.map"

;; Use in HTML generation
(html5
  [:head
   [:script {:type "module" :src d*/CDN-url}]]
  [:body ...])
```

## Integration Examples

### With Reitit

```clojure
(require '[reitit.ring :as ring]
         '[starfederation.datastar.clojure.api :as d*]
         '[starfederation.datastar.clojure.adapter.http-kit :as hk])

(def app
  (ring/ring-handler
    (ring/router
      [["/api"
        ["/stream" {:get
                    (fn [request]
                      (hk/->sse-response request
                        {hk/on-open
                         (fn [sse]
                           (d*/with-open-sse sse
                             (d*/patch-elements! sse
                               "<div id='data'>Streamed!</div>")))}))}]]])))
```

### With Integrant

```clojure
(require '[integrant.core :as ig])

(defmethod ig/init-key ::sse-connections [_ _]
  (atom #{}))

(defmethod ig/init-key ::sse-handler [_ {:keys [connections]}]
  (fn [request]
    (hk/->sse-response request
      {hk/on-open
       (fn [sse] (swap! connections conj sse))
       
       hk/on-close
       (fn [sse _] (swap! connections disj sse))})))
```

### With Component

```clojure
(require '[com.stuartsierra.component :as component])

(defrecord SSEBroadcaster [connections]
  component/Lifecycle
  (start [this]
    (assoc this :connections (atom #{})))
  
  (stop [this]
    (doseq [conn @connections]
      (d*/close-sse! conn))
    (assoc this :connections nil)))

(defn make-broadcaster []
  (map->SSEBroadcaster {}))
```

## Resources

- Official Datastar Site: https://data-star.dev
- SDK GitHub: https://github.com/starfederation/datastar-clojure
- API Documentation: https://cljdoc.org/d/dev.data-star.clojure/sdk
- SSE Specification: https://html.spec.whatwg.org/multipage/server-sent-events.html
- Write Profiles Guide: https://github.com/starfederation/datastar-clojure/blob/main/doc/Write-profiles.md

## Summary

The Datastar Clojure SDK provides server-side tools for building reactive web applications:

1. **SSE Generators** - Protocol-based abstraction for streaming events
2. **Multiple Adapters** - http-kit and ring implementations
3. **Write Profiles** - Control buffering and compression
4. **Element Patching** - Send HTML updates via `patch-elements!`
5. **Signal Management** - Update frontend state via `patch-signals!`
6. **Lifecycle Control** - on-open, on-close, on-exception callbacks
7. **Thread Safety** - Built-in locking with `lock-sse!`
8. **Compression** - Gzip and brotli support

**Core pattern:**

```clojure
;; 1. Add dependencies
{:deps {dev.data-star.clojure/sdk {:mvn/version "1.0.0-RC4"}
        dev.data-star.clojure/http-kit {:mvn/version "1.0.0-RC4"}}}

;; 2. Require API and adapter
(require '[starfederation.datastar.clojure.api :as d*]
         '[starfederation.datastar.clojure.adapter.http-kit :as hk])

;; 3. Create SSE handler
(defn handler [request]
  (hk/->sse-response request
    {hk/on-open
     (fn [sse-gen]
       (d*/with-open-sse sse-gen
         (d*/patch-elements! sse-gen html)))}))

;; 4. Use with http-kit server
(http-kit/run-server handler {:port 3000})
```

Perfect for building real-time, server-driven UIs with minimal frontend complexity.
