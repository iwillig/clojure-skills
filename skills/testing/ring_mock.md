---
name: ring-mock-testing
description: |
  Test Ring web applications with mock HTTP requests using ring-mock. Use when
  testing Ring handlers, middleware, web APIs, HTTP request handling, or when
  the user mentions Ring testing, mock requests, HTTP testing, handler testing,
  middleware testing, web application testing, or unit testing web handlers.
---

# Ring Mock

## Quick Start

ring-mock provides utilities for creating mock HTTP request maps to test Ring handlers and middleware without starting a real HTTP server.

```clojure
;; Add dependency
{:deps {ring/ring-mock {:mvn/version "0.4.0"}}}

;; Basic usage
(require '[ring.mock.request :as mock])

;; Create a simple GET request
(def request (mock/request :get "/"))
;; => {:protocol "HTTP/1.1", :server-port 80, :server-name "localhost",
;;     :remote-addr "127.0.0.1", :uri "/", :scheme :http,
;;     :request-method :get, :headers {"host" "localhost"}}

;; Test a handler
(defn my-handler [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello, World!"})

(my-handler (mock/request :get "/"))
;; => {:status 200, :headers {"Content-Type" "text/plain"}, :body "Hello, World!"}
```

**Key benefits:**
- Fast unit tests without starting HTTP servers
- Composable request building with threading macros
- Support for all HTTP methods
- JSON and form-encoded body helpers
- Cookie and header manipulation
- Works seamlessly with Ring middleware

## Core Concepts

### Mock Request Maps

ring-mock creates Ring-compliant request maps that match what a real HTTP server would produce:

```clojure
(mock/request :get "/users")
;; => {:protocol "HTTP/1.1"
;;     :server-port 80
;;     :server-name "localhost"
;;     :remote-addr "127.0.0.1"
;;     :uri "/users"
;;     :scheme :http
;;     :request-method :get
;;     :headers {"host" "localhost"}}
```

All mock requests include sensible defaults for required Ring keys.

### Request Building with Threading

The primary pattern is using `->` to build requests by chaining helper functions:

```clojure
(-> (mock/request :post "/api/users")
    (mock/header "Authorization" "Bearer token123")
    (mock/json-body {:name "Alice" :email "alice@example.com"}))
```

Each helper function returns a modified request map, making them composable.

### Helper Functions

**Core request builder:**
- `(request method uri)` - Create base request
- `(request method uri params)` - Create request with query parameters

**Request modifiers:**
- `(header request name value)` - Add HTTP header
- `(body request value)` - Set body (string or form params)
- `(json-body request value)` - Set JSON body with proper Content-Type
- `(content-type request type)` - Set Content-Type header
- `(content-length request length)` - Set Content-Length header
- `(query-string request params)` - Set query string
- `(cookie request name value)` - Add cookie

## Common Workflows

### Workflow 1: Testing Basic Handlers

```clojure
(require '[ring.mock.request :as mock]
         '[clojure.test :refer [deftest is testing]])

(defn hello-handler [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body (str "Hello, " (get-in request [:params :name] "World") "!")})

(deftest test-hello-handler
  (testing "responds with hello message"
    (let [response (hello-handler (mock/request :get "/"))]
      (is (= 200 (:status response)))
      (is (= "Hello, World!" (:body response)))))

  (testing "uses name parameter when provided"
    (let [response (hello-handler 
                     (mock/request :get "/hello" {:name "Alice"}))]
      (is (= "Hello, Alice!" (:body response))))))
```

### Workflow 2: Testing JSON APIs

```clojure
(require '[ring.mock.request :as mock]
         '[cheshire.core :as json])

(defn create-user-handler [request]
  (let [user (-> request :body slurp (json/parse-string true))]
    {:status 201
     :headers {"Content-Type" "application/json"}
     :body (json/generate-string 
             {:id 123
              :name (:name user)
              :email (:email user)})}))

(deftest test-create-user
  (testing "creates user from JSON body"
    (let [request (-> (mock/request :post "/api/users")
                      (mock/json-body {:name "Bob" 
                                       :email "bob@example.com"}))
          response (create-user-handler request)
          body (json/parse-string (:body response) true)]
      
      (is (= 201 (:status response)))
      (is (= "application/json" (get-in response [:headers "Content-Type"])))
      (is (= "Bob" (:name body)))
      (is (= "bob@example.com" (:email body)))
      (is (= 123 (:id body))))))
```

### Workflow 3: Testing Authentication

