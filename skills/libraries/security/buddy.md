---
name: buddy-security
description: |
  Security and cryptography library for Clojure. Use when hashing passwords, creating/verifying 
  JWTs, implementing authentication, signing/encrypting data, generating cryptographic hashes, 
  or when the user mentions password hashing, JWT, JWS, JWE, bcrypt, scrypt, pbkdf2, digital 
  signatures, message authentication codes (MAC), HMAC, SHA, encryption, or security. Buddy 
  provides password hashers, JSON Web Tokens, cryptographic hash functions, message signing, 
  and authentication/authorization for Ring applications.
---

# Buddy - Security Library for Clojure

## Quick Start

Buddy is a comprehensive security library providing password hashing, JWT support, cryptographic operations, and authentication for Clojure applications.

```clojure
;; Password Hashing
(require '[buddy.hashers :as hashers])

(def hashed (hashers/derive "my-password"))
;; => "bcrypt+sha512$...$12$..."

(hashers/verify "my-password" hashed)
;; => {:valid true, :update false}

;; JWT Tokens
(require '[buddy.sign.jwt :as jwt])

(def token (jwt/sign {:user "alice" :role "admin"} "secret-key"))
;; => "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyIjoiYWxpY2UiLCJyb2xlIjoiYWRtaW4ifQ...."

(jwt/unsign token "secret-key")
;; => {:user "alice", :role "admin"}

;; Cryptographic Hashing
(require '[buddy.core.hash :as hash]
         '[buddy.core.codecs :as codecs])

(-> "hello world" hash/sha256 codecs/bytes->hex)
;; => "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"
```

**Key benefits:**
- **Comprehensive** - Covers all common security needs in one library
- **Secure defaults** - Uses strong algorithms by default
- **Flexible** - Multiple algorithms and configuration options
- **Well-tested** - Battle-tested in production applications
- **Ring integration** - Built-in middleware for web applications

## Core Concepts

### Buddy Modules

Buddy is split into four modules (but can be used as one monolithic package):

1. **buddy-core** - Low-level cryptographic operations
   - Hash functions (SHA, BLAKE2, Skein, etc.)
   - Message Authentication Codes (MAC/HMAC)
   - Digital signatures
   - Encryption/decryption
   - Key derivation functions

2. **buddy-hashers** - Password hashing
   - bcrypt, scrypt, pbkdf2
   - Argon2 support
   - Automatic salt generation
   - Password verification

3. **buddy-sign** - Message signing and encryption
   - JSON Web Tokens (JWT)
   - JSON Web Signature (JWS)
   - JSON Web Encryption (JWE)
   - Compact message signing

4. **buddy-auth** - Authentication/authorization for Ring
   - HTTP Basic Auth
   - Token-based auth
   - Session auth
   - Access rules

### Security Principles

- **Never store passwords in plain text** - Always hash them
- **Use secure random salts** - Buddy handles this automatically
- **Validate signatures** - Always verify JWTs before trusting
- **Use appropriate algorithms** - bcrypt/scrypt for passwords, HS256/RS256 for JWTs
- **Protect secrets** - Never commit keys or secrets to version control

## Common Workflows

### Workflow 1: Password Hashing and Verification

Hash passwords for storage and verify user login attempts:

```clojure
(require '[buddy.hashers :as hashers])

;; Register new user - hash their password
(defn create-user [username password]
  {:username username
   :password-hash (hashers/derive password)  ; Uses bcrypt+sha512 by default
   :created-at (java.util.Date.)})

(def alice (create-user "alice" "secret123"))
;; => {:username "alice"
;;     :password-hash "bcrypt+sha512$760f8e048409dee21eb5852647977ee5$12$..."
;;     :created-at #inst "2025-01-10..."}

;; Login - verify password
(defn authenticate [username password user-db]
  (when-let [user (get user-db username)]
    (let [result (hashers/verify password (:password-hash user))]
      (when (:valid result)
        (if (:update result)
          ;; Password algorithm outdated, rehash it
          (do
            (println "Updating password hash for" username)
            (assoc user :password-hash (hashers/derive password)))
          ;; Password OK, return user
          user)))))

;; Test authentication
(authenticate "alice" "secret123" {"alice" alice})
;; => {:username "alice", :password-hash "...", :created-at ...}

(authenticate "alice" "wrong-password" {"alice" alice})
;; => nil
```

### Workflow 2: Choosing Password Hashing Algorithms

Different algorithms for different needs:

```clojure
;; bcrypt+sha512 (default) - Good balance of security and performance
(hashers/derive "password")
;; => "bcrypt+sha512$...$12$..."

;; bcrypt - Industry standard, good for most cases
(hashers/derive "password" {:alg :bcrypt})
;; => "bcrypt$...$12$..."

;; scrypt - Memory-hard, better against hardware attacks
(hashers/derive "password" {:alg :scrypt})
;; => "scrypt$...$65536$8$1$..."

;; pbkdf2+sha256 - Good for compliance requirements (FIPS)
(hashers/derive "password" {:alg :pbkdf2+sha256})
;; => "pbkdf2+sha256$...$100000$..."

;; Custom iterations (more = slower = more secure)
(hashers/derive "password" {:alg :bcrypt :iterations 14})
;; => "bcrypt$...$14$..."  ; Takes ~4x longer than 12
```

**Algorithm comparison:**

| Algorithm | Security | Speed | Memory | Best For |
|-----------|----------|-------|---------|----------|
| bcrypt+sha512 | Excellent | Good | Low | General use (default) |
| bcrypt | Excellent | Good | Low | General use |
| scrypt | Excellent | Slow | High | High-security, prevent GPU attacks |
| pbkdf2+sha256 | Good | Fast | Low | Compliance (FIPS) |

### Workflow 3: Creating and Verifying JWTs

Use JWTs for stateless authentication:

```clojure
(require '[buddy.sign.jwt :as jwt]
         '[clojure.pprint :refer [pprint]])

;; Create JWT token for authenticated user
(defn create-auth-token [user]
  (let [claims {:user (:username user)
                :roles (:roles user)
                :iat (quot (System/currentTimeMillis) 1000)  ; Issued at
                :exp (+ (quot (System/currentTimeMillis) 1000) 3600)}] ; Expires in 1 hour
    (jwt/sign claims "my-secret-key")))

(def token (create-auth-token {:username "alice" :roles ["user" "admin"]}))
;; => "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyIjoiYWxpY2UiLCJyb2xlcyI6WyJ1c2VyIiwiYWRtaW4iXSwiaWF0IjoxNzMxMjQwMDAwLCJleHAiOjE3MzEyNDM2MDB9...."

;; Verify and decode token
(defn verify-token [token secret]
  (try
    (jwt/unsign token secret)
    (catch Exception e
      (println "Invalid token:" (.getMessage e))
      nil)))

(verify-token token "my-secret-key")
;; => {:user "alice", :roles ["user" "admin"], :iat 1731240000, :exp 1731243600}

(verify-token token "wrong-secret")
;; => nil

;; Decode without verification (for debugging only!)
(jwt/decode-header token)
;; => {:alg "HS256"}
```

### Workflow 4: JWT with RSA Keys (Asymmetric)

Use RSA for distributed systems where multiple services verify tokens:

```clojure
(require '[buddy.core.keys :as keys])

;; Generate RSA key pair (in production, load from files)
(def privkey (keys/private-key "path/to/private-key.pem"))
(def pubkey (keys/public-key "path/to/public-key.pem"))

;; Or use string keys
(def rsa-priv-key "-----BEGIN PRIVATE KEY-----\nMIIEvQ...")
(def rsa-pub-key "-----BEGIN PUBLIC KEY-----\nMIIBIj...")

(def privkey (keys/str->private-key rsa-priv-key))
(def pubkey (keys/str->public-key rsa-pub-key))

;; Sign with private key (only auth service has this)
(def token (jwt/sign {:user "alice"} privkey {:alg :rs256}))

;; Verify with public key (all services can have this)
(jwt/unsign token pubkey {:alg :rs256})
;; => {:user "alice"}
```

**When to use symmetric vs asymmetric:**

- **Symmetric (HS256)** - Single service or trusted services sharing secret
- **Asymmetric (RS256)** - Multiple services where only auth service signs tokens

### Workflow 5: Cryptographic Hash Functions

Generate cryptographic hashes for data integrity:

```clojure
(require '[buddy.core.hash :as hash]
         '[buddy.core.codecs :as codecs])

;; SHA-256 (most common)
(defn hash-file-content [content]
  (-> content hash/sha256 codecs/bytes->hex))

(hash-file-content "file contents here")
;; => "a3d2f1b89c4e5d6..."

;; Different hash algorithms
(-> "hello" hash/sha256 codecs/bytes->hex)    ; SHA-256
(-> "hello" hash/sha512 codecs/bytes->hex)    ; SHA-512
(-> "hello" hash/blake2b-256 codecs/bytes->hex) ; BLAKE2b (faster)
(-> "hello" hash/sha3-256 codecs/bytes->hex)  ; SHA-3

;; Hash comparison (constant-time to prevent timing attacks)
(require '[buddy.core.codecs :as codecs])

(defn hashes-match? [hash1 hash2]
  (codecs/bytes= hash1 hash2))

(hashes-match? (hash/sha256 "hello") (hash/sha256 "hello"))
;; => true
```

