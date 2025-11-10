---
name: ring-http-abstraction
description: |
  Build web applications using Ring, the foundational HTTP abstraction for Clojure.
  Use when creating HTTP handlers, building REST APIs, working with HTTP requests/responses,
  composing middleware, serving static files, handling sessions/cookies, or when the user
  mentions Ring, web app, HTTP server, handlers, middleware, or web development.
---

# Ring HTTP Abstraction

## Quick Start

Ring is the foundational HTTP abstraction layer for Clojure web applications. It defines a simple, composable interface for handling HTTP requests through handlers and middleware.

```clojure
(require '[ring.adapter.jetty :as jetty]
         '[ring.util.response :as response])

;; Define a simple handler (request -> response)
(defn hello-handler [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello, World!"})

;; Start server
(def server (jetty/run-jetty hello-handler {:port 8080 :join? false}))
;; Server running at http://localhost:8080

;; Stop server
(.stop server)
```

**Key concepts:**
- **Handlers** - Functions that take a request map and return a response map
- **Middleware** - Higher-order functions that wrap handlers to add functionality
- **Request maps** - Clojure maps representing HTTP requests
- **Response maps** - Clojure maps representing HTTP responses
- **Adapters** - Connect Ring handlers to HTTP servers (Jetty, http-kit, etc.)

## Core Concepts

### Request Maps

Every HTTP request is represented as a Clojure map with these keys:

```clojure
{:uri "/users/123"              ; Request path
 :request-method :get           ; HTTP method (:get, :post, :put, :delete, etc.)
 :scheme :http                  ; :http or :https
 :server-name "localhost"       ; Server hostname
 :server-port 8080              ; Server port
 :remote-addr "127.0.0.1"       ; Client IP
 :headers {"content-type" "application/json"  ; HTTP headers (lowercase keys)
           "user-agent" "..."}
 :query-string "page=1&limit=10"  ; Query parameters (as string)
 :body <InputStream>            ; Request body (InputStream)
 :protocol "HTTP/1.1"}          ; HTTP protocol version
```

### Response Maps

Handlers return response maps with these keys:

```clojure
{:status 200                    ; HTTP status code (required)
 :headers {"Content-Type" "text/html"}  ; Response headers (required, can be empty)
 :body "Response content"}      ; Response body (optional)
```

The `:body` can be:
- String - Text content
- `InputStream` - Binary data stream
- `File` - File to serve
- `ISeq` - Sequence of strings/byte arrays (for streaming)
- `nil` - Empty response

### Handlers

Handlers are functions that transform requests into responses:

```clojure
;; Simple handler
(defn my-handler [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello!"})

;; Handler using request data
(defn echo-uri [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (str "You requested: " (:uri request))})

;; Conditional responses
(defn conditional-handler [request]
  (case (:uri request)
    "/" {:status 200 :headers {} :body "Home"}
    "/about" {:status 200 :headers {} :body "About"}
    {:status 404 :headers {} :body "Not Found"}))
```

### Middleware

Middleware wraps handlers to add cross-cutting functionality:

```clojure
;; Middleware is a function that takes a handler and returns a new handler
(defn wrap-logging [handler]
  (fn [request]
    (println "Request:" (:uri request))
    (let [response (handler request)]
      (println "Response:" (:status response))
      response)))

;; Using middleware
(def app (wrap-logging my-handler))

;; Composing multiple middleware with threading
(def app
  (-> my-handler
      wrap-logging
      wrap-params
      wrap-keyword-params))
```

## Common Workflows

### Workflow 1: Building Basic Responses

```clojure
(require '[ring.util.response :as response])

;; Success response
(response/response "Success!")
;; => {:status 200, :headers {}, :body "Success!"}

;; JSON response
(response/content-type
  (response/response "{\"status\": \"ok\"}")
  "application/json")
;; => {:status 200, :headers {"Content-Type" "application/json"}, :body ...}

;; 404 Not Found
(response/not-found "Page not found")
;; => {:status 404, :headers {}, :body "Page not found"}

;; 400 Bad Request
(response/bad-request "Invalid input")
;; => {:status 400, :headers {}, :body "Invalid input"}

;; Redirect
(response/redirect "/new-location")
;; => {:status 302, :headers {"Location" "/new-location"}, :body ""}

;; Created with location
(response/created "/users/123" {:id 123 :name "Alice"})
;; => {:status 201, :headers {"Location" "/users/123"}, :body ...}

;; Custom status
(response/status (response/response "Accepted") 202)
;; => {:status 202, :headers {}, :body "Accepted"}

;; Add header
(response/header (response/response "OK") "X-Custom" "value")
;; => {:status 200, :headers {"X-Custom" "value"}, :body "OK"}

;; Set charset
(response/charset (response/response "Hello") "UTF-8")
;; => {:status 200, :headers {"Content-Type" "text/plain; charset=UTF-8"}, :body ...}
```

