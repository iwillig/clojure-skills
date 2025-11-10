---
name: java-foreign-function-memory-api
description: |
  Call native C libraries and access off-heap memory safely from Clojure using Java's Foreign
  Function and Memory (FFM) API. Use when interoperating with native libraries, calling C functions,
  managing off-heap memory, replacing JNI, working with native structs, passing function pointers,
  or when the user mentions FFM API, native interop, C library bindings, Panama, jextract, memory
  segments, arenas, downcalls, upcalls, or native memory access. The FFM API (Java 19+ preview,
  finalized in Java 22) provides a safe, efficient alternative to JNI for native code interop
  without the complexity and danger of traditional JNI.
---

# Java Foreign Function and Memory API

## Quick Start

The Foreign Function and Memory (FFM) API enables Java programs to call native functions and access native memory safely, without JNI's complexity.

**Note**: FFM API requires Java 19+ (preview) or Java 22+ (finalized). Enable with `--enable-preview` flag for preview versions.

```clojure
;; Call C's strlen function from Clojure
(import '[java.lang.foreign Arena MemorySegment ValueLayout]
        '[java.lang.foreign Linker FunctionDescriptor SymbolLookup])

(defn call-strlen [^String s]
  (with-open [arena (Arena/ofConfined)]
    ;; Allocate off-heap memory for string
    (let [native-str (.allocateUtf8String arena s)
          
          ;; Get native linker
          linker (Linker/nativeLinker)
          
          ;; Find strlen function
          std-lib (.defaultLookup linker)
          strlen-addr (.get (.find std-lib "strlen"))
          
          ;; Describe function signature
          strlen-sig (FunctionDescriptor/of ValueLayout/JAVA_LONG
                                            ValueLayout/ADDRESS)
          
          ;; Create downcall handle
          strlen (.downcallHandle linker strlen-addr strlen-sig)]
      
      ;; Call C function from Java/Clojure
      (.invokeExact strlen native-str))))

(call-strlen "Hello World")
;; => 11
```

**Key benefits:**
- **Safe** - No manual memory management or crashes
- **Fast** - Direct calls without JNI overhead
- **Simple** - No C compilation or header files needed
- **Deterministic** - Explicit memory lifecycle with arenas
- **Type-safe** - Memory layouts prevent corruption

## Core Concepts

### Memory Segments

Memory segments represent contiguous regions of memory:

**Heap Segments** - Backed by Java heap (on-heap)
**Native Segments** - Backed by off-heap memory (outside JVM)

```clojure
(import '[java.lang.foreign Arena MemorySegment ValueLayout])

(with-open [arena (Arena/ofConfined)]
  ;; Allocate 1024 bytes off-heap
  (let [segment (.allocate arena 1024)]
    
    ;; Write int at offset 0
    (.set segment ValueLayout/JAVA_INT 0 42)
    
    ;; Read int from offset 0
    (.get segment ValueLayout/JAVA_INT 0)))
;; => 42
```

### Arenas (Memory Lifecycle)

Arenas control when memory is deallocated:

| Arena Type | Thread Safety | Deallocation | Use Case |
|------------|---------------|--------------|----------|
| Confined | Single-threaded | Manual (close) | Most common |
| Shared | Multi-threaded | Manual (close) | Concurrent access |
| Auto | Multi-threaded | GC managed | Convenience |
| Global | Multi-threaded | Never | System-wide memory |

```clojure
;; Confined arena (single thread)
(with-open [arena (Arena/ofConfined)]
  (let [seg (.allocate arena 100)]
    ;; Use segment
    ))
;; Memory deallocated here

;; Shared arena (multiple threads)
(with-open [arena (Arena/ofShared)]
  (let [seg (.allocate arena 100)]
    ;; Multiple threads can access seg
    ))

;; Global arena (never deallocated)
(let [arena (Arena/global)
      seg (.allocate arena 100)]
  ;; seg lives forever
  )
```

### Value Layouts

Value layouts describe memory structure:

