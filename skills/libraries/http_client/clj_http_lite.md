---
name: clj_http_lite
description: |
  Lightweight HTTP client for Clojure with minimal dependencies. Use when making HTTP 
  requests in resource-constrained environments, building command-line tools, or when 
  the full clj-http library is too heavy. Use when the user mentions HTTP client, REST 
  client, HTTP requests, lightweight HTTP, minimal dependencies, API calls, or web requests.
---

# clj-http-lite

A minimal HTTP client library for Clojure with no external dependencies beyond the JVM. Ideal for situations where you need a simple HTTP client without the overhead of Apache HttpClient.

## Quick Start

```clojure
;; Add dependency
{:deps {org.clj-commons/clj-http-lite {:mvn/version "1.0.13"}}}

;; Basic GET request
(require '[clj-http.lite.client :as http])

(http/get "https://api.example.com/users")
;; => {:status 200
;;     :headers {"content-type" "application/json" ...}
;;     :body "..."}

;; POST with form parameters
(http/post "https://api.example.com/users"
  {:form-params {:name "Alice" :email "alice@example.com"}})

;; JSON POST
(require '[cheshire.core :as json])

(http/post "https://api.example.com/users"
  {:body (json/generate-string {:name "Alice" :active true})
   :content-type :json})
```

**Key benefits:**
- Zero external dependencies (only requires JVM)
- Small footprint (perfect for CLIs and scripts)
- Simple, familiar API
- Built on java.net.HttpURLConnection
- Drop-in replacement for most clj-http use cases

## Core Concepts

### Request/Response Maps

All HTTP operations use Clojure maps for both requests and responses:

```clojure
;; Request map
{:method :get                    ; HTTP method
 :url "https://example.com"      ; Target URL
 :headers {"Accept" "text/html"} ; Request headers
 :body "request body"            ; Request body
 :query-params {:q "search"}     ; Query parameters
 :form-params {:name "Alice"}    ; Form parameters
 :basic-auth ["user" "pass"]     ; Basic authentication
 :throw-exceptions true}         ; Exception behavior

;; Response map
{:status 200                                    ; HTTP status code
 :headers {"content-type" "application/json"}  ; Response headers
 :body "..."}                                   ; Response body
```

### Automatic Conversions

The library automatically converts request options into appropriate formats:

```clojure
;; :url automatically parsed into components
{:url "https://joe:blow@example.com:443/path?q=clojure"}
;; Converts to:
{:scheme :https
 :server-name "example.com"
 :server-port 443
 :uri "/path"
 :query-string "q=clojure"
 :basic-auth ["joe" "blow"]}

;; :query-params converted to query string
{:query-params {:q "clojure" :page 1}}
;; Converts to: :query-string "q=clojure&page=1"

;; :form-params converted to URL-encoded body
{:form-params {:name "Alice" :age 30}}
;; Converts to: :body "name=Alice&age=30"
;;              :content-type "application/x-www-form-urlencoded"

;; :content-type shortcuts
{:content-type :json}
;; Converts to: "application/json; charset=UTF-8"
```

### Response Body Coercion

Control how response bodies are processed with the `:as` option:

```clojure
;; Auto-detect charset from response headers (default)
(http/get url {:as :auto})

;; Return as byte array
(http/get url {:as :byte-array})

;; Return as stream (for large responses)
(http/get url {:as :stream})

;; Decode with specific charset
(http/get url {:as "UTF-16"})

;; Default behavior (UTF-8 string)
(http/get url)
```

## Common Workflows

### Workflow 1: Basic HTTP Methods

```clojure
(require '[clj-http.lite.client :as http])

;; GET request
(http/get "https://api.example.com/users/123")
;; => {:status 200 :headers {...} :body "..."}

;; POST request
(http/post "https://api.example.com/users"
  {:body "user data"})

;; PUT request
(http/put "https://api.example.com/users/123"
  {:body "updated data"})

;; DELETE request
(http/delete "https://api.example.com/users/123")

;; HEAD request (get headers without body)
(http/head "https://api.example.com/users/123")
;; => {:status 200 :headers {...} :body nil}
```

### Workflow 2: Query Parameters

```clojure
;; Using :query-params (recommended)
(http/get "https://api.example.com/search"
  {:query-params {:q "clojure"
                  :page 2
                  :limit 10}})
;; Sends: GET /search?q=clojure&page=2&limit=10

;; Using keywords as keys
(http/get "https://api.example.com/search"
  {:query-params {:q "clojure" :filter [:active :new]}})

;; Parameters automatically URL-encoded
(http/get "https://api.example.com/search"
  {:query-params {:q "clojure & lisp"}})
;; Sends: GET /search?q=clojure%20%26%20lisp

;; Or include in URL directly
(http/get "https://api.example.com/search?q=clojure")
```

