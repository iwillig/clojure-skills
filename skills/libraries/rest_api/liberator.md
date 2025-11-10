---
name: liberator-rest-resources
description: |
  Build RESTful HTTP resources with proper HTTP semantics and automatic content negotiation.
  Use when building REST APIs, implementing HTTP caching, handling content negotiation,
  supporting multiple response formats, implementing proper HTTP status codes, or when the
  user mentions REST resources, HTTP decision graph, content negotiation, ETags, conditional
  requests, OPTIONS, HEAD, or RFC-2616 compliance. Liberator provides a declarative way to
  build resources that automatically handle HTTP semantics like caching, authentication,
  method validation, and content type negotiation.
---

# Liberator - REST Resources for Clojure

## Quick Start

Liberator helps you build RESTful resources that automatically comply with HTTP specifications (RFC-2616).

```clojure
(require '[liberator.core :refer [resource defresource]])

;; Simple read-only resource
(def hello-resource
  (resource
    :available-media-types ["text/plain"]
    :handle-ok "Hello, World!"))

;; Use as Ring handler
(hello-resource {:request-method :get})
;; => {:status 200
;;     :headers {"Content-Type" "text/plain;charset=UTF-8", "Vary" "Accept"}
;;     :body "Hello, World!"}

;; Resource with content negotiation
(def data-resource
  (resource
    :available-media-types ["application/json" "text/html"]
    :handle-ok (fn [ctx]
                 (case (get-in ctx [:representation :media-type])
                   "application/json" {:users ["alice" "bob"]}
                   "text/html" "<html><body>Users: alice, bob</body></html>"))))

;; JSON is automatically serialized
(data-resource {:request-method :get
                :headers {"accept" "application/json"}})
;; => {:status 200, :body "{\"users\":[\"alice\",\"bob\"]}", ...}
```

**Key benefits:**
- **HTTP compliant** - Automatically follows RFC-2616 specifications
- **Declarative** - Define what resources do, not how HTTP works
- **Content negotiation** - Supports multiple formats automatically
- **Caching** - ETags, Last-Modified, conditional requests built-in
- **Decision graph** - Clear flow through HTTP decision points

## Core Concepts

### Resources as Ring Handlers

Resources are Ring handlers that process requests through a decision graph:

```clojure
;; A resource is just a function that takes a request map
(def my-resource (resource :handle-ok "Response"))

;; Call it like any Ring handler
(my-resource {:request-method :get})
;; => {:status 200, :headers {...}, :body "Response"}
```

### Decision Graph

Liberator processes requests through a series of decisions that determine the response:

```
service-available? → known-method? → uri-too-long? → method-allowed?
  → malformed? → authorized? → allowed? → valid-content-header?
  → known-content-type? → valid-entity-length? → exists?
  → ...and many more
```

Each decision point can:
- Return `true`/`false` to continue through the graph
- Return a map to update the context
- Return a value that becomes part of the response

### Context

The context flows through all decisions and handlers, accumulating state:

```clojure
(resource
  :exists? (fn [ctx]
             ;; Check if resource exists, add to context
             (if-let [user (db/find-user (get-in ctx [:request :params :id]))]
               {:user user}  ; Add user to context
               false))       ; Resource doesn't exist
  :handle-ok (fn [ctx]
              ;; Access user from context
              (:user ctx)))
```

### Handlers vs Decisions

**Decisions** determine the flow (end with `?`):
- Return truthy/falsy values
- Can update context
- Examples: `:exists?`, `:authorized?`, `:allowed?`

**Handlers** generate responses:
- Return the response body
- Examples: `:handle-ok`, `:handle-not-found`, `:handle-created`

**Actions** perform side effects (end with `!`):
- Modify state (create/update/delete)
- Return context updates
- Examples: `:post!`, `:put!`, `:delete!`

## Common Workflows

### Workflow 1: Read-Only Resource with Content Negotiation

Serve data in multiple formats:

```clojure
(require '[liberator.core :refer [resource]]
         '[cheshire.core :as json])

(def users-db
  {1 {:id 1 :name "Alice" :email "alice@example.com"}
   2 {:id 2 :name "Bob" :email "bob@example.com"}})

(def users-resource
  (resource
    :available-media-types ["application/json" "text/html" "text/plain"]
    :handle-ok (fn [ctx]
                 (let [users (vals users-db)
                       media-type (get-in ctx [:representation :media-type])]
                   (case media-type
                     "application/json" users  ; Auto-serialized to JSON
                     "text/html" (str "<html><body><ul>"
                                     (apply str (map #(str "<li>" (:name %) "</li>") users))
                                     "</ul></body></html>")
                     "text/plain" (apply str (map #(str (:name %) "\n") users)))))))

;; Request JSON
(users-resource {:request-method :get
                 :headers {"accept" "application/json"}})
;; => {:status 200, :body "[{\"id\":1,\"name\":\"Alice\"...}]", ...}

;; Request HTML
(users-resource {:request-method :get
                 :headers {"accept" "text/html"}})
;; => {:status 200, :body "<html><body><ul><li>Alice</li>...", ...}
```