```clojure
;; Java primitive layouts
ValueLayout/JAVA_BYTE      ; 8 bits, byte alignment
ValueLayout/JAVA_SHORT     ; 16 bits, 2-byte alignment
ValueLayout/JAVA_INT       ; 32 bits, 4-byte alignment
ValueLayout/JAVA_LONG      ; 64 bits, 8-byte alignment
ValueLayout/JAVA_FLOAT     ; 32 bits, float
ValueLayout/JAVA_DOUBLE    ; 64 bits, double
ValueLayout/ADDRESS        ; Platform-specific pointer size

;; Example: Write/read different types
(with-open [arena (Arena/ofConfined)]
  (let [seg (.allocate arena 100)]
    ;; Write values
    (.set seg ValueLayout/JAVA_INT 0 42)
    (.set seg ValueLayout/JAVA_LONG 4 1000000)
    (.set seg ValueLayout/JAVA_DOUBLE 12 3.14)
    
    ;; Read values
    [(.get seg ValueLayout/JAVA_INT 0)
     (.get seg ValueLayout/JAVA_LONG 4)
     (.get seg ValueLayout/JAVA_DOUBLE 12)]))
;; => [42 1000000 3.14]
```

### Function Descriptors

Describe native function signatures:

```clojure
(import '[java.lang.foreign FunctionDescriptor ValueLayout])

;; C: int add(int a, int b)
(def add-descriptor
  (FunctionDescriptor/of ValueLayout/JAVA_INT    ; return type
                         ValueLayout/JAVA_INT    ; param 1
                         ValueLayout/JAVA_INT))  ; param 2

;; C: void* malloc(size_t size)
(def malloc-descriptor
  (FunctionDescriptor/of ValueLayout/ADDRESS     ; return pointer
                         ValueLayout/JAVA_LONG)) ; size parameter

;; C: void free(void* ptr)
(def free-descriptor
  (FunctionDescriptor/ofVoid ValueLayout/ADDRESS)) ; void return
```

## Common Workflows

### Workflow 1: Allocating and Accessing Off-Heap Memory

Basic memory allocation and access:

```clojure
(import '[java.lang.foreign Arena MemorySegment ValueLayout])

(defn basic-memory-example []
  (with-open [arena (Arena/ofConfined)]
    ;; Allocate 100 bytes
    (let [segment (.allocate arena 100)]
      
      ;; Write data
      (.set segment ValueLayout/JAVA_INT 0 123)
      (.set segment ValueLayout/JAVA_INT 4 456)
      (.set segment ValueLayout/JAVA_INT 8 789)
      
      ;; Read data back
      (println "Values:")
      (doseq [offset [0 4 8]]
        (println (format "  Offset %d: %d"
                        offset
                        (.get segment ValueLayout/JAVA_INT offset))))
      
      ;; Get segment info
      (println (format "\nSegment size: %d bytes" (.byteSize segment)))
      (println (format "Segment address: %s" (.address segment))))))

(basic-memory-example)
;; Values:
;;   Offset 0: 123
;;   Offset 4: 456
;;   Offset 8: 789
;; Segment size: 100 bytes
;; Segment address: MemorySegment{...}
```

### Workflow 2: Storing and Reading Strings

Work with C strings:

```clojure
(import '[java.lang.foreign Arena MemorySegment ValueLayout])

(defn string-example []
  (with-open [arena (Arena/ofConfined)]
    (let [s "Hello, FFM API!"
          
          ;; Allocate and store UTF-8 string
          native-str (.allocateUtf8String arena s)]
      
      ;; Read bytes back
      (println "Reading string byte-by-byte:")
      (dotimes [i (.length s)]
        (print (char (.get native-str ValueLayout/JAVA_BYTE i))))
      (println)
      
      ;; Convert back to Java string
      (println "As Java string:" (.getUtf8String native-str 0)))))

(string-example)
;; Reading string byte-by-byte:
;; Hello, FFM API!
;; As Java string: Hello, FFM API!
```

### Workflow 3: Calling a Simple C Function

Call strlen from C standard library:

```clojure
(import '[java.lang.foreign Arena MemorySegment ValueLayout]
        '[java.lang.foreign Linker FunctionDescriptor SymbolLookup])

(defn call-strlen-example [s]
  (with-open [arena (Arena/ofConfined)]
    ;; 1. Allocate memory for argument
    (let [native-str (.allocateUtf8String arena s)
          
          ;; 2. Get native linker
          linker (Linker/nativeLinker)
          
          ;; 3. Find function address
          std-lib (.defaultLookup linker)
          strlen-addr (.get (.find std-lib "strlen"))
          
          ;; 4. Describe function: size_t strlen(const char *s)
          strlen-sig (FunctionDescriptor/of
                      ValueLayout/JAVA_LONG    ; size_t return
                      ValueLayout/ADDRESS)      ; const char* param
          
          ;; 5. Create downcall handle
          strlen (.downcallHandle linker strlen-addr strlen-sig)]
      
      ;; 6. Call function
      (.invokeExact strlen native-str))))

(call-strlen-example "Hello")
;; => 5

(call-strlen-example "This is a longer string")
;; => 23
```

### Workflow 4: Working with C Structures

Define and access structured data:

```clojure
(import '[java.lang.foreign Arena MemorySegment MemoryLayout]
        '[java.lang.foreign StructLayout ValueLayout])
(import '[java.lang SequenceLayout])

;; C struct:
;; struct Point {
;;     int x;
;;     int y;
;; }

(defn point-struct-example []
  (with-open [arena (Arena/ofConfined)]
    ;; Define struct layout
    (let [point-layout (MemoryLayout/structLayout
                        (ValueLayout/JAVA_INT) ; x field
                        (ValueLayout/JAVA_INT)) ; y field
          
          ;; Allocate struct
          point (.allocate arena point-layout)]
      
      ;; Write fields (by offset)
      (.set point ValueLayout/JAVA_INT 0 10)  ; x = 10
      (.set point ValueLayout/JAVA_INT 4 20)  ; y = 20
      
      ;; Read fields
      (let [x (.get point ValueLayout/JAVA_INT 0)
            y (.get point ValueLayout/JAVA_INT 4)]
        (println (format "Point: x=%d, y=%d" x y))
        {:x x :y y}))))

(point-struct-example)
;; Point: x=10, y=20
;; => {:x 10, :y 20}

;; More complex struct with named fields
(defn person-struct-example []
  (with-open [arena (Arena/ofConfined)]
    ;; struct Person {
    ;;     int age;
    ;;     long id;
    ;; }
    (let [person-layout (MemoryLayout/structLayout
                         (.withName ValueLayout/JAVA_INT "age")
                         (.withName ValueLayout/JAVA_LONG "id"))
          
          ;; Get field offsets
          age-offset (.byteOffset (.select person-layout "age"))
          id-offset (.byteOffset (.select person-layout "id"))
          
          ;; Allocate and populate
          person (.allocate arena person-layout)]
      
      (.set person ValueLayout/JAVA_INT age-offset 30)
      (.set person ValueLayout/JAVA_LONG id-offset 123456)
      
      {:age (.get person ValueLayout/JAVA_INT age-offset)
       :id (.get person ValueLayout/JAVA_LONG id-offset)})))

(person-struct-example)
;; => {:age 30, :id 123456}
```

### Workflow 5: Calling Functions with Multiple Arguments

Call functions with complex signatures:

```clojure
;; Call C function: int compare(int a, int b, int c)
;; Returns 1 if a > b and a > c, else 0

(defn load-custom-lib-and-call []
  (with-open [arena (Arena/ofConfined)]
    (let [linker (Linker/nativeLinker)
          
          ;; Load custom library
          lib (SymbolLookup/libraryLookup "libmylib.so" arena)
          compare-addr (.get (.find lib "compare"))
          
          ;; Describe: int compare(int, int, int)
          compare-sig (FunctionDescriptor/of
                       ValueLayout/JAVA_INT
                       ValueLayout/JAVA_INT
                       ValueLayout/JAVA_INT
                       ValueLayout/JAVA_INT)
          
          ;; Create handle
          compare (.downcallHandle linker compare-addr compare-sig)]
      
      ;; Call with arguments
      (.invokeExact compare 10 5 3))))
;; => 1 (if 10 > 5 and 10 > 3)
```

