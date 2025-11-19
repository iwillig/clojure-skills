---
name: data-zip
description: |
  Navigate and query XML trees using zipper-based selectors. Use when querying XML
  documents, filtering elements by attributes or text, extracting nested values,
  implementing XPath-like queries, or traversing XML hierarchies. Works with
  clojure.data.xml structures. Essential for XML data extraction and transformation.
---

# clojure.data.zip - XML Navigation with Zippers

## Quick Start

clojure.data.zip provides functional navigation and querying of XML trees using zippers.

```clojure
(require '[clojure.data.xml :as xml])
(require '[clojure.zip :as zip])
(require '[clojure.data.zip.xml :as zx])

;; Create sample XML
(def books
  (xml/element :library {}
    (xml/element :book {:id "1"}
      (xml/element :title {} "The Hobbit")
      (xml/element :author {} "J.R.R. Tolkien")
      (xml/element :year {} "1937"))
    (xml/element :book {:id "2"}
      (xml/element :title {} "1984")
      (xml/element :author {} "George Orwell")
      (xml/element :year {} "1949"))))

;; Create zipper
(def loc (zip/xml-zip books))

;; Query all book titles
(zx/xml-> loc :book :title zx/text)
; => ("The Hobbit" "1984")

;; Query single result
(zx/xml1-> loc :book :title zx/text)
; => "The Hobbit"

;; Filter by attribute
(zx/xml-> loc :book (zx/attr= :id "2") :title zx/text)
; => ("1984")

;; Extract attribute values
(zx/xml-> loc :book (zx/attr :id))
; => ("1" "2")
```

**Key benefits:**
- XPath-like querying with Clojure syntax
- Composable query predicates
- Lazy evaluation for large XML documents
- Filter by tags, attributes, and text content
- Navigate parent-child relationships
- Works seamlessly with clojure.data.xml

## Core Concepts

### Zippers for XML Navigation

A zipper is a functional data structure for tree traversal and modification. clojure.data.zip builds on clojure.zip to provide XML-specific navigation:

```clojure
(require '[clojure.zip :as zip])
(require '[clojure.data.xml :as xml])

;; Create XML zipper from an element
(def xml-tree (xml/parse-str "<root><child>value</child></root>"))
(def loc (zip/xml-zip xml-tree))

;; Zipper location contains:
;; - Current node (zip/node loc)
;; - Navigation context (parent, siblings, etc.)
;; - Movement functions (zip/down, zip/up, zip/right)

;; Get current node
(zip/node loc)
; => #xml/element{:tag :root, :content [...]}

;; Move down to first child
(-> loc zip/down zip/node)
; => #xml/element{:tag :child, :content ["value"]}
```

Zippers enable efficient tree navigation without modifying the original structure.

### xml-> Threading Macro

The `xml->` macro chains navigation steps from left to right:

```clojure
;; Syntax: (xml-> loc & preds)
;; Each predicate filters or navigates the current locations

(zx/xml-> loc :book)
; => Find all <book> children

(zx/xml-> loc :book :title)
; => Find all <title> elements within <book> elements

(zx/xml-> loc :book :title zx/text)
; => Extract text content from those titles

;; Equivalent to:
(->> loc
     (filter-by-tag :book)
     (mapcat (fn [loc] (filter-by-tag :title loc)))
     (map extract-text))
```

Each step filters or transforms the sequence of locations.

### xml1-> for Single Results

Use `xml1->` when you expect exactly one result:

```clojure
;; xml-> returns a sequence
(zx/xml-> loc :book :title zx/text)
; => ("The Hobbit" "1984")

;; xml1-> returns first result only
(zx/xml1-> loc :book :title zx/text)
; => "The Hobbit"

;; Useful for known-unique queries
(zx/xml1-> loc :metadata :version zx/text)
; => "1.0"
```

## Common Workflows

### Workflow 1: Extracting Text Content

```clojure
(def doc
  (xml/element :article {}
    (xml/element :title {} "Clojure Data Structures")
    (xml/element :author {} "Rich Hickey")
    (xml/element :content {}
      (xml/element :section {}
        (xml/element :heading {} "Immutability")
        (xml/element :paragraph {} "Data structures are immutable...")))))

(def loc (zip/xml-zip doc))

;; Extract single text value
(zx/xml1-> loc :title zx/text)
; => "Clojure Data Structures"

;; Extract all section headings
(zx/xml-> loc :content :section :heading zx/text)
; => ("Immutability")

;; Extract nested text
(zx/xml-> loc :content :section :paragraph zx/text)
; => ("Data structures are immutable...")
```

### Workflow 2: Filtering by Attributes