### Workflow 2: CRUD Resource with Validation

Full create, read, update, delete with validation:

```clojure
(require '[liberator.core :refer [resource]])

(def users-store (atom users-db))

(defn user-resource [id]
  (resource
    :available-media-types ["application/json"]
    :allowed-methods [:get :put :delete]
    
    ;; Check if user exists
    :exists? (fn [ctx]
               (if-let [user (get @users-store id)]
                 {:user user}
                 false))
    
    ;; Validate PUT requests
    :malformed? (fn [ctx]
                  (when (= :put (get-in ctx [:request :request-method]))
                    (let [body (get-in ctx [:request :body])]
                      (if (and body (:name body) (:email body))
                        false  ; Valid
                        [true {:message "Missing required fields: name, email"}]))))
    
    ;; Update user
    :put! (fn [ctx]
            (let [updated-user (merge {:id id}
                                     (get-in ctx [:request :body]))]
              (swap! users-store assoc id updated-user)
              {:user updated-user}))
    
    ;; Delete user
    :delete! (fn [ctx]
               (swap! users-store dissoc id))
    
    ;; Handlers
    :handle-ok (fn [ctx] (:user ctx))
    :handle-not-found "User not found"
    :handle-malformed (fn [ctx] (:message ctx))))

;; GET existing user
(user-resource 1)
({:request-method :get})
;; => {:status 200, :body "{\"id\":1,\"name\":\"Alice\"...}", ...}

;; PUT to update user
((user-resource 1)
 {:request-method :put
  :body {:name "Alice Smith" :email "alice.smith@example.com"}})
;; => {:status 200, ...}

;; DELETE user
((user-resource 1)
 {:request-method :delete})
;; => {:status 204, :body nil}
```

### Workflow 3: Creating Resources with POST

Handle POST to create new resources:

```clojure
(def next-id (atom 3))

(def users-collection-resource
  (resource
    :available-media-types ["application/json"]
    :allowed-methods [:get :post]
    
    ;; Validate POST body
    :malformed? (fn [ctx]
                  (when (= :post (get-in ctx [:request :request-method]))
                    (let [body (get-in ctx [:request :body])]
                      (not (and body (:name body) (:email body))))))
    
    ;; Create new user
    :post! (fn [ctx]
             (let [new-id (swap! next-id inc)
                   new-user (merge {:id new-id}
                                  (get-in ctx [:request :body]))]
               (swap! users-store assoc new-id new-user)
               {:user new-user}))
    
    ;; Return location of created resource
    :location (fn [ctx]
                (str "/users/" (get-in ctx [:user :id])))
    
    ;; List all users on GET
    :handle-ok (fn [ctx]
                 (vals @users-store))
    
    ;; Return created user on POST
    :handle-created (fn [ctx]
                      (:user ctx))))

;; POST to create user
(users-collection-resource
  {:request-method :post
   :body {:name "Charlie" :email "charlie@example.com"}})
;; => {:status 201
;;     :headers {"Location" "/users/3", ...}
;;     :body "{\"id\":3,\"name\":\"Charlie\"...}"}
```

### Workflow 4: Authentication and Authorization

Protect resources with auth:

```clojure
(def admin-resource
  (resource
    :available-media-types ["application/json"]
    :allowed-methods [:get :post]
    
    ;; Check if request has valid auth
    :authorized? (fn [ctx]
                   (if-let [token (get-in ctx [:request :headers "authorization"])]
                     (if (valid-token? token)
                       {:user (get-user-from-token token)}
                       false)
                     false))
    
    ;; Check if user is admin
    :allowed? (fn [ctx]
                (= "admin" (get-in ctx [:user :role])))
    
    :handle-ok "Admin data"
    :handle-unauthorized "Please authenticate"
    :handle-forbidden "Insufficient permissions"))

;; No auth header
(admin-resource {:request-method :get})
;; => {:status 401, :body "Please authenticate"}

;; Valid auth but not admin
(admin-resource {:request-method :get
                 :headers {"authorization" "Bearer user-token"}})
;; => {:status 403, :body "Insufficient permissions"}

;; Valid admin auth
(admin-resource {:request-method :get
                 :headers {"authorization" "Bearer admin-token"}})
;; => {:status 200, :body "Admin data"}
```