```clojure
(defn protected-handler [request]
  (if-let [auth (get-in request [:headers "authorization"])]
    (if (.startsWith auth "Bearer ")
      {:status 200 :headers {} :body "Authorized"}
      {:status 401 :headers {} :body "Invalid token"})
    {:status 401 :headers {} :body "Unauthorized"}))

(deftest test-protected-endpoint
  (testing "rejects requests without auth header"
    (let [response (protected-handler (mock/request :get "/protected"))]
      (is (= 401 (:status response)))
      (is (= "Unauthorized" (:body response)))))

  (testing "accepts valid bearer token"
    (let [response (protected-handler 
                     (-> (mock/request :get "/protected")
                         (mock/header "Authorization" "Bearer valid-token")))]
      (is (= 200 (:status response)))
      (is (= "Authorized" (:body response)))))

  (testing "rejects invalid auth format"
    (let [response (protected-handler 
                     (-> (mock/request :get "/protected")
                         (mock/header "Authorization" "Basic credentials")))]
      (is (= 401 (:status response))))))
```

### Workflow 4: Testing Middleware

```clojure
(require '[ring.middleware.params :refer [wrap-params]])

(defn echo-params-handler [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string (:params request))})

(def app (wrap-params echo-params-handler))

(deftest test-middleware
  (testing "wrap-params parses query parameters"
    (let [response (app (mock/request :get "/search" {:q "clojure" :page 1}))
          body (json/parse-string (:body response) true)]
      (is (= 200 (:status response)))
      (is (= "clojure" (:q body)))
      (is (= "1" (:page body)))))  ; Note: params are strings

  (testing "wrap-params parses form body"
    (let [response (app (-> (mock/request :post "/login")
                            (mock/body {:username "alice" 
                                        :password "secret"})))
          body (json/parse-string (:body response) true)]
      (is (= "alice" (:username body)))
      (is (= "secret" (:password body))))))
```

### Workflow 5: Testing Form Submissions

```clojure
(defn login-handler [request]
  (let [params (:params request)]
    (if (and (= "alice" (:username params))
             (= "secret" (:password params)))
      {:status 302
       :headers {"Location" "/dashboard"}
       :body ""}
      {:status 401
       :headers {}
       :body "Invalid credentials"})))

(def login-app (wrap-params login-handler))

(deftest test-login
  (testing "successful login redirects"
    (let [response (login-app 
                     (-> (mock/request :post "/login")
                         (mock/body {:username "alice" 
                                     :password "secret"})))]
      (is (= 302 (:status response)))
      (is (= "/dashboard" (get-in response [:headers "Location"])))))

  (testing "failed login returns 401"
    (let [response (login-app 
                     (-> (mock/request :post "/login")
                         (mock/body {:username "alice" 
                                     :password "wrong"})))]
      (is (= 401 (:status response)))
      (is (= "Invalid credentials" (:body response))))))
```

### Workflow 6: Testing Cookies

```clojure
(defn session-handler [request]
  (if-let [session-id (get-in request [:headers "cookie"])]
    {:status 200 
     :headers {} 
     :body (str "Session: " session-id)}
    {:status 401 
     :headers {} 
     :body "No session"}))

(deftest test-cookies
  (testing "handles requests without cookies"
    (let [response (session-handler (mock/request :get "/profile"))]
      (is (= 401 (:status response)))
      (is (= "No session" (:body response)))))

  (testing "handles requests with cookies"
    (let [response (session-handler 
                     (-> (mock/request :get "/profile")
                         (mock/cookie "session-id" "abc123")))]
      (is (= 200 (:status response)))
      (is (.contains (:body response) "session-id=abc123")))))
```

### Workflow 7: Testing Different HTTP Methods

```clojure
(defn restful-handler [request]
  (case (:request-method request)
    :get {:status 200 :headers {} :body "GET response"}
    :post {:status 201 :headers {} :body "Created"}
    :put {:status 200 :headers {} :body "Updated"}
    :delete {:status 204 :headers {} :body ""}
    {:status 405 :headers {} :body "Method not allowed"}))

(deftest test-http-methods
  (testing "GET request"
    (let [response (restful-handler (mock/request :get "/resource"))]
      (is (= 200 (:status response)))
      (is (= "GET response" (:body response)))))

  (testing "POST request"
    (let [response (restful-handler (mock/request :post "/resource"))]
      (is (= 201 (:status response)))
      (is (= "Created" (:body response)))))

  (testing "PUT request"
    (let [response (restful-handler (mock/request :put "/resource"))]
      (is (= 200 (:status response)))
      (is (= "Updated" (:body response)))))

  (testing "DELETE request"
    (let [response (restful-handler (mock/request :delete "/resource"))]
      (is (= 204 (:status response)))
      (is (= "" (:body response)))))

  (testing "unsupported method"
    (let [response (restful-handler (mock/request :patch "/resource"))]
      (is (= 405 (:status response))))))
```