**Algorithm selection:**
- **SHA-256** - Industry standard, use for general purposes
- **SHA-512** - More secure, use for high-security needs
- **BLAKE2b** - Faster than SHA, use for performance-critical hashing
- **SHA-3** - Latest standard, use for future-proofing

### Workflow 6: Message Authentication Codes (MAC)

Verify message authenticity and integrity:

```clojure
(require '[buddy.core.mac :as mac]
         '[buddy.core.codecs :as codecs])

;; Generate MAC for message
(defn sign-message [message secret]
  (let [mac-bytes (mac/hash message {:key secret :alg :hmac+sha256})]
    {:message message
     :signature (codecs/bytes->hex mac-bytes)}))

(def signed (sign-message "transfer $100 to alice" "secret-key"))
;; => {:message "transfer $100 to alice"
;;     :signature "8f4e7c2a1b9d3f5e..."}

;; Verify message MAC
(defn verify-message [signed secret]
  (let [expected-mac (mac/hash (:message signed) 
                               {:key secret :alg :hmac+sha256})
        actual-mac (codecs/hex->bytes (:signature signed))]
    (codecs/bytes= expected-mac actual-mac)))

(verify-message signed "secret-key")
;; => true (message authentic)

(verify-message (assoc signed :message "transfer $999 to eve") "secret-key")
;; => false (message tampered)
```

**Use MAC when:**
- Verifying API webhooks (e.g., GitHub, Stripe)
- Ensuring message integrity over untrusted channels
- Implementing HMAC authentication schemes

### Workflow 7: JWT Claims and Validation

Use standard JWT claims for automatic validation:

```clojure
;; Standard JWT claims
(def claims
  {:sub "user-id-123"                    ; Subject (user ID)
   :iss "https://myapp.com"             ; Issuer
   :aud "https://api.myapp.com"         ; Audience
   :iat (quot (System/currentTimeMillis) 1000)  ; Issued at
   :exp (+ (quot (System/currentTimeMillis) 1000) 3600)  ; Expires (1 hour)
   :nbf (quot (System/currentTimeMillis) 1000)  ; Not before
   :jti (str (java.util.UUID/randomUUID))  ; JWT ID (unique)
   
   ;; Custom claims
   :user "alice"
   :roles ["admin" "user"]})

(def token (jwt/sign claims "secret"))

;; Automatic validation on unsign
(try
  (jwt/unsign token "secret")  ; Validates exp, nbf automatically
  (catch Exception e
    (println "Token validation failed:" (.getMessage e))))

;; Manual validation with options
(jwt/unsign token "secret" {:skip-validation true})  ; Skip validation (dangerous!)

;; Check if token expired
(defn token-expired? [token]
  (try
    (jwt/unsign token "secret")
    false
    (catch clojure.lang.ExceptionInfo e
      (= :exp (:cause (ex-data e))))))
```

**Standard claims:**
- **:exp** - Expiration time (Unix timestamp) - automatically validated
- **:nbf** - Not before (Unix timestamp) - automatically validated
- **:iat** - Issued at (Unix timestamp) - for tracking
- **:iss** - Issuer (string) - who created the token
- **:sub** - Subject (string) - who token is about (user ID)
- **:aud** - Audience (string) - who token is for
- **:jti** - JWT ID (string) - unique token identifier

### Workflow 8: Secure Token Storage and Transmission

Best practices for handling tokens:

```clojure
;; Store tokens securely
(defn store-token-secure [user-id token]
  ;; DON'T store tokens in localStorage (XSS vulnerable)
  ;; DO store in httpOnly cookies or secure session storage
  {:set-cookie (str "auth-token=" token 
                    "; HttpOnly"        ; Prevent JavaScript access
                    "; Secure"          ; HTTPS only
                    "; SameSite=Strict" ; CSRF protection
                    "; Max-Age=3600")}) ; 1 hour expiry

;; Refresh token pattern
(defn create-token-pair [user]
  (let [access-token (jwt/sign {:user (:id user) 
                                :exp (+ (System/currentTimeMillis) 900000)}  ; 15 min
                               "access-secret")
        refresh-token (jwt/sign {:user (:id user)
                                 :exp (+ (System/currentTimeMillis) 2592000000)} ; 30 days
                                "refresh-secret")]
    {:access-token access-token
     :refresh-token refresh-token}))

;; Use short-lived access tokens, long-lived refresh tokens
(defn refresh-access-token [refresh-token]
  (when-let [claims (jwt/unsign refresh-token "refresh-secret")]
    (jwt/sign (select-keys claims [:user]) "access-secret")))
```

