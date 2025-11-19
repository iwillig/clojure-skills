---
name: data-xml
description: |
  Parse and emit XML documents using Clojure data structures. Use when reading XML files,
  generating XML documents, working with XML APIs, handling RSS/Atom feeds, SOAP services,
  or configuration files. Supports XML namespaces, streaming, lazy parsing, CDATA, comments,
  and QNames. Ideal for XML processing, data transformation, and web services integration.
---

# clojure.data.xml - XML Processing for Clojure

## Quick Start

clojure.data.xml provides idiomatic XML parsing and emission using Clojure data structures.

```clojure
(require '[clojure.data.xml :as xml])

;; Parse XML string
(xml/parse-str "<root><child>value</child></root>")
; => #xml/element{:tag :root, :content [#xml/element{:tag :child, :content ["value"]}]}

;; Create XML elements
(xml/element :root {}
  (xml/element :child {} "value"))
; => #xml/element{:tag :root, :content [#xml/element{:tag :child, :content ["value"]}]}

;; Emit XML string
(xml/emit-str (xml/element :root {} (xml/element :child {} "value")))
; => "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><child>value</child></root>"

;; Hiccup-style construction
(xml/sexp-as-element [:root [:child "value"]])
; => #xml/element{:tag :root, :content [#xml/element{:tag :child, :content ["value"]}]}
```

**Key benefits:**
- Parse and emit XML using Clojure data structures
- Lazy, streaming parsing for large XML files
- Full XML namespace support with keyword encoding
- CDATA and comment support
- StAX-based for performance
- ClojureScript support with DOM integration

## Core Concepts

### Elements as Data

XML elements are represented as Clojure records with three keys:

```clojure
;; Element structure
{:tag :element-name        ; Keyword or QName
 :attrs {:attr "value"}    ; Map of attributes
 :content [child-nodes]}   ; Vector of content (strings, elements, etc.)

;; Example
(xml/element :person {:id "123"}
  (xml/element :name {} "Alice")
  (xml/element :age {} "30"))
; => #xml/element{:tag :person, 
;                 :attrs {:id "123"},
;                 :content [#xml/element{:tag :name, :content ["Alice"]}
;                           #xml/element{:tag :age, :content ["30"]}]}
```

### Parsing vs Emitting

**Parsing**: XML text → Clojure data structures
- `parse` - Parse from InputStream or Reader
- `parse-str` - Parse from string
- Returns lazy tree of Element records

**Emitting**: Clojure data structures → XML text
- `emit` - Write to Writer or OutputStream
- `emit-str` - Return as string
- `indent` / `indent-str` - Pretty-printed output

### Element Construction

Three ways to create elements:

```clojure
;; 1. element function (most explicit)
(xml/element :root {:version "1.0"}
  (xml/element :child {} "content"))

;; 2. sexp-as-element (Hiccup-style, concise)
(xml/sexp-as-element
  [:root {:version "1.0"}
   [:child {} "content"]])

;; 3. Plain maps (most flexible)
{:tag :root
 :attrs {:version "1.0"}
 :content [{:tag :child
            :content ["content"]}]}
```

All three produce equivalent Element records and can be used interchangeably.

## Common Workflows

### Workflow 1: Parsing XML Documents

Parse XML from various sources:

```clojure
(require '[clojure.data.xml :as xml])

;; Parse from string
(def doc (xml/parse-str "<?xml version=\"1.0\"?>
                         <catalog>
                           <book id=\"1\">
                             <title>Clojure Programming</title>
                             <author>O'Reilly</author>
                           </book>
                         </catalog>"))

;; Access parsed data
(:tag doc)           ; => :catalog
(:content doc)       ; => [#xml/element{:tag :book, ...}]
(-> doc :content first :attrs :id)  ; => "1"

;; Parse from file
(with-open [in (clojure.java.io/input-stream "catalog.xml")]
  (xml/parse in))

;; Parse from URL
(with-open [in (clojure.java.io/input-stream "https://example.com/feed.xml")]
  (xml/parse in))

;; Parse with options
(xml/parse-str "<root>text</root>" 
  :coalescing false           ; Don't merge adjacent text nodes
  :location-info false)       ; Don't add location metadata
```

### Workflow 2: Building XML Documents

Create XML using element construction:

```clojure
;; Using element function
(def catalog
  (xml/element :catalog {:version "1.0"}
    (xml/element :book {:id "1"}
      (xml/element :title {} "Clojure Programming")
      (xml/element :author {} "O'Reilly"))
    (xml/element :book {:id "2"}
      (xml/element :title {} "Joy of Clojure")
      (xml/element :author {} "Manning"))))

;; Using sexp-as-element (more concise)
(def catalog
  (xml/sexp-as-element
    [:catalog {:version "1.0"}
     [:book {:id "1"}
      [:title "Clojure Programming"]
      [:author "O'Reilly"]]
     [:book {:id "2"}
      [:title "Joy of Clojure"]
      [:author "Manning"]]]))

;; Build programmatically
(defn make-book [id title author]
  (xml/element :book {:id (str id)}
    (xml/element :title {} title)
    (xml/element :author {} author)))

(def catalog
  (xml/element :catalog {}
    (make-book 1 "Clojure Programming" "O'Reilly")
    (make-book 2 "Joy of Clojure" "Manning")))

;; Generate from data
(def books [{:id 1 :title "Book 1" :author "Author 1"}
            {:id 2 :title "Book 2" :author "Author 2"}])

(def catalog
  (xml/element :catalog {}
    (map (fn [{:keys [id title author]}]
           (make-book id title author))
         books)))
```

### Workflow 3: Emitting XML

Write XML to various destinations:

```clojure
;; Emit to string
(xml/emit-str catalog)
; => "<?xml version=\"1.0\" encoding=\"UTF-8\"?><catalog version=\"1.0\">..."

;; Emit to file
(with-open [out (clojure.java.io/writer "catalog.xml")]
  (xml/emit catalog out))

;; Pretty-printed output (for debugging/readability)
(println (xml/indent-str catalog))
; <?xml version="1.0" encoding="UTF-8"?>
; <catalog version="1.0">
;   <book id="1">
;     <title>Clojure Programming</title>
;     <author>O'Reilly</author>
;   </book>
; </catalog>

;; Emit with options
(with-open [out (clojure.java.io/writer "catalog.xml")]
  (xml/emit catalog out
    :encoding "ISO-8859-1"
    :doctype "<!DOCTYPE catalog SYSTEM \"catalog.dtd\">"))
```

### Workflow 4: XML Transformation

Transform XML documents using Clojure data manipulation:

```clojure
(require '[clojure.zip :as zip])
(require '[clojure.data.xml :as xml])

;; Parse XML
(def doc (xml/parse-str 
  "<catalog>
     <book id=\"1\" price=\"29.99\">
       <title>Clojure Programming</title>
     </book>
     <book id=\"2\" price=\"39.99\">
       <title>Joy of Clojure</title>
     </book>
   </catalog>"))

;; Extract specific data
(defn extract-titles [catalog-elem]
  (->> (:content catalog-elem)
       (filter #(= :book (:tag %)))
       (map #(first (:content (first (filter (fn [e] (= :title (:tag e))) 
                                              (:content %))))))
       (filter string?)))

(extract-titles doc)
; => ("Clojure Programming" "Joy of Clojure")

;; Transform attributes
(defn increase-prices [catalog-elem percent]
  (update catalog-elem :content
    (fn [books]
      (map (fn [book]
             (if (= :book (:tag book))
               (update-in book [:attrs :price]
                 (fn [price]
                   (str (* (Double/parseDouble price) (+ 1 (/ percent 100.0))))))
               book))
           books))))

(xml/emit-str (increase-prices doc 10))
; Prices increased by 10%

;; Filter elements
(defn expensive-books [catalog-elem min-price]
  (update catalog-elem :content
    (fn [books]
      (filter (fn [book]
                (when (= :book (:tag book))
                  (> (Double/parseDouble (get-in book [:attrs :price]))
                     min-price)))
              books))))

(xml/emit-str (expensive-books doc 35.0))
; Only books over $35
```

### Workflow 5: Working with CDATA and Comments

Handle special XML node types:

```clojure
;; CDATA sections (unescaped text)
(def doc-with-cdata
  (xml/element :script {:type "text/javascript"}
    (xml/cdata "if (x < y && y > z) { alert('hello'); }")))

(xml/emit-str doc-with-cdata)
; => "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
;     <script type=\"text/javascript\"><![CDATA[if (x < y && y > z) { alert('hello'); }]]></script>"

;; Using sexp-as-element with CDATA
(xml/sexp-as-element
  [:script {:type "text/javascript"}
   [:-cdata "if (x < y && y > z) { alert('hello'); }"]])

;; Comments
(def doc-with-comments
  (xml/element :root {}
    (xml/xml-comment "This is a comment")
    (xml/element :data {} "value")))

(xml/emit-str doc-with-comments)
; => "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
;     <root><!--This is a comment--><data>value</data></root>"

;; Using sexp-as-element with comments
(xml/sexp-as-element
  [:root
   [:-comment "This is a comment"]
   [:data "value"]])

;; Note: Comments are ignored when parsing
(xml/parse-str "<root><!-- comment --><data>value</data></root>")
; => #xml/element{:tag :root, :content [#xml/element{:tag :data, :content ["value"]}]}
; Comment is not in the result
```

### Workflow 6: XML Namespaces

Work with XML namespaces using qualified names:

```clojure
;; Define namespace alias (must be at top level)
(xml/alias-uri 'xh "http://www.w3.org/1999/xhtml")

;; Create namespaced elements
(def xhtml-doc
  (xml/element ::xh/html {}
    (xml/element ::xh/head {}
      (xml/element ::xh/title {} "Page Title"))
    (xml/element ::xh/body {}
      (xml/element ::xh/p {} "Content"))))

(xml/emit-str xhtml-doc)
; => "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
;     <a:html xmlns:a=\"http://www.w3.org/1999/xhtml\">
;       <a:head><a:title>Page Title</a:title></a:head>
;       <a:body><a:p>Content</a:p></a:body>
;     </a:html>"

;; Use default namespace (no prefix)
(def xhtml-doc-no-prefix
  (xml/element ::xh/html {:xmlns "http://www.w3.org/1999/xhtml"}
    (xml/element ::xh/head {}
      (xml/element ::xh/title {} "Page Title"))
    (xml/element ::xh/body {}
      (xml/element ::xh/p {} "Content"))))

(xml/emit-str xhtml-doc-no-prefix)
; => "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
;     <html xmlns=\"http://www.w3.org/1999/xhtml\">
;       <head><title>Page Title</title></head>
;       <body><p>Content</p></body>
;     </html>"

;; Custom prefix
(xml/emit-str
  (xml/element (xml/qname "http://www.w3.org/1999/xhtml" "html")
               {:xmlns/myprefix "http://www.w3.org/1999/xhtml"}
               "content"))
; => "<myprefix:html xmlns:myprefix=\"http://www.w3.org/1999/xhtml\">content</myprefix:html>"

;; Parse namespaced XML
(def parsed-ns-doc
  (xml/parse-str "<foo:html xmlns:foo=\"http://www.w3.org/1999/xhtml\">
                    <foo:body>content</foo:body>
                  </foo:html>"))

(:tag parsed-ns-doc)
; => :xmlns.http%3A%2F%2Fwww.w3.org%2F1999%2Fxhtml/html
; Namespace URI is percent-encoded in the keyword

;; Multiple namespaces
(xml/alias-uri 'xh "http://www.w3.org/1999/xhtml")
(xml/alias-uri 'svg "http://www.w3.org/2000/svg")

(xml/sexp-as-element
  [::xh/html {}
   [::xh/body {}
    [::svg/svg {:width "100" :height "100"}
     [::svg/circle {:cx "50" :cy "50" :r "40"}]]]])
```

### Workflow 7: Streaming Large XML Files

Process large XML files efficiently with lazy parsing:

```clojure
;; event-seq provides low-level streaming access
(require '[clojure.java.io :as io])

;; Process events lazily
(with-open [input (io/input-stream "large-file.xml")]
  (doseq [event (xml/event-seq (xml/parse input)
                  :include-node? #{:element :characters})]
    (when (= :element (:type event))
      (println "Element:" (:tag (:node event))))))

;; Extract specific elements from large document
(defn extract-books [xml-file]
  (with-open [input (io/input-stream xml-file)]
    (let [events (xml/event-seq (xml/parse input))]
      (->> events
           (filter #(and (= :element (:type %))
                        (= :book (:tag (:node %)))))
           (map :node)
           doall))))

;; Process in chunks
(defn process-in-batches [xml-file batch-size]
  (with-open [input (io/input-stream xml-file)]
    (let [elements (->> (xml/parse input)
                       (xml/event-seq)
                       (filter #(= :element (:type %)))
                       (map :node))]
      (doseq [batch (partition-all batch-size elements)]
        (process-batch batch)))))
```