### Workflow 8: Testing Content Types

```clojure
(defn content-aware-handler [request]
  (let [content-type (get-in request [:headers "content-type"])]
    {:status 200
     :headers {"Content-Type" content-type}
     :body (str "Received: " content-type)}))

(deftest test-content-types
  (testing "JSON content type"
    (let [response (content-aware-handler 
                     (-> (mock/request :post "/upload")
                         (mock/content-type "application/json")))]
      (is (= "application/json" 
             (get-in response [:headers "Content-Type"])))))

  (testing "XML content type"
    (let [response (content-aware-handler 
                     (-> (mock/request :post "/upload")
                         (mock/content-type "application/xml")))]
      (is (= "application/xml" 
             (get-in response [:headers "Content-Type"])))))

  (testing "multipart form data"
    (let [response (content-aware-handler 
                     (-> (mock/request :post "/upload")
                         (mock/content-type "multipart/form-data")))]
      (is (= "multipart/form-data" 
             (get-in response [:headers "Content-Type"]))))))
```

## When to Use ring-mock

**Use ring-mock when:**
- Unit testing Ring handlers
- Testing middleware behavior
- Testing API endpoints without HTTP server overhead
- Need fast, isolated tests
- Testing request/response transformations
- Verifying authentication/authorization logic
- Testing form handling and validation

**Don't use ring-mock when:**
- Integration testing with real HTTP clients
- Testing server startup/shutdown
- Testing WebSocket connections
- Need to test actual network behavior
- Testing SSL/TLS configuration
- Testing server-specific features (use appropriate server adapter tests)

## Best Practices

**DO:**
- Use threading macros (`->`) to build requests clearly
- Test both success and failure cases
- Verify status codes, headers, and body content
- Use `mock/json-body` for JSON APIs (sets Content-Type automatically)
- Test middleware composition
- Keep tests focused on single aspects
- Use descriptive test names
- Test edge cases (missing headers, invalid data)

**DON'T:**
- Start actual HTTP servers in unit tests (use integration tests instead)
- Forget to test authentication headers
- Skip testing error responses
- Hardcode expected values (use constants/fixtures)
- Test multiple concerns in one test
- Ignore Content-Type headers
- Assume parameter types (middleware converts strings)

## Common Issues

### Issue: Parameters Are Strings

**Problem:** Query parameters and form data are strings, not their original types.

```clojure
(mock/request :get "/api/users" {:page 1})
;; => {:query-string "page=1", ...}

;; After wrap-params middleware
;; (:params request) => {"page" "1"}  ; String, not integer
```

**Solution:** Parse parameters in your handler or test expectations:

```clojure
(defn handler [request]
  (let [page (Integer/parseInt (get-in request [:params "page"] "1"))]
    {:status 200 :body (str "Page: " page)}))

;; Or test for string values
(deftest test-handler
  (let [response (app (mock/request :get "/search" {:page 1}))]
    (is (= "Page: 1" (:body response)))))  ; Expect string
```

### Issue: Body Is InputStream

**Problem:** Mock request body is an InputStream that can only be read once.

```clojure
(def req (-> (mock/request :post "/api/users")
             (mock/json-body {:name "Alice"})))

(slurp (:body req))  ; Works first time
(slurp (:body req))  ; Returns empty string (stream exhausted)
```

**Solution:** Read the body only once in your handler, or recreate the request for each test:

```clojure
(deftest test-multiple-times
  (testing "first test"
    (let [req (-> (mock/request :post "/api/users")
                  (mock/json-body {:name "Alice"}))]
      (is (= 201 (:status (handler req))))))

  (testing "second test"
    (let [req (-> (mock/request :post "/api/users")  ; New request
                  (mock/json-body {:name "Bob"}))]
      (is (= 201 (:status (handler req)))))))
```

### Issue: Missing Middleware Effects

**Problem:** Testing unwrapped handler doesn't apply middleware transformations.