### Workflow 5: ETags and Conditional Requests

Implement caching with ETags:

```clojure
(def cached-resource
  (resource
    :available-media-types ["application/json"]
    :exists? (fn [ctx]
               (if-let [data (get-data-from-db)]
                 {:data data}
                 false))
    
    ;; Generate ETag from data
    :etag (fn [ctx]
            (str "\"" (hash (get-in ctx [:data])) "\""))
    
    ;; Generate Last-Modified timestamp
    :last-modified (fn [ctx]
                     (get-in ctx [:data :updated-at]))
    
    :handle-ok (fn [ctx]
                 (:data ctx))))

;; First request - returns full response with ETag
(cached-resource {:request-method :get})
;; => {:status 200
;;     :headers {"ETag" "\"12345\"", "Last-Modified" "...", ...}
;;     :body "{...}"}

;; Conditional request with matching ETag - not modified
(cached-resource {:request-method :get
                  :headers {"if-none-match" "\"12345\""}})
;; => {:status 304, :body nil}  ; Not Modified

;; Conditional request with old ETag - returns new data
(cached-resource {:request-method :get
                  :headers {"if-none-match" "\"99999\""}})
;; => {:status 200, :body "{...}"}  ; Full response
```

### Workflow 6: Handling Different Methods

Support multiple HTTP methods on same resource:

```clojure
(require '[liberator.core :refer [by-method]])

(def item-resource
  (resource
    :available-media-types ["application/json"]
    :allowed-methods [:get :post :put :patch :delete :head :options]
    
    :exists? (fn [ctx]
               {:item (get-item-from-db)})
    
    ;; Use by-method helper to handle different methods
    :handle-ok (by-method
                 {:get (fn [ctx] (:item ctx))
                  :head (fn [ctx] nil)})  ; HEAD returns no body
    
    :post! (fn [ctx]
             {:item (create-item (get-in ctx [:request :body]))})
    
    :put! (fn [ctx]
            {:item (replace-item (get-in ctx [:request :body]))})
    
    :patch! (fn [ctx]
              {:item (update-item (get-in ctx [:request :body]))})
    
    :delete! (fn [ctx]
               (delete-item)
               {})
    
    :handle-created (fn [ctx] (:item ctx))))

;; OPTIONS request - returns allowed methods
(item-resource {:request-method :options})
;; => {:status 200
;;     :headers {"Allow" "GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS"}
;;     :body nil}
```

### Workflow 7: Conneg with Language and Charset

Full content negotiation including language and charset:

```clojure
(def i18n-resource
  (resource
    :available-media-types ["text/html"]
    :available-languages ["en" "es" "fr"]
    :available-charsets ["UTF-8" "ISO-8859-1"]
    
    :handle-ok (fn [ctx]
                 (let [lang (get-in ctx [:representation :language])]
                   (case lang
                     "en" "<h1>Hello</h1>"
                     "es" "<h1>Hola</h1>"
                     "fr" "<h1>Bonjour</h1>"
                     "<h1>Hello</h1>")))))

;; Request in Spanish
(i18n-resource {:request-method :get
                :headers {"accept-language" "es"}})
;; => {:status 200, :body "<h1>Hola</h1>", ...}

;; Request in French
(i18n-resource {:request-method :get
                :headers {"accept-language" "fr"}})
;; => {:status 200, :body "<h1>Bonjour</h1>", ...}

;; No language preference - defaults to first available
(i18n-resource {:request-method :get})
;; => {:status 200, :body "<h1>Hello</h1>", ...}
```

### Workflow 8: Using defresource for Reusable Resources

Define reusable resource templates with `defresource`:

```clojure
(require '[liberator.core :refer [defresource]])

;; Define resource as a function
(defresource user-resource [user-id]
  :available-media-types ["application/json"]
  :allowed-methods [:get :put :delete]
  
  :exists? (fn [ctx]
             (if-let [user (get @users-store user-id)]
               {:user user}
               false))
  
  :put! (fn [ctx]
          (let [updated (merge {:id user-id}
                              (get-in ctx [:request :body]))]
            (swap! users-store assoc user-id updated)
            {:user updated}))
  
  :delete! (fn [ctx]
             (swap! users-store dissoc user-id))
  
  :handle-ok (fn [ctx] (:user ctx))
  :handle-not-found "User not found")

;; Use with routing
(defroutes app-routes
  (GET "/users/:id" [id]
    (user-resource (Integer/parseInt id)))
  
  (PUT "/users/:id" [id]
    (user-resource (Integer/parseInt id)))
  
  (DELETE "/users/:id" [id]
    (user-resource (Integer/parseInt id))))
```