### Workflow 6: Upcalls (Passing Java Code to C)

Allow C code to call back into Java:

```clojure
(import '[java.lang.foreign Arena Linker FunctionDescriptor]
        '[java.lang.foreign ValueLayout MemorySegment])

;; Java function to pass to C
(defn my-comparator [a b]
  (compare a b))

(defn create-upcall-example []
  (with-open [arena (Arena/ofConfined)]
    (let [linker (Linker/nativeLinker)
          
          ;; Describe callback: int compare(int, int)
          comparator-sig (FunctionDescriptor/of
                          ValueLayout/JAVA_INT
                          ValueLayout/JAVA_INT
                          ValueLayout/JAVA_INT)
          
          ;; Create method handle for Java function
          target (reify java.lang.invoke.MethodHandle
                   ;; Implementation would wrap my-comparator
                   )
          
          ;; Create upcall stub
          upcall-stub (.upcallStub linker target comparator-sig arena)]
      
      ;; upcall-stub is a MemorySegment containing function pointer
      ;; Can be passed to C functions expecting a callback
      upcall-stub)))

;; Note: Full upcall example requires MethodHandles.Lookup
;; See Java documentation for complete implementation
```

### Workflow 7: Working with Arrays

Allocate and access arrays in native memory:

```clojure
(import '[java.lang.foreign Arena MemorySegment ValueLayout])

(defn array-example []
  (with-open [arena (Arena/ofConfined)]
    (let [array-size 10
          int-size (.byteSize ValueLayout/JAVA_INT)
          
          ;; Allocate array of ints
          arr (.allocate arena (* array-size int-size))]
      
      ;; Write array values
      (dotimes [i array-size]
        (.set arr ValueLayout/JAVA_INT (* i int-size) (* i i)))
      
      ;; Read array values
      (println "Array contents:")
      (dotimes [i array-size]
        (println (format "  arr[%d] = %d"
                        i
                        (.get arr ValueLayout/JAVA_INT (* i int-size)))))
      
      ;; Return as Clojure vector
      (mapv #(.get arr ValueLayout/JAVA_INT (* % int-size))
            (range array-size)))))

(array-example)
;; Array contents:
;;   arr[0] = 0
;;   arr[1] = 1
;;   arr[2] = 4
;;   arr[3] = 9
;;   arr[4] = 16
;;   ...
;; => [0 1 4 9 16 25 36 49 64 81]
```

### Workflow 8: Slicing Memory Segments

Work with portions of memory segments:

```clojure
(import '[java.lang.foreign Arena MemorySegment ValueLayout])

(defn slicing-example []
  (with-open [arena (Arena/ofConfined)]
    (let [segment (.allocate arena 100)]
      
      ;; Write data to original segment
      (.set segment ValueLayout/JAVA_INT 0 10)
      (.set segment ValueLayout/JAVA_INT 20 20)
      (.set segment ValueLayout/JAVA_INT 40 30)
      
      ;; Create slices
      (let [slice1 (.asSlice segment 0 20)    ; Bytes 0-19
            slice2 (.asSlice segment 20 20)]  ; Bytes 20-39
        
        (println "Slice 1:")
        (println "  Value at offset 0:" (.get slice1 ValueLayout/JAVA_INT 0))
        
        (println "Slice 2:")
        (println "  Value at offset 0:" (.get slice2 ValueLayout/JAVA_INT 0))
        
        ;; Slices share memory with original
        (.set slice1 ValueLayout/JAVA_INT 0 999)
        (println "After modifying slice1:")
        (println "  Original at offset 0:" (.get segment ValueLayout/JAVA_INT 0))))))

(slicing-example)
;; Slice 1:
;;   Value at offset 0: 10
;; Slice 2:
;;   Value at offset 0: 20
;; After modifying slice1:
;;   Original at offset 0: 999
```

## Best Practices

**Do:**
- Use `try-with-resources` (with-open) for arena management
- Use confined arenas for single-threaded access (fastest)
- Check segment validity before access
- Use value layouts to prevent alignment issues
- Close arenas explicitly to free memory
- Use defaultLookup() for standard library functions
- Handle function invocation exceptions