### Workflow 8: Real-World Examples

Common XML processing patterns:

```clojure
;; RSS Feed Generation
(defn generate-rss-feed [entries]
  (xml/element :rss {:version "2.0"}
    (xml/element :channel {}
      (xml/element :title {} "My Blog")
      (xml/element :link {} "https://example.com")
      (xml/element :description {} "A blog about Clojure")
      (map (fn [{:keys [title link description pubDate]}]
             (xml/element :item {}
               (xml/element :title {} title)
               (xml/element :link {} link)
               (xml/element :description {} description)
               (xml/element :pubDate {} pubDate)))
           entries))))

(def feed-entries
  [{:title "First Post"
    :link "https://example.com/first"
    :description "Introduction to Clojure"
    :pubDate "Mon, 01 Jan 2024 00:00:00 GMT"}])

(xml/indent-str (generate-rss-feed feed-entries))

;; SOAP Request/Response
(defn create-soap-request [operation params]
  (xml/alias-uri 'soap "http://schemas.xmlsoap.org/soap/envelope/")
  (xml/element ::soap/Envelope 
    {:xmlns "http://schemas.xmlsoap.org/soap/envelope/"}
    (xml/element ::soap/Body {}
      (xml/element (keyword operation) {}
        (map (fn [[k v]]
               (xml/element k {} (str v)))
             params)))))

(xml/emit-str 
  (create-soap-request "GetUser" {:userId "123"}))

;; Configuration Files (web.xml, pom.xml style)
(defn create-webapp-config [config]
  (xml/element :web-app {:version "3.0"}
    (xml/element :display-name {} (:app-name config))
    (map (fn [{:keys [param-name param-value]}]
           (xml/element :context-param {}
             (xml/element :param-name {} param-name)
             (xml/element :param-value {} param-value)))
         (:context-params config))
    (map (fn [{:keys [servlet-name servlet-class]}]
           (xml/element :servlet {}
             (xml/element :servlet-name {} servlet-name)
             (xml/element :servlet-class {} servlet-class)))
         (:servlets config))))
```

## When to Use Each Approach

**Use `element` function when:**
- You need explicit control over element creation
- Building XML programmatically from data
- Element structure is dynamic
- You want clear, self-documenting code

**Use `sexp-as-element` when:**
- XML structure is static or mostly static
- You want concise, readable code
- You're familiar with Hiccup-style syntax
- Quickly prototyping XML structures

**Use plain maps when:**
- Maximum flexibility is needed
- Integrating with other code that uses maps
- Programmatic manipulation of structure before creation
- Working with generic data transformation

**Use `emit-str` when:**
- Need XML as a string (API responses, tests)
- Small to medium documents
- In-memory processing

**Use `emit` with streams when:**
- Writing large XML files
- Memory efficiency is important
- Streaming to network or file system

**Use `indent-str` when:**
- Debugging XML output
- Human-readable output needed
- Not concerned about performance (indentation is slow)

## Best Practices

**DO:**
- Use `parse` with streams for large files (memory efficient)
- Use `:location-info false` when parsing large files to save memory
- Use `alias-uri` at namespace level for namespace prefixes
- Test XML round-trips: `(= doc (parse-str (emit-str doc)))`
- Use `sexp-as-element` for static, declarative XML structures
- Use CDATA for embedding code or markup
- Validate XML structure with your domain logic after parsing
- Handle parse exceptions with try/catch
- Close streams properly with `with-open`

**DON'T:**
- Parse entire large files into memory - use streaming/event-seq
- Forget to handle malformed XML (wrap parse in try/catch)
- Use `indent-str` in production (slow, for debugging only)
- Ignore namespace URIs when working with namespaced XML
- Manually construct XML strings (always use emit functions)
- Mix parsing modes - stick to one approach per codebase
- Forget that comments are ignored when parsing
- Use `parse-str` for large documents (use `parse` with stream)

## Common Issues

### Issue: Parse Error - Unexpected EOF

**Problem:** Parsing fails with unexpected end of file error.

```clojure
(xml/parse-str "<root><child>")
; => ERROR: Unexpected EOF
```

**Solution:** Ensure XML is well-formed with closing tags.