### Workflow 3: Request Headers

```clojure
;; Custom headers
(http/get "https://api.example.com/data"
  {:headers {"X-API-Key" "secret-key"
             "X-Request-ID" "12345"
             "User-Agent" "MyApp/1.0"}})

;; Content-Type shorthand
(http/post "https://api.example.com/data"
  {:body "{\"name\": \"Alice\"}"
   :content-type :json})
;; Sets: Content-Type: application/json; charset=UTF-8

;; Accept header shorthand
(http/get "https://api.example.com/data"
  {:accept :json})
;; Sets: Accept: application/json

;; Accept-Encoding
(http/get "https://api.example.com/data"
  {:accept-encoding [:gzip :deflate :identity]})
;; Sets: Accept-Encoding: gzip, deflate, identity
```

### Workflow 4: Form Data Submission

```clojure
;; URL-encoded form (POST only)
(http/post "https://example.com/login"
  {:form-params {:username "alice"
                 :password "secret"}})
;; Sends: Content-Type: application/x-www-form-urlencoded
;;        Body: username=alice&password=secret

;; Nested parameters
(http/post "https://example.com/form"
  {:form-params {:user {:name "Alice"
                        :email "alice@example.com"}
                 :tags ["clojure" "http"]}})

;; For GET requests, use :query-params instead
(http/get "https://example.com/search"
  {:query-params {:q "clojure"}})
```

### Workflow 5: JSON API Integration

```clojure
(require '[clj-http.lite.client :as http]
         '[cheshire.core :as json])

;; POST JSON data
(http/post "https://api.example.com/users"
  {:body (json/generate-string {:name "Alice"
                                  :email "alice@example.com"
                                  :active true})
   :content-type :json})

;; Parse JSON response
(defn get-user [id]
  (-> (http/get (str "https://api.example.com/users/" id)
                {:accept :json})
      :body
      (json/parse-string true)))

;; Complete JSON workflow
(defn create-user [user-data]
  (-> (http/post "https://api.example.com/users"
                 {:body (json/generate-string user-data)
                  :content-type :json
                  :accept :json})
      :body
      (json/parse-string true)))

(create-user {:name "Bob" :email "bob@example.com"})
;; => {:id 123 :name "Bob" :email "bob@example.com" :created-at "..."}
```

### Workflow 6: Authentication

```clojure
;; Basic authentication
(http/get "https://api.example.com/protected"
  {:basic-auth ["username" "password"]})
;; Sets: Authorization: Basic <base64-encoded-credentials>

;; OAuth bearer token
(http/get "https://api.example.com/protected"
  {:oauth-token "my-access-token"})
;; Sets: Authorization: Bearer my-access-token

;; API key in header
(http/get "https://api.example.com/data"
  {:headers {"X-API-Key" "secret-key"}})

;; Basic auth from URL (automatically extracted)
(http/get "https://user:pass@api.example.com/data")
;; Automatically converts to :basic-auth
```

### Workflow 7: Error Handling

```clojure
;; By default, throws on 4xx/5xx responses
(try
  (http/get "https://api.example.com/not-found")
  (catch Exception e
    (println "Error:" (.getMessage e))))

;; Disable exceptions to handle status manually
(let [response (http/get "https://api.example.com/resource"
                         {:throw-exceptions false})]
  (case (:status response)
    200 (process-success response)
    404 (println "Not found")
    500 (println "Server error")
    (println "Unexpected status:" (:status response))))

;; Check status before processing
(defn safe-get [url]
  (let [response (http/get url {:throw-exceptions false})]
    (when (< (:status response) 400)
      (:body response))))

;; Handle unknown host errors
(http/get "https://does-not-exist.invalid"
  {:ignore-unknown-host? true
   :throw-exceptions false})
;; Returns response map instead of throwing
```

### Workflow 8: Redirects and Timeouts

```clojure
;; Follow redirects (default behavior)
(http/get "https://example.com/redirect-me")
;; Automatically follows 3xx redirects

;; Disable redirect following
(http/get "https://example.com/redirect-me"
  {:follow-redirects false})
;; Returns 3xx response without following

;; Connection timeout (milliseconds)
(http/get "https://slow-server.com"
  {:conn-timeout 5000})  ; 5 second timeout to establish connection

;; Socket timeout (milliseconds)
(http/get "https://slow-server.com"
  {:socket-timeout 30000})  ; 30 second timeout waiting for data

;; Both timeouts
(http/get "https://slow-server.com"
  {:conn-timeout 5000
   :socket-timeout 30000})
```