```clojure
;; Good: Proper arena management
(with-open [arena (Arena/ofConfined)]
  (let [seg (.allocate arena 100)]
    ;; Use segment
    ))
;; Memory automatically freed

;; Good: Explicit layout usage
(.set segment ValueLayout/JAVA_INT offset value)

;; Good: Exception handling
(try
  (.invokeExact native-func arg)
  (catch Throwable t
    (println "Native call failed:" (.getMessage t))))
```

**Don't:**
- Don't access segments after arena is closed
- Don't forget --enable-preview flag (Java 19-21)
- Don't ignore alignment requirements
- Don't share confined arena segments across threads
- Don't assume pointer sizes (use ValueLayout/ADDRESS)
- Don't manually manage memory lifecycle (use arenas)
- Don't skip function descriptor validation

```clojure
;; Bad: Accessing closed segment
(let [seg (with-open [arena (Arena/ofConfined)]
            (.allocate arena 100))]
  (.get seg ValueLayout/JAVA_INT 0))  ; IllegalStateException!

;; Bad: Wrong alignment
(.set segment ValueLayout/JAVA_LONG 3 value)  ; Not 8-byte aligned!

;; Good: Proper alignment
(.set segment ValueLayout/JAVA_LONG 8 value)  ; 8-byte aligned
```

## Common Issues

### Issue: IllegalStateException - Already closed

```clojure
(let [seg (with-open [arena (Arena/ofConfined)]
            (.allocate arena 100))]
  (.get seg ValueLayout/JAVA_INT 0))
;; Exception: java.lang.IllegalStateException: Already closed
```

**Solution**: Access segments within arena scope:
```clojure
(with-open [arena (Arena/ofConfined)]
  (let [seg (.allocate arena 100)]
    (.get seg ValueLayout/JAVA_INT 0)))  ; OK
```

### Issue: UnsatisfiedLinkError - Function not found

```clojure
(let [linker (Linker/nativeLinker)
      lib (.defaultLookup linker)]
  (.find lib "nonexistent_function"))
;; Returns empty Optional
```

**Solution**: Check library path and function name:
```clojure
(let [linker (Linker/nativeLinker)
      lib (.defaultLookup linker)
      func (.find lib "strlen")]
  (if (.isPresent func)
    (.get func)
    (throw (Exception. "Function not found"))))
```

### Issue: WrongMethodTypeException - Signature mismatch

```clojure
;; Wrong: int parameter but long descriptor
(let [descriptor (FunctionDescriptor/of
                  ValueLayout/JAVA_INT
                  ValueLayout/JAVA_LONG)]  ; Wrong!
  (.invokeExact handle 42))  ; 42 is int, not long
```

**Solution**: Match descriptor to actual arguments:
```clojure
;; Correct: Cast to match descriptor
(.invokeExact handle (long 42))

;; Or: Use correct descriptor
(let [descriptor (FunctionDescriptor/of
                  ValueLayout/JAVA_INT
                  ValueLayout/JAVA_INT)]
  (.invokeExact handle 42))
```

### Issue: Preview features not enabled

```
Error: strlen is a preview API and is disabled by default.
```

**Solution**: Enable preview features:
```bash
# Clojure CLI
clojure -J--enable-preview -J--add-modules=jdk.incubator.foreign

# Leiningen (project.clj)
:jvm-opts ["--enable-preview" "--add-modules=jdk.incubator.foreign"]

# Java 22+ (finalized, no flag needed)
clojure
```

### Issue: Segmentation fault / JVM crash

```
# A fatal error has been detected by the Java Runtime Environment:
# SIGSEGV (0xb) at pc=0x00007fff...
```

**Cause**: Accessing invalid memory, wrong pointer, or alignment issue

**Solution**: 
- Verify segment validity
- Check offsets and sizes
- Validate function descriptors match C signatures
- Ensure proper alignment for all accesses

## Advanced Topics

### Restricted Methods

Some FFM methods are "restricted" and require special permission:

```clojure
;; Methods like these require --enable-native-access
;; or are restricted in some contexts:
(.address segment)  ; Get raw memory address
(.reinterpret segment new-size)  ; Change segment interpretation
```

Enable with:
```bash
java --enable-native-access=ALL-UNNAMED
```

### jextract Tool

Generate Java bindings from C headers automatically:

```bash
# Generate bindings for a library
jextract --output src -t com.example.bindings /usr/include/mylib.h

# Use generated bindings
(import '[com.example.bindings mylib])
(mylib/my_function arg1 arg2)
```

**Benefits:**
- No manual function descriptor creation
- Type-safe bindings
- Automatic structure layouts
- Comprehensive API coverage

### MemorySession (Legacy API - Before Java 20)

Older versions used MemorySession instead of Arena:

```clojure
;; Old API (Java 19)
(let [session (MemorySession/openConfined)]
  (try
    (let [seg (MemorySegment/allocateNative 100 session)]
      ;; Use segment
      )
    (finally
      (.close session))))

;; New API (Java 20+)
(with-open [arena (Arena/ofConfined)]
  (let [seg (.allocate arena 100)]
    ;; Use segment
    ))
```

### Endianness and Byte Order

Control byte order for multi-byte values:

```clojure
(import '[java.nio ByteOrder])

;; Create layout with specific byte order
(let [big-endian-int (.withOrder ValueLayout/JAVA_INT
                                  ByteOrder/BIG_ENDIAN)
      little-endian-int (.withOrder ValueLayout/JAVA_INT
                                    ByteOrder/LITTLE_ENDIAN)]
  
  ;; Use in set/get operations
  (.set segment big-endian-int 0 42))
```

## Related Technologies

- **JNI** - Traditional Java Native Interface (complex, unsafe)
- **JNA** - Java Native Access library (reflection-based, slower)
- **JNR** - Java Native Runtime (predecessor to FFM)
- **Panama Project** - Overall project for FFM API development
- **jextract** - Tool to generate FFM bindings from C headers

## Resources

- Java 21 FFM docs: https://docs.oracle.com/en/java/javase/21/core/foreign-function-and-memory-api.html
- JEP 442 (FFM API): https://openjdk.org/jeps/442
- JEP 454 (FFM API Finalized): https://openjdk.org/jeps/454
- Panama Project: https://openjdk.org/projects/panama/
- jextract tool: https://github.com/openjdk/jextract
- API Javadoc: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/foreign/package-summary.html

## Summary

The Foreign Function and Memory API enables safe, efficient native interop:

1. **Memory Segments** - Safe access to on-heap and off-heap memory
2. **Arenas** - Explicit, deterministic memory lifecycle management
3. **Value Layouts** - Type-safe memory access with proper alignment
4. **Downcalls** - Call C functions directly from Java/Clojure
5. **Upcalls** - Pass Java code as callbacks to C functions
6. **Structured Access** - Work with C structs and arrays safely

**Most common patterns:**

```clojure
;; Allocate and access memory
(with-open [arena (Arena/ofConfined)]
  (let [seg (.allocate arena 100)]
    (.set seg ValueLayout/JAVA_INT 0 42)
    (.get seg ValueLayout/JAVA_INT 0)))

;; Call C function
(with-open [arena (Arena/ofConfined)]
  (let [arg (.allocateUtf8String arena "hello")
        linker (Linker/nativeLinker)
        lib (.defaultLookup linker)
        func-addr (.get (.find lib "strlen"))
        func-sig (FunctionDescriptor/of ValueLayout/JAVA_LONG
                                        ValueLayout/ADDRESS)
        handle (.downcallHandle linker func-addr func-sig)]
    (.invokeExact handle arg)))

;; Work with C struct
(let [point-layout (MemoryLayout/structLayout
                     ValueLayout/JAVA_INT
                     ValueLayout/JAVA_INT)
      point (.allocate arena point-layout)]
  (.set point ValueLayout/JAVA_INT 0 10)
  (.set point ValueLayout/JAVA_INT 4 20))
```

Perfect for calling native libraries, system APIs, and high-performance native code from Clojure without JNI complexity.