### Workflow 2: Handling Query Parameters

```clojure
(require '[ring.middleware.params :refer [wrap-params]]
         '[ring.middleware.keyword-params :refer [wrap-keyword-params]])

;; Handler that uses query params
(defn search-handler [request]
  (let [query (get-in request [:params :q])
        page (get-in request [:params :page] "1")]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body (str "Searching for: " query ", page: " page)}))

;; Wrap with middleware to parse params
(def app
  (-> search-handler
      wrap-keyword-params  ; Converts param keys to keywords
      wrap-params))        ; Parses query string and form params

;; Test it
(app {:uri "/search"
      :request-method :get
      :query-string "q=clojure&page=2"})
;; => {:status 200, :headers {...}, :body "Searching for: clojure, page: 2"}
```

### Workflow 3: Serving Static Files

```clojure
(require '[ring.middleware.resource :refer [wrap-resource]]
         '[ring.middleware.file :refer [wrap-file]]
         '[ring.middleware.content-type :refer [wrap-content-type]]
         '[ring.middleware.head :refer [wrap-head]]
         '[ring.util.response :as response])

;; Serve files from resources (classpath)
(defn handler [request]
  (response/not-found "Not found"))

(def app
  (-> handler
      (wrap-resource "public")      ; Serve from resources/public
      wrap-content-type             ; Add Content-Type headers
      wrap-head))                   ; Support HEAD requests

;; Serve files from filesystem
(def file-app
  (-> handler
      (wrap-file "/var/www/static") ; Serve from filesystem
      wrap-content-type
      wrap-head))

;; Serve specific file
(defn serve-image [request]
  (response/file-response "logo.png" {:root "resources/public"}))

;; Check if file exists
(response/resource-response "index.html" {:root "public"})
;; => response map if found, nil otherwise
```

### Workflow 4: Working with Cookies

```clojure
(require '[ring.middleware.cookies :refer [wrap-cookies]]
         '[ring.util.response :as response])

(defn cookie-handler [request]
  ;; Read cookies
  (let [user-id (get-in request [:cookies "user-id" :value])]
    ;; Set cookie in response
    (-> (response/response (str "User ID: " (or user-id "not set")))
        (response/set-cookie "user-id" "12345" 
                            {:max-age 3600      ; Expires in 1 hour
                             :path "/"          ; Available on all paths
                             :http-only true    ; Not accessible via JS
                             :secure false}))))  ; HTTPS only when true

(def app (wrap-cookies cookie-handler))

;; Test it
(app {:uri "/" :request-method :get})
;; => {:status 200, 
;;     :headers {"Set-Cookie" "user-id=12345; Max-Age=3600; Path=/; HttpOnly"}
;;     :body "User ID: not set"}
```

### Workflow 5: Managing Sessions

```clojure
(require '[ring.middleware.session :refer [wrap-session]]
         '[ring.middleware.session.memory :refer [memory-store]]
         '[ring.util.response :as response])

(defn login-handler [request]
  (let [username (get-in request [:params :username])]
    (-> (response/response (str "Logged in as: " username))
        (assoc :session {:user username}))))  ; Store in session

(defn profile-handler [request]
  (let [user (get-in request [:session :user])]
    (if user
      (response/response (str "Welcome back, " user))
      (response/redirect "/login"))))

(defn logout-handler [request]
  (-> (response/response "Logged out")
      (assoc :session nil)))  ; Clear session

;; Configure session middleware
(def app
  (-> (fn [req]
        (case (:uri req)
          "/login" (login-handler req)
          "/profile" (profile-handler req)
          "/logout" (logout-handler req)
          (response/not-found "Not found")))
      (wrap-session {:store (memory-store)      ; In-memory store
                     :cookie-name "my-session"  ; Session cookie name
                     :cookie-attrs {:max-age 3600  ; 1 hour
                                    :http-only true
                                    :secure false}})))
```

### Workflow 6: Handling JSON APIs