```clojure
;; Fix malformed XML before parsing
(defn validate-xml [xml-str]
  (try
    (xml/parse-str xml-str)
    (catch Exception e
      (println "Invalid XML:" (.getMessage e))
      nil)))

(validate-xml "<root><child></child></root>")
; => Works

(validate-xml "<root><child>")
; => Invalid XML: Unexpected EOF
; => nil
```

### Issue: Namespace Prefixes Lost

**Problem:** Namespace prefixes change when round-tripping XML.

```clojure
(def original "<foo:element xmlns:foo=\"http://example.com/foo\" />")
(xml/emit-str (xml/parse-str original))
; => "<a:element xmlns:a=\"http://example.com/foo\" />"
; Prefix changed from 'foo' to 'a'
```

**Solution:** This is expected - prefixes are just aliases. Namespace URIs are preserved (what matters for XML correctness). To preserve exact prefixes, use metadata:

```clojure
;; Namespace URI is preserved (semantically equivalent)
(= (xml/parse-str "<foo:title xmlns:foo=\"http://example.com\">Text</foo:title>")
   (xml/parse-str "<bar:title xmlns:bar=\"http://example.com\">Text</bar:title>"))
; => true

;; To use specific prefix, declare it explicitly
(xml/emit-str
  (xml/element (xml/qname "http://example.com/foo" "element")
               {:xmlns/foo "http://example.com/foo"}
               "content"))
; => "<foo:element xmlns:foo=\"http://example.com/foo\">content</foo:element>"
```

### Issue: CDATA Content Becomes Plain Text

**Problem:** CDATA sections are parsed as regular strings.

```clojure
(def xml-with-cdata "<root><![CDATA[<unescaped>]]></root>")
(xml/parse-str xml-with-cdata)
; => #xml/element{:tag :root, :content ["<unescaped>"]}
; CDATA markers gone, just content remains
```

**Solution:** This is expected behavior - CDATA is a serialization detail. When parsing, CDATA content becomes regular text. When emitting, you can add CDATA explicitly:

```clojure
;; Parse treats CDATA and text equivalently
(= (xml/parse-str "<root><![CDATA[text]]></root>")
   (xml/parse-str "<root>text</root>"))
; => true

;; Add CDATA when emitting if needed
(xml/emit-str
  (xml/element :root {}
    (xml/cdata "<unescaped>")))
; => "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><![CDATA[<unescaped>]]></root>"
```

### Issue: Out of Memory with Large Files

**Problem:** Parsing large XML file causes OutOfMemoryError.

```clojure
(xml/parse-str (slurp "huge-file.xml"))
; => OutOfMemoryError: Java heap space
```

**Solution:** Use streaming with `parse` and event-seq instead of loading entire file:

```clojure
;; Don't slurp entire file
;; Bad:
(xml/parse-str (slurp "huge-file.xml"))

;; Good: Use streaming parse
(with-open [input (clojure.java.io/input-stream "huge-file.xml")]
  (let [doc (xml/parse input)]
    ;; Process lazily
    (doseq [event (xml/event-seq doc)]
      (process-event event))))

;; Or disable location-info to save memory
(with-open [input (clojure.java.io/input-stream "huge-file.xml")]
  (xml/parse input :location-info false))
```

### Issue: Attributes Not Appearing in Output

**Problem:** Attributes missing from emitted XML.

```clojure
(xml/emit-str (xml/element :root "value"))
; => "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>value</root>"
; Where did {:id "1"} go?
```

**Solution:** Attributes must be the second argument (an empty map if none):

```clojure
;; Wrong - "value" treated as attrs
(xml/element :root "value")  ; Wrong!

;; Right - explicit empty attrs map
(xml/element :root {} "value")

;; With actual attributes
(xml/element :root {:id "1"} "value")
; => "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root id=\"1\">value</root>"
```

### Issue: Special Characters Not Escaped

**Problem:** Concerned about special XML characters in content.

```clojure
(xml/emit-str (xml/element :root {} "5 < 10 & 20 > 15"))
; Will special chars break the XML?
```

**Solution:** data.xml automatically escapes special characters:

```clojure
(xml/emit-str (xml/element :root {} "5 < 10 & 20 > 15"))
; => "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>5 &lt; 10 &amp; 20 &gt; 15</root>"
; Automatically escaped!

;; Only use CDATA if you DON'T want escaping
(xml/emit-str (xml/element :root {} (xml/cdata "5 < 10 & 20 > 15")))
; => "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><![CDATA[5 < 10 & 20 > 15]]></root>"
```

### Issue: Cannot Modify Parsed XML

**Problem:** Trying to change parsed XML elements.

```clojure
(def doc (xml/parse-str "<root><child>old</child></root>"))
(assoc doc :tag :newroot)
; Works, but how to update nested content?
```

**Solution:** Use standard Clojure data manipulation functions:

```clojure
;; Update tag
(assoc doc :tag :newroot)

;; Update attributes
(assoc-in doc [:attrs :version] "2.0")

;; Update nested content
(update-in doc [:content 0 :content 0] (constantly "new"))

;; Or use helper function
(defn update-element-content [elem new-content]
  (assoc elem :content [new-content]))

;; Transform recursively
(clojure.walk/postwalk
  (fn [node]
    (if (and (map? node) (= :child (:tag node)))
      (assoc node :content ["modified"])
      node))
  doc)
```

## Advanced Topics

### Event-Based Parsing

For low-level control, use `event-seq` to process parse events:

```clojure
(require '[clojure.java.io :as io])

;; Event types
(with-open [input (io/input-stream "doc.xml")]
  (doseq [event (xml/event-seq (xml/parse input))]
    (case (:type event)
      :start-element (println "Start:" (:tag (:node event)))
      :end-element   (println "End:" (:tag (:node event)))
      :characters    (println "Text:" (:str event))
      :comment       (println "Comment:" (:str event))
      nil)))

;; Filter events
(defn extract-text [xml-source]
  (->> (xml/event-seq (xml/parse xml-source))
       (filter #(= :characters (:type %)))
       (map :str)
       (apply str)))
```

### Custom Element Types

Create specialized element constructor functions:

```clojure
(defn html-element [tag attrs & children]
  (xml/element (keyword "http://www.w3.org/1999/xhtml" (name tag))
               attrs
               children))

(defn svg-element [tag attrs & children]
  (xml/element (keyword "http://www.w3.org/2000/svg" (name tag))
               attrs
               children))

;; Usage
(html-element :div {:class "container"}
  (html-element :p {} "Hello World"))
```

### XML Schema Validation

While data.xml doesn't include validation, you can integrate with Java's XML Schema:

```clojure
(import '[javax.xml.validation SchemaFactory]
        '[javax.xml.transform.stream StreamSource]
        '[java.io StringReader])

(defn validate-against-schema [xml-str schema-file]
  (let [factory (SchemaFactory/newInstance 
                  javax.xml.XMLConstants/W3C_XML_SCHEMA_NS_URI)
        schema (.newSchema factory (java.io.File. schema-file))
        validator (.newValidator schema)]
    (try
      (.validate validator (StreamSource. (StringReader. xml-str)))
      true
      (catch Exception e
        (println "Validation error:" (.getMessage e))
        false))))
```

## Resources

- [Official Documentation](https://github.com/clojure/data.xml)
- [API Reference](https://clojure.github.io/data.xml/)
- [JIRA (Bug Tracker)](https://clojure.atlassian.net/browse/DXML)
- [Clojure XML Guide](https://clojure.org/guides/xml)
- [XML Namespace Spec](https://www.w3.org/TR/xml-names/)

## Related Libraries

- **clojure.data.zip** - Zipper utilities for XML trees
- **clojure.xml** - Older, simpler XML parsing (JDK-based)
- **enlive** - HTML parsing and templating (uses data.xml internally)
- **hiccup** - HTML generation (similar syntax style)

## Summary

clojure.data.xml is the modern, idiomatic way to work with XML in Clojure:

1. **Parse XML** - `parse` and `parse-str` convert XML to Clojure data
2. **Create elements** - `element`, `sexp-as-element`, or plain maps
3. **Emit XML** - `emit`, `emit-str`, and `indent-str` generate XML
4. **Streaming** - Lazy parsing with `event-seq` for large files
5. **Namespaces** - Full support via `alias-uri` and QNames
6. **Special nodes** - CDATA with `cdata`, comments with `xml-comment`

Use data.xml for RSS/Atom feeds, SOAP services, configuration files, SVG generation, and any XML processing needs. It provides the right balance of performance, features, and Clojure idioms.