```clojure
(defn handler [request]
  ;; Expects :params to exist (added by wrap-params)
  {:status 200 :body (str "User: " (get-in request [:params :user]))})

(deftest test-handler
  (let [response (handler (mock/request :get "/profile" {:user "alice"}))]
    (is (= "User: alice" (:body response)))))  ; FAILS: :params is nil
```

**Solution:** Test the wrapped handler, not the raw handler:

```clojure
(def app (wrap-params handler))

(deftest test-handler
  (let [response (app (mock/request :get "/profile" {:user "alice"}))]
    (is (= "User: alice" (:body response)))))  ; Works
```

### Issue: JSON Body Not Parsed

**Problem:** `mock/json-body` creates the body but doesn't parse it in the handler.

```clojure
(defn handler [request]
  ;; Expects :body-params or parsed JSON (requires middleware)
  (let [data (:body-params request)]
    {:status 200 :body (str "Name: " (:name data))}))

(deftest test
  (let [response (handler (-> (mock/request :post "/api/users")
                              (mock/json-body {:name "Alice"})))]
    (is (= "Name: Alice" (:body response)))))  ; FAILS: :body-params is nil
```

**Solution:** Either use JSON middleware or parse in your handler:

```clojure
;; Option 1: Use middleware
(require '[ring.middleware.json :refer [wrap-json-body]])
(def app (wrap-json-body handler {:keywords? true}))

;; Option 2: Parse manually in handler
(defn handler [request]
  (let [data (-> request :body slurp (json/parse-string true))]
    {:status 200 :body (str "Name: " (:name data))}))
```

## Integration with Test Frameworks

### With clojure.test

```clojure
(ns myapp.handler-test
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as mock]
            [myapp.handler :refer [app]]))

(deftest test-app
  (testing "homepage"
    (let [response (app (mock/request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "api endpoint"
    (let [response (app (-> (mock/request :post "/api/data")
                            (mock/json-body {:value 42})))]
      (is (= 201 (:status response))))))
```

### With Kaocha

```clojure
;; tests.edn
#kaocha/v1
{:tests [{:id :unit
          :test-paths ["test"]}]}
```

```clojure
;; Run with: bb test
(ns myapp.handler-test
  (:require [clojure.test :refer [deftest is]]
            [ring.mock.request :as mock]))
```

## Advanced Topics

### Testing with Multiple Middleware Layers

```clojure
(require '[ring.middleware.params :refer [wrap-params]]
         '[ring.middleware.keyword-params :refer [wrap-keyword-params]]
         '[ring.middleware.json :refer [wrap-json-response]])

(defn handler [request]
  {:status 200
   :body {:user (:user (:params request))
          :method (name (:request-method request))}})

(def app
  (-> handler
      wrap-keyword-params
      wrap-params
      wrap-json-response))

(deftest test-middleware-stack
  (let [response (app (mock/request :get "/api/user" {:user "alice"}))
        body (json/parse-string (:body response) true)]
    (is (= 200 (:status response)))
    (is (= "alice" (:user body)))
    (is (= "get" (:method body)))))
```

### Custom Request Helpers

```clojure
(defn authenticated-request
  "Create a request with authentication header"
  [method uri token]
  (-> (mock/request method uri)
      (mock/header "Authorization" (str "Bearer " token))))

(defn json-request
  "Create a request with JSON body"
  [method uri data]
  (-> (mock/request method uri)
      (mock/json-body data)))

(deftest test-with-helpers
  (testing "authenticated API call"
    (let [response (app (authenticated-request :get "/api/profile" "token123"))]
      (is (= 200 (:status response))))))
```

## Resources

- [Ring Mock GitHub](https://github.com/ring-clojure/ring-mock)
- [Ring Documentation](https://github.com/ring-clojure/ring)
- [Ring Spec](https://github.com/ring-clojure/ring/blob/master/SPEC.md)
- [API Documentation](https://ring-clojure.github.io/ring-mock/)

## Summary

ring-mock provides fast, isolated testing for Ring web applications:

1. **Mock requests** - Create Ring-compliant request maps without HTTP servers
2. **Composable builders** - Use threading macros to build complex requests
3. **Helper functions** - Convenient functions for headers, bodies, cookies
4. **JSON support** - Built-in `json-body` helper for API testing
5. **Middleware testing** - Test middleware behavior in isolation
6. **Fast tests** - No network overhead, instant feedback

Use ring-mock for unit testing handlers and middleware, ensuring your web application logic is correct before integration testing with real HTTP clients.