```clojure
(def products
  (xml/element :catalog {}
    (xml/element :product {:id "p1" :category "electronics"}
      (xml/element :name {} "Laptop")
      (xml/element :price {} "999"))
    (xml/element :product {:id "p2" :category "books"}
      (xml/element :name {} "Clojure Programming")
      (xml/element :price {} "45"))
    (xml/element :product {:id "p3" :category "electronics"}
      (xml/element :name {} "Keyboard")
      (xml/element :price {} "79"))))

(def loc (zip/xml-zip products))

;; Find products by category
(zx/xml-> loc :product (zx/attr= :category "electronics") :name zx/text)
; => ("Laptop" "Keyboard")

;; Find product by ID
(zx/xml1-> loc :product (zx/attr= :id "p2") :name zx/text)
; => "Clojure Programming"

;; Extract all product IDs
(zx/xml-> loc :product (zx/attr :id))
; => ("p1" "p2" "p3")
```

### Workflow 3: Filtering by Text Content

```clojure
(def config
  (xml/element :settings {}
    (xml/element :option {:name "debug"}
      (xml/element :value {} "true"))
    (xml/element :option {:name "timeout"}
      (xml/element :value {} "30"))
    (xml/element :option {:name "retry"}
      (xml/element :value {} "true"))))

(def loc (zip/xml-zip config))

;; Find options with value "true"
(zx/xml-> loc :option :value (zx/text= "true"))
; => [zipper-location zipper-location]
;; Returns zipper locations, not just text

;; Get parent option names where value is "true"
(->> (zx/xml-> loc :option)
     (filter #(= "true" (zx/xml1-> % :value zx/text)))
     (map #(zx/attr % :name)))
; => ("debug" "retry")
```

### Workflow 4: Complex Predicates

```clojure
(def inventory
  (xml/element :warehouse {}
    (xml/element :item {:type "widget"}
      (xml/element :quantity {} "100")
      (xml/element :location {} "A1"))
    (xml/element :item {:type "gadget"}
      (xml/element :quantity {} "5")
      (xml/element :location {} "B2"))
    (xml/element :item {:type "widget"}
      (xml/element :quantity {} "50")
      (xml/element :location {} "C3"))))

(def loc (zip/xml-zip inventory))

;; Find low-stock items (quantity < 10)
(->> (zx/xml-> loc :item)
     (filter (fn [item-loc]
               (when-let [qty (zx/xml1-> item-loc :quantity zx/text)]
                 (< (Integer/parseInt qty) 10))))
     (map #(zx/attr % :type)))
; => ("gadget")

;; Combine multiple filters
(defn widget-in-aisle-a? [item-loc]
  (and (= "widget" (zx/attr item-loc :type))
       (when-let [loc-code (zx/xml1-> item-loc :location zx/text)]
         (.startsWith loc-code "A"))))

(->> (zx/xml-> loc :item)
     (filter widget-in-aisle-a?)
     (map #(zx/xml1-> % :quantity zx/text)))
; => ("100")
```

### Workflow 5: Navigating Hierarchies

```clojure
(require '[clojure.data.zip :as dz])

(def menu
  (xml/element :menu {}
    (xml/element :category {:name "File"}
      (xml/element :item {} "New")
      (xml/element :item {} "Open")
      (xml/element :submenu {:name "Recent"}
        (xml/element :item {} "file1.txt")
        (xml/element :item {} "file2.txt")))
    (xml/element :category {:name "Edit"}
      (xml/element :item {} "Copy")
      (xml/element :item {} "Paste"))))

(def loc (zip/xml-zip menu))

;; Get all immediate children
(map zip/node (dz/children loc))
; => [<category "File"> <category "Edit">]

;; Get all descendants (entire tree)
(count (dz/descendants loc))
; => 13

;; Find all items at any depth
(zx/xml-> loc :category :* zx/text)
; => ("New" "Open" "Copy" "Paste")
; Note: :* matches any tag, but only at specific depth

;; Navigate with custom predicates
(defn find-items-in-category [loc category-name]
  (zx/xml-> loc :category 
    (zx/attr= :name category-name)
    :item zx/text))

(find-items-in-category loc "File")
; => ("New" "Open")
```

### Workflow 6: Working with Namespaced XML

```clojure
(require '[clojure.data.xml :as xml])

;; XML with namespaces
(def ns-doc
  (xml/parse-str
    "<?xml version=\"1.0\"?>
     <rss version=\"2.0\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">
       <channel>
         <item>
           <title>Article 1</title>
           <dc:creator>John Doe</dc:creator>
         </item>
       </channel>
     </rss>"))

(def loc (zip/xml-zip ns-doc))

;; Access namespaced elements using keyword syntax
;; Namespace URIs are encoded in keywords
(zx/xml-> loc :channel :item :title zx/text)
; => ("Article 1")

;; For namespaced tags, use the full namespace URI
;; The tag will be a QName, not a simple keyword
```