## When to Use Each Buddy Module

### Use buddy-hashers when:
- Storing user passwords
- Implementing user registration/login
- Need slow, secure password hashing
- Want automatic password hash migration

### Use buddy-sign when:
- Implementing JWT authentication
- Creating API tokens
- Signing/verifying messages
- Need stateless authentication

### Use buddy-core when:
- Computing file checksums
- Implementing custom cryptographic protocols
- Need low-level crypto operations
- Building secure APIs with HMAC

### Use buddy-auth when:
- Building Ring web applications
- Need authentication middleware
- Implementing access control rules
- Want ready-made auth backends

## Best Practices

**Do:**
- Use `bcrypt+sha512` (default) or `scrypt` for password hashing
- Set JWT expiration times (`:exp` claim)
- Use HTTPS for transmitting tokens
- Store tokens in httpOnly cookies when possible
- Rotate secrets periodically
- Use RSA (RS256) for distributed systems
- Validate JWTs on every request
- Use constant-time comparison for secrets
- Add custom claims to JWTs (roles, permissions)

```clojure
;; Good: JWT with expiration
(jwt/sign {:user "alice" 
           :exp (+ (System/currentTimeMillis) 3600000)}
          "secret")

;; Good: Secure password hashing
(hashers/derive "password" {:alg :bcrypt :iterations 12})

;; Good: Constant-time comparison
(codecs/bytes= hash1 hash2)
```