```clojure
(require '[ring.middleware.params :refer [wrap-params]]
         '[ring.middleware.keyword-params :refer [wrap-keyword-params]]
         '[ring.util.response :as response]
         '[cheshire.core :as json])

;; Middleware to parse JSON body
(defn wrap-json-body [handler]
  (fn [request]
    (if (= "application/json" (get-in request [:headers "content-type"]))
      (let [body-str (slurp (:body request))
            json-body (json/parse-string body-str true)]
        (handler (assoc request :json-body json-body)))
      (handler request))))

;; Middleware to encode response as JSON
(defn wrap-json-response [handler]
  (fn [request]
    (let [response (handler request)]
      (if (map? (:body response))
        (-> response
            (assoc :body (json/generate-string (:body response)))
            (response/content-type "application/json"))
        response))))

;; JSON API handler
(defn api-handler [request]
  (case [(:request-method request) (:uri request)]
    [:get "/api/users"]
    (response/response [{:id 1 :name "Alice"}
                       {:id 2 :name "Bob"}])
    
    [:post "/api/users"]
    (let [user (:json-body request)]
      (response/created "/api/users/3" 
                       (assoc user :id 3)))
    
    [:get "/api/users/1"]
    (response/response {:id 1 :name "Alice"})
    
    (response/not-found {:error "Not found"})))

;; Compose middleware
(def json-api
  (-> api-handler
      wrap-json-response
      wrap-json-body
      wrap-keyword-params
      wrap-params))
```

### Workflow 7: Running Different Servers

```clojure
;; Jetty (most common, production-ready)
(require '[ring.adapter.jetty :as jetty])
(def server (jetty/run-jetty handler 
              {:port 8080
               :join? false      ; Don't block
               :max-threads 50   ; Thread pool size
               :min-threads 8}))
(.stop server)

;; http-kit (high performance, async)
(require '[org.httpkit.server :as http])
(def server (http/run-server handler {:port 8080}))
(server :timeout 100)  ; Stop with timeout

;; Aleph (async, WebSocket support)
(require '[aleph.http :as aleph])
(def server (aleph/start-server handler {:port 8080}))
(.close server)
```

## When to Use Each Approach

**Use Ring core when:**
- Building any web application in Clojure
- You need a standard, stable HTTP abstraction
- Working with multiple HTTP servers (Ring is the common interface)
- Composing middleware from different sources
- Building reusable web components

**Use response helpers (`ring.util.response`) when:**
- Building responses programmatically
- You need common status codes (404, 302, etc.)
- Setting headers or content types
- Creating redirects or file responses

**Use middleware when:**
- Adding cross-cutting concerns (logging, auth, CORS)
- Processing request/response data (params, JSON, sessions)
- You want reusable, composable functionality
- Building middleware stacks for different environments

**Use different adapters when:**
- **Jetty** - Production apps, stability, servlet compatibility
- **http-kit** - High concurrency, async, WebSockets
- **Aleph** - Async/reactive, WebSockets, streaming

**Don't use Ring directly when:**
- You need routing (use Compojure, Reitit, Bidi)
- You want a full-stack framework (use Luminus, Pedestal)
- Building simple scripts (overkill for non-web use cases)

## Best Practices

**Do:**
- Keep handlers pure and simple (request -> response)
- Use middleware for cross-cutting concerns
- Compose middleware with `->` threading macro (bottom-up)
- Use response helpers instead of manual map creation
- Validate request data before processing
- Set appropriate Content-Type headers
- Handle edge cases (404, 400, 500)
- Test handlers with plain request maps (no server needed)
- Use keyword params middleware for cleaner code
- Return proper status codes

**Don't:**
- Put business logic in middleware
- Mutate request/response maps (return new ones)
- Forget to parse request body when needed
- Ignore middleware ordering (matters!)
- Block handlers with slow operations (use async)
- Return nil as response (always return a map)
- Hardcode URLs (use routing libraries)
- Mix concerns in handlers (separate routing, logic, data)
- Skip Content-Type headers
- Assume params exist (provide defaults)

## Common Issues

### Issue: "Middleware Order Matters"

```clojure
;; Wrong: wrap-keyword-params before wrap-params
(def app
  (-> handler
      wrap-params
      wrap-keyword-params))
;; params are strings, not keywords!

;; Right: wrap-keyword-params after wrap-params
(def app
  (-> handler
      wrap-keyword-params  ; Applied last (outermost)
      wrap-params))        ; Applied first (innermost)

;; Remember: -> applies bottom-to-top for middleware!
```

### Issue: "Session Not Persisting"