### Workflow 7: Combining with Data Transformation

```clojure
(def orders
  (xml/element :orders {}
    (xml/element :order {:id "o1"}
      (xml/element :item {} "Widget")
      (xml/element :quantity {} "10")
      (xml/element :price {} "99.99"))
    (xml/element :order {:id "o2"}
      (xml/element :item {} "Gadget")
      (xml/element :quantity {} "5")
      (xml/element :price {} "149.99"))))

(def loc (zip/xml-zip orders))

;; Extract structured data
(->> (zx/xml-> loc :order)
     (map (fn [order-loc]
            {:id (zx/attr order-loc :id)
             :item (zx/xml1-> order-loc :item zx/text)
             :quantity (some-> (zx/xml1-> order-loc :quantity zx/text)
                               Integer/parseInt)
             :price (some-> (zx/xml1-> order-loc :price zx/text)
                            Double/parseDouble)}))
     (filter #(> (:quantity %) 5)))
; => ({:id "o1", :item "Widget", :quantity 10, :price 99.99})

;; Calculate totals
(->> (zx/xml-> loc :order)
     (map (fn [order-loc]
            (let [qty (some-> (zx/xml1-> order-loc :quantity zx/text)
                              Integer/parseInt)
                  price (some-> (zx/xml1-> order-loc :price zx/text)
                                Double/parseDouble)]
              (* qty price))))
     (reduce +))
; => 1749.85
```

## Core Functions Reference

### Query Functions

**`xml-> [loc & preds]`**
- Chains navigation steps left to right
- Returns sequence of locations matching all predicates
- Each step filters or transforms current locations

**`xml1-> [loc & preds]`**
- Like `xml->` but returns first result only
- Use when expecting single result
- Returns nil if no match found

### Predicate Functions

**`text`**
- Extracts text content from element
- Returns string or nil

**`text= [string]`**
- Predicate that matches elements with exact text content
- Returns sequence of matching locations

**`attr [attr-name]`**
- Extracts attribute value
- Returns string value or nil

**`attr= [attr-name value]`**
- Predicate that matches elements with specific attribute value
- Case-sensitive comparison

**`tag= [tagname]`**
- Predicate that matches elements with specific tag name
- Use with `:*` wildcard for any child

### Navigation Functions (clojure.data.zip)

**`children [loc]`**
- Returns lazy sequence of immediate child locations
- Left to right order

**`descendants [loc]`**
- Returns lazy sequence of all descendant locations
- Depth-first traversal

**`ancestors [loc]`**
- Returns sequence of ancestor locations
- From immediate parent to root

**`left-locs [loc]`**
- Returns sequence of all left sibling locations

**`right-locs [loc]`**
- Returns sequence of all right sibling locations

**`leftmost? [loc]`**
- Returns true if location is leftmost sibling

**`rightmost? [loc]`**
- Returns true if location is rightmost sibling

## When to Use Each Approach

**Use `xml->` when:**
- Querying multiple elements
- Building sequences of results
- Traversing collections

**Use `xml1->` when:**
- Expecting exactly one result
- Accessing known-unique elements
- Simplifying code (avoid `first`)

**Use `attr` when:**
- Extracting attribute values
- Building data maps from XML

**Use `attr=` when:**
- Filtering by attribute values
- Finding specific elements by ID

**Use `text` when:**
- Extracting element content
- Building strings from XML

**Use `text=` when:**
- Filtering by text content
- Finding elements with specific values

**Use custom predicates when:**
- Complex filtering logic required
- Combining multiple conditions
- Domain-specific queries

## Best Practices

**DO:**
- Use `xml1->` for single-result queries (clearer intent)
- Chain predicates for readable queries
- Extract data transformation logic to named functions
- Handle nil results with `some->` or `when-let`
- Use zippers for read-only navigation (not mutation)
- Leverage lazy evaluation for large XML documents
- Combine with clojure.data.xml for complete XML workflow

**DON'T:**
- Assume queries always return non-nil results
- Mix zipper modification with data.zip queries
- Use zippers for XML generation (use data.xml instead)
- Forget that `xml->` returns a sequence
- Parse strings repeatedly (parse once, query many times)
- Use mutable operations on zipper locations

## Common Issues

### Issue: Query Returns Empty Sequence

**Problem:** Expected results but got empty sequence.

```clojure
(zx/xml-> loc :book :title zx/text)
; => ()
```

**Solution:** Check tag names match exactly (case-sensitive).

```clojure
;; Debug: Print available tags
(map (comp :tag zip/node) (dz/children loc))
; => (:Book)  ; Capital B!

;; Fix: Use correct tag name
(zx/xml-> loc :Book :title zx/text)
; => ("The Hobbit")
```