**Don't:**
- Store passwords in plain text
- Use weak secrets ("password", "secret", "123456")
- Skip JWT validation
- Store sensitive data in JWT (it's not encrypted!)
- Use MD5 or SHA1 for security (they're broken)
- Forget to set token expiration
- Use the same secret for everything
- Store tokens in localStorage (XSS risk)

```clojure
;; Bad: No expiration
(jwt/sign {:user "alice"} "secret")

;; Bad: Sensitive data in JWT (JWTs are just base64 encoded!)
(jwt/sign {:user "alice" :ssn "123-45-6789"} "secret")

;; Bad: Skip validation (dangerous!)
(jwt/unsign token "secret" {:skip-validation true})
```

## Common Issues

### Issue: Password verification fails after algorithm change

```clojure
(def old-hash (hashers/derive "password" {:alg :bcrypt}))
;; Change default algorithm in config...
(hashers/verify "password" old-hash {:alg :scrypt})
;; => {:valid false ...}  ; Wrong algorithm!
```

**Solution**: Buddy automatically detects algorithm from hash string:

```clojure
(hashers/verify "password" old-hash)
;; => {:valid true, :update true}  ; Suggests updating hash

;; Handle update suggestion
(let [result (hashers/verify password stored-hash)]
  (when (:update result)
    ;; Rehash with new algorithm
    (update-user-password! user (hashers/derive password))))
```

### Issue: JWT expired error

```clojure
(jwt/unsign old-token "secret")
;; => ExceptionInfo: Token is expired
```

**Solution**: Check expiration before unsigning or handle exception:

```clojure
(defn verify-token-safe [token secret]
  (try
    {:valid true :claims (jwt/unsign token secret)}
    (catch clojure.lang.ExceptionInfo e
      (let [cause (:cause (ex-data e))]
        (case cause
          :exp {:valid false :reason "expired"}
          :signature {:valid false :reason "invalid"}
          {:valid false :reason "unknown"})))))

(verify-token-safe expired-token "secret")
;; => {:valid false :reason "expired"}
```

### Issue: JWT signature verification fails

```clojure
(jwt/unsign token "wrong-secret")
;; => ExceptionInfo: Message seems corrupt or manipulated
```

**Solution**: Ensure using correct secret and algorithm:

```clojure
;; Verify algorithm matches
(jwt/decode-header token)
;; => {:alg "HS256"}

;; Use correct secret for that token
(jwt/unsign token correct-secret)
```

### Issue: Hash comparison vulnerable to timing attacks

```clojure
;; Bad: Standard comparison leaks timing information
(= stored-hash computed-hash)
```

**Solution**: Use constant-time comparison:

```clojure
(require '[buddy.core.codecs :as codecs])

;; Good: Constant-time comparison
(codecs/bytes= stored-hash computed-hash)
```

### Issue: JWT too large for headers

```clojure
(def huge-claims {:user "alice" :data (range 10000)})
(def token (jwt/sign huge-claims "secret"))
;; => Very long token (>8KB), may exceed header limits
```

**Solution**: Store large data server-side, reference by ID in JWT:

```clojure
;; Good: Store data, reference in token
(def session-id (save-session-data! {:data (range 10000)}))
(def token (jwt/sign {:user "alice" :session session-id} "secret"))
```

### Issue: Hashing performance too slow

```clojure
;; Takes ~200ms per password
(time (hashers/derive "password" {:alg :bcrypt :iterations 14}))
```

**Solution**: Reduce iterations (but maintain security):

```clojure
;; Faster but still secure
(hashers/derive "password" {:alg :bcrypt :iterations 12})  ; ~50ms

;; Or use pbkdf2 for faster performance
(hashers/derive "password" {:alg :pbkdf2+sha256})  ; ~20ms
```

## Advanced Topics

### Custom JWT Claims Validation

```clojure
(defn validate-custom-claims [claims]
  (and (contains? claims :roles)
       (some #{"admin" "user"} (:roles claims))
       (< (System/currentTimeMillis) (:exp claims))))

(let [claims (jwt/unsign token "secret")]
  (when (validate-custom-claims claims)
    (do-authorized-action claims)))
```

### JWE (Encrypted JWTs)

```clojure
(require '[buddy.sign.jwe :as jwe])

;; Encrypt sensitive data in JWT
(def encrypted-token
  (jwe/encrypt {:user "alice" :ssn "123-45-6789"} "32-byte-secret-key-here-exactly"))

;; Decrypt
(jwe/decrypt encrypted-token "32-byte-secret-key-here-exactly")
;; => {:user "alice", :ssn "123-45-6789"}
```

### Key Management

```clojure
(require '[buddy.core.keys :as keys])

;; Load keys from files
(def privkey (keys/private-key "resources/privkey.pem"))
(def pubkey (keys/public-key "resources/pubkey.pem"))

;; Generate key pair programmatically
(def keypair (keys/generate-key-pair :rsa {:key-size 2048}))
(def privkey (:private keypair))
(def pubkey (:public keypair))
```

### Password Strength Requirements

```clojure
(defn password-strong? [password]
  (and (>= (count password) 12)
       (re-find #"[a-z]" password)
       (re-find #"[A-Z]" password)
       (re-find #"[0-9]" password)
       (re-find #"[!@#$%^&*]" password)))

(defn register-user [username password]
  (if (password-strong? password)
    {:username username
     :password-hash (hashers/derive password)}
    {:error "Password must be at least 12 characters with uppercase, lowercase, number, and special character"}))
```

## Related Libraries

- **ring-session-memory** - Session storage for Ring
- **ring-middleware-oauth2** - OAuth2 integration
- **clj-jwt** - Alternative JWT library
- **caesium** - Libsodium bindings for Clojure
- **tink** - Google's crypto library bindings

## Resources

- Main docs: https://funcool.github.io/buddy-core/latest/
- buddy-core: https://github.com/funcool/buddy-core
- buddy-sign: https://github.com/funcool/buddy-sign
- buddy-hashers: https://github.com/funcool/buddy-hashers
- buddy-auth: https://github.com/funcool/buddy-auth
- JWT spec: https://jwt.io/
- Password hashing guide: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html

## Summary

Buddy provides comprehensive security for Clojure applications:

1. **Password Hashing** - bcrypt, scrypt, pbkdf2, automatic salting
2. **JWT Support** - Sign, verify, encrypt tokens with standard claims
3. **Cryptographic Hashing** - SHA-256/512, BLAKE2b, SHA-3, etc.
4. **Message Authentication** - HMAC for verifying message integrity
5. **Digital Signatures** - RSA, ECDSA for asymmetric authentication
6. **Ring Integration** - Ready-made middleware for web apps

**Most common patterns:**

```clojure
;; Password hashing
(hashers/derive "password")
(hashers/verify "password" hash)

;; JWT authentication
(jwt/sign {:user "alice"} "secret")
(jwt/unsign token "secret")

;; Cryptographic hashing
(-> data hash/sha256 codecs/bytes->hex)

;; Message authentication
(mac/hash message {:key "secret" :alg :hmac+sha256})
```

Essential for building secure web applications, APIs, and authentication systems in Clojure.