```clojure
;; Wrong: Not returning session in response
(defn handler [request]
  {:status 200
   :body "OK"})  ; Session lost!

;; Right: Include session in response
(defn handler [request]
  {:status 200
   :body "OK"
   :session (:session request)})  ; Preserve session

;; Or modify session
(defn handler [request]
  {:status 200
   :body "OK"
   :session (assoc (:session request) :user "alice")})
```

### Issue: "Body Already Consumed"

```clojure
;; Wrong: Reading body multiple times
(defn handler [request]
  (let [body1 (slurp (:body request))
        body2 (slurp (:body request))]  ; Error! Stream consumed
    {:status 200 :body body1}))

;; Right: Read once and store
(defn handler [request]
  (let [body-str (slurp (:body request))]
    ;; Use body-str multiple times
    {:status 200 :body body-str}))
```

### Issue: "Response Not a Map"

```clojure
;; Wrong: Returning nil or non-map
(defn handler [request]
  (when (= "/health" (:uri request))
    {:status 200 :body "OK"}))  ; Returns nil for other URIs!

;; Right: Always return a response map
(defn handler [request]
  (if (= "/health" (:uri request))
    {:status 200 :body "OK"}
    {:status 404 :body "Not found"}))
```

### Issue: "Params Not Available"

```clojure
;; Problem: Accessing :params without middleware
(defn handler [request]
  (let [name (:params request)]  ; nil!
    {:status 200 :body name}))

;; Solution: Add params middleware
(def app
  (-> handler
      wrap-keyword-params
      wrap-params))

;; Now :params is available
(app {:uri "/" :request-method :get :query-string "name=Alice"})
;; request will have {:params {:name "Alice"}}
```

### Issue: "Static Files Not Served"

```clojure
;; Wrong: Middleware order or missing middleware
(def app
  (-> handler
      wrap-resource))  ; Missing wrap-content-type!

;; Right: Add content-type middleware
(def app
  (-> handler
      (wrap-resource "public")
      wrap-content-type   ; Sets Content-Type header
      wrap-head))         ; Supports HEAD requests

;; File structure: resources/public/index.html
;; Access: http://localhost:8080/index.html
```

## Advanced Topics

For comprehensive middleware documentation, see [Ring Wiki - Standard middleware](https://github.com/ring-clojure/ring/wiki/Standard-middleware)

For complex examples and patterns, see [Ring Wiki - Examples](https://github.com/ring-clojure/ring/wiki/Examples)

For WebSocket support, see [Ring Wiki - WebSockets](https://github.com/ring-clojure/ring/wiki/WebSockets)

## Standard Middleware

Ring provides these built-in middleware:

**Request Processing:**
- `wrap-params` - Parse query and form params
- `wrap-keyword-params` - Convert param keys to keywords
- `wrap-multipart-params` - Handle file uploads
- `wrap-nested-params` - Support nested parameter names

**Response Processing:**
- `wrap-content-type` - Set Content-Type headers
- `wrap-head` - Support HEAD requests
- `wrap-not-modified` - Handle 304 Not Modified

**Security:**
- `wrap-cookies` - Parse and set cookies
- `wrap-session` - Session management
- `wrap-ssl-redirect` - Force HTTPS

**Static Resources:**
- `wrap-resource` - Serve from classpath
- `wrap-file` - Serve from filesystem

**Development:**
- `wrap-reload` - Auto-reload modified files (ring-devel)
- `wrap-stacktrace` - Show friendly error pages (ring-devel)

## Related Libraries

- **Routing**: Compojure, Reitit, Bidi
- **JSON**: ring-json, cheshire
- **Authentication**: buddy, friend
- **Validation**: ring-spec, bouncer
- **Defaults**: ring-defaults (common middleware presets)
- **Servers**: ring-jetty-adapter, http-kit, aleph

## External Resources

- [Ring GitHub](https://github.com/ring-clojure/ring)
- [Ring Wiki](https://github.com/ring-clojure/ring/wiki)
- [Ring Spec](https://github.com/ring-clojure/ring/blob/master/SPEC.md)
- [API Documentation](https://ring-clojure.github.io/ring/)
- [Ring Defaults](https://github.com/ring-clojure/ring-defaults)

## Summary

Ring is the foundation of Clojure web development:

1. **Simple abstraction** - Request maps in, response maps out
2. **Composable** - Build applications from small, reusable pieces
3. **Middleware-based** - Add functionality by wrapping handlers
4. **Adapter-independent** - Works with any HTTP server
5. **Well-established** - Stable, documented, widely used

Start with simple handlers and request/response maps. Add middleware as needed. Use response helpers for cleaner code. Ring's simplicity makes it easy to understand, test, and extend.