### Issue: NullPointerException on Text Extraction

**Problem:** Calling string functions on nil result.

```clojure
(-> (zx/xml1-> loc :missing :element zx/text)
    clojure.string/upper-case)
; => NullPointerException
```

**Solution:** Use safe navigation.

```clojure
(some-> (zx/xml1-> loc :missing :element zx/text)
        clojure.string/upper-case)
; => nil

;; Or provide default
(or (zx/xml1-> loc :missing :element zx/text) "default")
; => "default"
```

### Issue: Confusion Between xml-> and xml1->

**Problem:** Using wrong function leads to type errors.

```clojure
;; Wrong: xml-> returns sequence, not string
(str/upper-case (zx/xml-> loc :title zx/text))
; => Error: Can't call upper-case on sequence

;; Wrong: xml1-> returns first only
(count (zx/xml1-> loc :book :title zx/text))
; => 11 (counts characters in "The Hobbit", not number of books)
```

**Solution:** Match function to expected result count.

```clojure
;; For single result, use xml1->
(str/upper-case (zx/xml1-> loc :title zx/text))
; => "THE HOBBIT"

;; For multiple results, use xml->
(count (zx/xml-> loc :book :title zx/text))
; => 2 (number of titles)
```

### Issue: Attribute Predicates Not Working

**Problem:** `attr=` doesn't filter as expected.

```clojure
(zx/xml-> loc :book (zx/attr= :id 1) :title zx/text)
; => ()  ; Expected "The Hobbit"
```

**Solution:** Attributes are strings, not numbers.

```clojure
;; Correct: Use string value
(zx/xml-> loc :book (zx/attr= :id "1") :title zx/text)
; => ("The Hobbit")
```

### Issue: Nested Query Performance

**Problem:** Deep queries are slow on large documents.

```clojure
;; Slow: Searches entire tree multiple times
(zx/xml-> loc :** :deeply :nested :element zx/text)
```

**Solution:** Parse once, cache intermediate results.

```clojure
;; Cache books once
(def book-locs (zx/xml-> loc :library :book))

;; Query cached results
(map #(zx/xml1-> % :title zx/text) book-locs)
; => ("The Hobbit" "1984")
```

## Advanced Topics

### Custom Predicate Combinators

```clojure
(defn or-pred [& preds]
  "Returns location if any predicate matches"
  (fn [loc]
    (when (some #(% loc) preds)
      [loc])))

(defn and-pred [& preds]
  "Returns location if all predicates match"
  (fn [loc]
    (when (every? #(% loc) preds)
      [loc])))

;; Usage
(zx/xml-> loc :item
  (or-pred (zx/attr= :type "widget")
           (zx/attr= :type "gadget")))
```

### Integration with clojure.data.xml

```clojure
(require '[clojure.data.xml :as xml])

;; Parse, query, and emit workflow
(-> (xml/parse-str xml-string)
    zip/xml-zip
    (zx/xml-> :config :setting zx/text)
    first
    (xml/element :result {}))
```

### XPath-Like Queries

```clojure
;; Simulate XPath: //book[@id='1']/title/text()
(zx/xml-> loc :book (zx/attr= :id "1") :title zx/text)

;; Simulate XPath: //book/title[contains(text(), 'Hobbit')]
(->> (zx/xml-> loc :book :title)
     (filter #(when-let [t (zx/text %)]
                (.contains t "Hobbit")))
     (map zx/text))
```

## Resources

- [clojure.data.zip API](https://clojure.github.io/data.zip/)
- [GitHub Repository](https://github.com/clojure/data.zip)
- [clojure.zip API](https://clojure.github.io/clojure/clojure.zip-api.html)
- [Zipper Tutorial](https://clojure.org/reference/other_libraries#_zippers)

## Summary

clojure.data.zip provides functional XML querying with zippers:

1. **xml-> threading** - Chain navigation steps for sequences
2. **xml1-> threading** - Extract single results
3. **Text predicates** - Filter and extract text content
4. **Attribute predicates** - Filter and extract attributes
5. **Tag predicates** - Match elements by tag name
6. **Navigation functions** - Traverse hierarchies (children, descendants, ancestors)
7. **Lazy evaluation** - Efficient for large documents

**Core workflow:**
1. Parse XML with clojure.data.xml
2. Create zipper with `zip/xml-zip`
3. Query with `xml->` or `xml1->`
4. Extract data with `text`, `attr`
5. Filter with predicates (`attr=`, `text=`, `tag=`)
6. Transform results into Clojure data structures

Use clojure.data.zip when you need XPath-like querying with functional composition and lazy evaluation.