## When to Use Liberator

**Use Liberator when:**
- Building REST APIs that need proper HTTP semantics
- You want automatic content negotiation
- Need to support conditional requests (ETags, Last-Modified)
- Want declarative resource definitions
- Need proper HTTP status codes without manual logic
- Building hypermedia APIs (HATEOAS)
- Compliance with HTTP specifications is important

**Use simpler routing when:**
- Building simple CRUD APIs without content negotiation
- Performance is absolutely critical (Liberator has overhead)
- You only need JSON responses
- HTTP caching is not needed
- Team is unfamiliar with HTTP decision graphs

**Use with:**
- **Ring** - Liberator resources are Ring handlers
- **Compojure/Reitit/Bidi** - For routing to resources
- **Cheshire** - JSON serialization (automatic)
- **Hiccup** - HTML generation

## Best Practices

**Do:**
- Use context to pass data between decisions and handlers
- Leverage content negotiation for multiple formats
- Return maps from decisions to update context
- Use `:etag` and `:last-modified` for caching
- Define `:allowed-methods` explicitly
- Use `:malformed?` to validate input early
- Separate authentication (`:authorized?`) from authorization (`:allowed?`)
- Use `by-method` for method-specific handlers

```clojure
;; Good: Update context in decisions
(resource
  :exists? (fn [ctx]
             (if-let [data (fetch-data)]
               {:data data}  ; Add to context
               false))
  :handle-ok (fn [ctx]
               (:data ctx)))  ; Use from context

;; Good: Validate early
(resource
  :malformed? (fn [ctx]
                (let [body (get-in ctx [:request :body])]
                  (not (valid? body))))
  :post! (fn [ctx]
           ;; Body is guaranteed valid here
           (create-item (get-in ctx [:request :body]))))
```

**Don't:**
- Fetch data in multiple places (use context)
- Ignore content negotiation (use `:available-media-types`)
- Return raw Ring responses from handlers (return data)
- Skip validation (use `:malformed?`)
- Mix authentication and authorization logic
- Forget to set `:allowed-methods`

```clojure
;; Bad: Fetching data multiple times
(resource
  :exists? (fn [ctx] (fetch-data))
  :handle-ok (fn [ctx] (fetch-data)))  ; Fetches again!

;; Good: Fetch once, pass through context
(resource
  :exists? (fn [ctx]
             (if-let [data (fetch-data)]
               {:data data}
               false))
  :handle-ok (fn [ctx] (:data ctx)))

;; Bad: Manual Ring response
(resource
  :handle-ok (fn [ctx]
               {:status 200
                :headers {"Content-Type" "application/json"}
                :body (json/generate-string {:foo "bar"})}))

;; Good: Return data, let Liberator handle HTTP
(resource
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx] {:foo "bar"}))
```

## Common Issues

### Issue: Context data not available in handler

```clojure
(resource
  :exists? (fn [ctx]
             (fetch-data)  ; Returns truthy but doesn't update context
             true)
  :handle-ok (fn [ctx]
               (:data ctx)))  ; => nil
```

**Solution**: Return a map to update context:

```clojure
(resource
  :exists? (fn [ctx]
             (if-let [data (fetch-data)]
               {:data data}  ; Updates context
               false))
  :handle-ok (fn [ctx]
               (:data ctx)))  ; => data
```

### Issue: 406 Not Acceptable error

```clojure
(resource
  :handle-ok "Hello")

;; Request
{:request-method :get
 :headers {"accept" "application/json"}}
;; => {:status 406}  ; Not Acceptable
```

**Solution**: Specify available media types:

```clojure
(resource
  :available-media-types ["application/json" "text/plain"]
  :handle-ok "Hello")
```

### Issue: Method not allowed

```clojure
(resource
  :handle-ok "Hello")

;; POST request
{:request-method :post}
;; => {:status 405}  ; Method Not Allowed
```

**Solution**: Specify allowed methods:

```clojure
(resource
  :allowed-methods [:get :post]
  :handle-ok "Hello"
  :post! (fn [ctx] {:created true})
  :handle-created "Created!")
```

### Issue: Actions not executing

```clojure
(resource
  :allowed-methods [:post]
  :post! (fn [ctx]
           (println "Creating resource")
           (create-resource)))

;; POST request succeeds but nothing is created
```

**Solution**: Actions need to return context updates (or nil):

```clojure
(resource
  :allowed-methods [:post]
  :post! (fn [ctx]
           (let [new-resource (create-resource)]
             {:resource new-resource}))  ; Return context update
  :handle-created (fn [ctx]
                    (:resource ctx)))
```