### Workflow 9: Streaming Large Responses

```clojure
(require '[clojure.java.io :as io])

;; Stream response to file
(with-open [input (:body (http/get "https://example.com/large-file.zip"
                                   {:as :stream}))]
  (io/copy input (io/file "download.zip")))

;; Process streaming response
(with-open [stream (:body (http/get "https://api.example.com/data"
                                    {:as :stream}))]
  (doseq [line (line-seq (io/reader stream))]
    (process-line line)))

;; Get binary data as byte array
(let [bytes (:body (http/get "https://example.com/image.png"
                             {:as :byte-array}))]
  (save-bytes-to-file bytes "image.png"))
```

### Workflow 10: Advanced Request Options

```clojure
;; Save original request in response
(let [response (http/get "https://api.example.com/data"
                         {:save-request? true
                          :query-params {:page 1}})]
  (println "Original request:" (:request response)))

;; Chunked streaming for large uploads
(http/post "https://api.example.com/upload"
  {:body large-input-stream
   :chunk-size 8192})  ; 8KB chunks

;; Insecure connections (skip SSL verification)
(http/get "https://self-signed.example.com"
  {:insecure? true})
;; WARNING: Only use for development/testing

;; Custom body encoding
(http/post "https://api.example.com/data"
  {:body "データ"
   :body-encoding "UTF-16"})
```

## When to Use clj-http-lite

**Use clj-http-lite when:**
- Building command-line tools or scripts
- Need minimal dependencies (JAR size matters)
- Working in resource-constrained environments
- Simple HTTP client needs (GET, POST, basic auth)
- Want fast startup times (no Apache HttpClient initialization)
- Using Babashka or GraalVM native image

**Use clj-http (full version) when:**
- Need connection pooling
- Require multipart file uploads
- Need cookie management
- Want automatic JSON/EDN parsing
- Require HTTP/2 support
- Need proxy configuration
- Working with complex authentication flows

**Use http-kit client when:**
- Need async HTTP requests
- Building high-performance applications
- Want WebSocket support
- Need better connection pooling

## Best Practices

**DO:**
- Use `:as :stream` for large downloads
- Set appropriate timeouts (`:conn-timeout`, `:socket-timeout`)
- Use `:throw-exceptions false` when status codes need manual handling
- Leverage `:query-params` for cleaner code
- Use content-type shortcuts (`:json`, `:xml`) for common types
- Parse response bodies outside the HTTP call (separation of concerns)
- Close streams when using `:as :stream`

**DON'T:**
- Make synchronous requests in async contexts (use http-kit instead)
- Skip error handling (wrap in try-catch or use `:throw-exceptions false`)
- Forget to set timeouts (requests can hang indefinitely)
- Use `:insecure? true` in production
- Parse response body inside HTTP call (keep concerns separate)
- Ignore response status codes
- Send sensitive data without HTTPS

## Common Issues

### Issue: "Connection Refused"

**Problem:** Cannot connect to server

```clojure
(http/get "http://localhost:8080")
;; Exception: Connection refused
```

**Solution:** Verify server is running and port is correct

```clojure
;; Add timeout to fail faster
(http/get "http://localhost:8080"
  {:conn-timeout 2000
   :throw-exceptions false})
;; Check :status in response
```

### Issue: "SocketTimeoutException"

**Problem:** Server too slow to respond

```clojure
(http/get "https://slow-api.com")
;; Exception: Read timed out
```

**Solution:** Increase socket timeout

```clojure
(http/get "https://slow-api.com"
  {:socket-timeout 60000})  ; 60 seconds
```

### Issue: 404/500 Exceptions

**Problem:** HTTP errors throw exceptions by default

```clojure
(http/get "https://api.example.com/not-found")
;; Exception: clj-http.lite: status 404
```

**Solution:** Handle errors explicitly

```clojure
;; Option 1: Catch exceptions
(try
  (http/get url)
  (catch Exception e
    {:error (.getMessage e)}))

;; Option 2: Disable exceptions
(let [resp (http/get url {:throw-exceptions false})]
  (if (< (:status resp) 400)
    (:body resp)
    (handle-error resp)))
```

### Issue: Response Body is Nil

**Problem:** HEAD requests or 204 responses have no body

```clojure
(http/head "https://api.example.com/users/123")
;; => {:status 200 :body nil ...}
```

**Solution:** This is expected behavior. Use GET if you need the body.

```clojure
;; HEAD is for checking existence/metadata
(let [resp (http/head url {:throw-exceptions false})]
  (when (= 200 (:status resp))
    (http/get url)))  ; Now fetch full body
```

### Issue: Query Parameters Not Encoded

**Problem:** Special characters in query params not handled

```clojure
;; Wrong - manual URL encoding needed
(http/get "https://api.example.com?q=a b&c")
;; => 400 Bad Request

;; Right - use :query-params
(http/get "https://api.example.com"
  {:query-params {:q "a b&c"}})
;; Automatically URL-encoded
```

### Issue: SSL Certificate Errors

**Problem:** Self-signed or invalid certificates

```clojure
(http/get "https://self-signed.example.com")
;; Exception: PKIX path building failed
```

**Solution:** Use `:insecure?` option (development only)

```clojure
;; Development/testing only
(http/get "https://self-signed.example.com"
  {:insecure? true})

;; Production: Fix certificates on server
```

### Issue: Large Response Memory Issues

**Problem:** Loading large response into memory causes OutOfMemoryError

```clojure
;; Bad - loads entire 1GB response into memory
(http/get "https://example.com/huge-file.zip")
```

**Solution:** Use streaming with `:as :stream`

```clojure
;; Good - streams response
(with-open [stream (:body (http/get url {:as :stream}))]
  (io/copy stream (io/file "output.zip")))
```

## Comparison with Other HTTP Clients

| Feature | clj-http-lite | clj-http | http-kit client |
|---------|---------------|----------|-----------------|
| Dependencies | None (JVM only) | Apache HttpClient | Async/NIO |
| JAR Size | ~20KB | ~5MB+ | ~300KB |
| Connection Pooling | No | Yes | Yes |
| Async Requests | No | No | Yes |
| Multipart Upload | No | Yes | Yes |
| Cookie Management | No | Yes | Limited |
| Startup Time | Fast | Slow | Medium |
| Use Case | CLIs, Scripts | Full-featured client | High-performance apps |

## Advanced Topics

### Custom Request Function

The `request` function is the foundation of all HTTP methods:

```clojure
;; All methods delegate to request
(http/request
  {:method :get
   :url "https://api.example.com/users"
   :headers {"Accept" "application/json"}
   :query-params {:page 1}
   :throw-exceptions false})

;; This is equivalent to:
(http/get "https://api.example.com/users"
  {:headers {"Accept" "application/json"}
   :query-params {:page 1}
   :throw-exceptions false})
```

### Middleware System

clj-http-lite uses middleware wrappers (similar to Ring):

```clojure
;; Available middleware functions:
;; - wrap-url: Parses :url into components
;; - wrap-method: Handles :method
;; - wrap-query-params: Converts :query-params to query string
;; - wrap-basic-auth: Handles :basic-auth
;; - wrap-oauth: Handles :oauth-token
;; - wrap-accept: Handles :accept
;; - wrap-accept-encoding: Handles :accept-encoding
;; - wrap-content-type: Handles :content-type
;; - wrap-form-params: Handles :form-params
;; - wrap-nested-params: Handles nested parameter maps
;; - wrap-redirects: Follows redirects
;; - wrap-decompression: Decompresses gzip/deflate
;; - wrap-exceptions: Throws on error status codes
;; - wrap-unknown-host: Handles unknown host errors

;; These are automatically applied by the client
```

## Resources

- [GitHub Repository](https://github.com/clj-commons/clj-http-lite)
- [API Documentation](https://cljdoc.org/d/org.clj-commons/clj-http-lite)
- [clj-http (full version)](https://github.com/dakrone/clj-http)
- [http-kit](https://github.com/http-kit/http-kit)

## Summary

clj-http-lite is a minimal, zero-dependency HTTP client for Clojure:

1. **Lightweight** - No external dependencies beyond JVM
2. **Simple API** - Request/response maps like clj-http
3. **Automatic conversions** - Query params, form params, auth
4. **Flexible responses** - String, byte array, or stream
5. **Good for CLIs** - Fast startup, small footprint
6. **Drop-in subset** - Compatible with most clj-http code

Use clj-http-lite when you need a simple HTTP client without the overhead of Apache HttpClient. It's perfect for command-line tools, scripts, and resource-constrained environments where minimal dependencies and fast startup matter more than advanced features like connection pooling or async requests.