### Issue: Body not being parsed

```clojure
(resource
  :allowed-methods [:post]
  :post! (fn [ctx]
           (println (get-in ctx [:request :body])))  ; => InputStream
  :handle-created "Created")
```

**Solution**: Use Ring middleware to parse body:

```clojure
(require '[ring.middleware.json :refer [wrap-json-body]])

(def app
  (-> (resource ...)
      (wrap-json-body {:keywords? true})))
```

### Issue: JSON not serializing automatically

```clojure
(resource
  :handle-ok (fn [ctx]
               {:message "Hello"}))  ; Returns map, not JSON string
```

**Solution**: Either specify media type or manually serialize:

```clojure
;; Option 1: Let Liberator serialize (requires media type)
(resource
  :available-media-types ["application/json"]
  :handle-ok {:message "Hello"})  ; Auto-serialized

;; Option 2: Manual serialization
(resource
  :handle-ok (json/generate-string {:message "Hello"}))
```

## Advanced Topics

### Decision Graph Debugging

Use `:trace?` to see decision flow:

```clojure
(def debug-resource
  (resource
    :trace? true  ; Enable tracing
    :exists? (fn [ctx] false)
    :handle-not-found "Not found"))

(debug-resource {:request-method :get})
;; Adds :trace to response with all decisions executed
```

### Custom Media Types

Handle custom content types:

```clojure
(resource
  :available-media-types ["application/vnd.myapp+json"
                          "application/vnd.myapp.v2+json"]
  :handle-ok (fn [ctx]
               (case (get-in ctx [:representation :media-type])
                 "application/vnd.myapp+json" {:version 1 :data "..."}
                 "application/vnd.myapp.v2+json" {:version 2 :data "..."})))
```

### Combining with Ring Middleware

```clojure
(require '[ring.middleware.params :refer [wrap-params]]
         '[ring.middleware.json :refer [wrap-json-body wrap-json-response]])

(def app
  (-> (resource
        :available-media-types ["application/json"]
        :handle-ok {:message "Hello"})
      wrap-json-response
      (wrap-json-body {:keywords? true})
      wrap-params))
```

### Hypermedia (HATEOAS)

Include links in responses:

```clojure
(resource
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
               {:user {:id 1 :name "Alice"}
                :links {:self "/users/1"
                        :friends "/users/1/friends"
                        :posts "/users/1/posts"}}))
```

## Related Libraries

- **ring** - Web application library (Liberator builds on Ring)
- **compojure** - Routing library
- **reitit** - Data-driven routing
- **bidi** - Bidirectional routing
- **cheshire** - JSON encoding/decoding
- **yada** - Alternative resource library with similar goals

## Resources

- Official site: https://clojure-liberator.github.io/liberator/
- GitHub: https://github.com/clojure-liberator/liberator
- Decision graph: https://clojure-liberator.github.io/liberator/tutorial/decision-graph.html
- Tutorial: https://clojure-liberator.github.io/liberator/tutorial/
- HTTP spec (RFC 2616): https://www.ietf.org/rfc/rfc2616.txt

## Summary

Liberator enables building proper REST resources in Clojure:

1. **HTTP Compliant** - Automatically follows HTTP specifications
2. **Content Negotiation** - Support multiple formats automatically
3. **Declarative** - Define resources with decision functions
4. **Caching** - Built-in support for ETags and conditional requests
5. **Decision Graph** - Clear, predictable request flow
6. **Ring Integration** - Works seamlessly with Ring ecosystem

**Most common patterns:**

```clojure
;; Simple read-only resource
(resource
  :available-media-types ["application/json"]
  :handle-ok {:message "Hello"})

;; Resource with existence check
(resource
  :exists? (fn [ctx]
             (if-let [data (fetch-data)]
               {:data data}
               false))
  :handle-ok (fn [ctx] (:data ctx))
  :handle-not-found "Not found")

;; CRUD resource
(resource
  :allowed-methods [:get :put :delete]
  :exists? (fn [ctx] {:item (fetch-item)})
  :put! (fn [ctx] (update-item))
  :delete! (fn [ctx] (delete-item))
  :handle-ok (fn [ctx] (:item ctx)))

;; Authentication
(resource
  :authorized? (fn [ctx] (valid-auth? ctx))
  :allowed? (fn [ctx] (has-permission? ctx))
  :handle-ok "Protected data"
  :handle-unauthorized "Login required"
  :handle-forbidden "Insufficient permissions")
```

Perfect for building REST APIs that properly implement HTTP semantics, content negotiation, and caching.
