---
name: criterium-benchmarking
description: |
  Rigorous performance benchmarking for Clojure code with statistical analysis. Use when
  measuring execution time, comparing algorithm performance, optimizing code, profiling hot
  paths, or when the user mentions benchmarking, performance measurement, JIT warmup, JVM
  benchmarking, execution time, statistical analysis, or performance testing. Criterium
  addresses JVM benchmarking pitfalls including JIT compilation, garbage collection, and
  statistical variance through multiple evaluations, warmup periods, GC control, and
  statistical processing of results.
---

# Criterium - Statistical Benchmarking for Clojure

## Quick Start

Criterium provides rigorous benchmarking that accounts for JVM quirks like JIT compilation and garbage collection.

```clojure
(require '[criterium.core :refer [bench quick-bench]])

;; Quick benchmark - faster, less rigorous
(quick-bench (reduce + (range 1000)))
;; => Execution time mean : 8.123 µs
;;    Execution time std-deviation : 234.5 ns
;;    Execution time lower quantile : 7.891 µs ( 2.5%)
;;    Execution time upper quantile : 8.567 µs (97.5%)
;;    Overhead used : 2.1 ns

;; Full benchmark - more samples, more accurate
(bench (reduce + (range 1000)))
;; => Takes ~10-60 seconds
;;    Provides detailed statistical analysis

;; With progress reporting
(require '[criterium.core :refer [with-progress-reporting]])

(with-progress-reporting
  (bench (Thread/sleep 1) :verbose))
;; Prints progress during benchmark
```

**Key benefits:**
- **JIT warmup** - Runs code before measuring to let JIT optimize
- **GC isolation** - Forces GC before/after to isolate timing
- **Statistical analysis** - Multiple samples with mean, std-dev, quantiles
- **Outlier detection** - Identifies and reports outliers
- **Overhead estimation** - Accounts for measurement overhead

## Core Concepts

### JVM Benchmarking Challenges

**JIT Compilation:**
- JVM compiles frequently-run code to native code
- First runs are slower (interpreted mode)
- Criterium runs warmup period before measurement

**Garbage Collection:**
- GC can pause execution unpredictably
- Criterium forces GC before samples
- Reports final GC impact after measurement

**Measurement Overhead:**
- Timing code itself takes time
- Criterium estimates and subtracts overhead
- Prevents negative times for fast operations

**Statistical Variance:**
- Single measurements are unreliable
- Criterium takes multiple samples
- Calculates mean, standard deviation, quantiles

### Benchmark Types

**Full Benchmark** (`bench`):
- Takes 10-70 seconds
- 60 samples minimum
- Rigorous warmup period
- Best for accurate measurements

**Quick Benchmark** (`quick-bench`):
- Takes 1-10 seconds
- Fewer samples
- Shorter warmup
- Good for rough estimates

**Custom Benchmark** (`benchmark`):
- Full control over options
- Returns data structure
- For programmatic use

### Statistical Outputs

Criterium reports:
- **Mean** - Average execution time
- **Standard Deviation** - Variance in measurements
- **Quantiles** - 2.5% and 97.5% (95% confidence interval)
- **Outliers** - Samples outside normal range
- **Overhead** - Measurement overhead estimate

## Common Workflows

### Workflow 1: Quick Performance Check

Get a fast estimate of execution time:

```clojure
(require '[criterium.core :refer [quick-bench]])

;; Benchmark collection operations
(quick-bench (into [] (range 1000)))
;; => Execution time mean : 15.2 µs

(quick-bench (vec (range 1000)))
;; => Execution time mean : 14.8 µs

(quick-bench (into [] (map inc) (range 1000)))
;; => Execution time mean : 18.5 µs

;; Benchmark string operations
(quick-bench (clojure.string/join "," (range 100)))
;; => Execution time mean : 12.3 µs

;; Benchmark map operations
(quick-bench (assoc {:a 1 :b 2} :c 3))
;; => Execution time mean : 125 ns
```

**Use `quick-bench` when:**
- Getting rough performance estimates
- Testing multiple approaches quickly
- Time is limited
- Exact precision not critical

### Workflow 2: Rigorous Performance Measurement

Get accurate measurements with full statistical analysis:

```clojure
(require '[criterium.core :refer [bench]])

;; Full benchmark with all statistics
(bench (reduce + (range 10000)))
;; Output:
;;   Evaluation count : 5760 in 60 samples of 96 calls.
;;              Execution time mean : 10.234 µs
;;     Execution time std-deviation : 1.234 µs
;;    Execution time lower quantile : 9.123 µs ( 2.5%)
;;    Execution time upper quantile : 12.456 µs (97.5%)
;;                    Overhead used : 2.1 ns
;;
;; Found 3 outliers in 60 samples (5.0000 %)
;;   low-severe   2 (3.3333 %)
;;   low-mild     1 (1.6667 %)
;; Variance from outliers : 13.8889 % Variance is moderately inflated by outliers
```

**Use `bench` when:**
- Need accurate measurements
- Comparing subtle performance differences
- Publishing benchmark results
- Making optimization decisions

### Workflow 3: Comparing Implementations

Compare multiple approaches to find the fastest:

```clojure
(require '[criterium.core :refer [quick-bench]])

;; Compare ways to sum numbers
(println "reduce +:")
(quick-bench (reduce + (range 1000)))

(println "\napply +:")
(quick-bench (apply + (range 1000)))

(println "\nloop/recur:")
(quick-bench 
  (loop [i 0 sum 0]
    (if (< i 1000)
      (recur (inc i) (+ sum i))
      sum)))

;; Compare transducers vs lazy sequences
(println "\nLazy map:")
(quick-bench (doall (map inc (range 1000))))

(println "\nTransducer:")
(quick-bench (into [] (map inc) (range 1000)))

;; Results show which is fastest
```

**Pattern:**
1. Identify alternative implementations
2. Benchmark each separately
3. Compare mean execution times
4. Check for variance/outliers
5. Choose fastest stable approach

### Workflow 4: Programmatic Benchmarking

Access raw benchmark data for custom analysis:

```clojure
(require '[criterium.core :refer [benchmark quick-benchmark]])

;; Get benchmark data structure
(def result (quick-benchmark (reduce + (range 1000)) {}))

;; Examine results
(:sample-count result)    ;; => 60
(:execution-count result) ;; => 960 (executions per sample)
(:mean result)            ;; => [8.123e-6 8.234e-6] (mean with CI)
(:variance result)        ;; => [2.345e-13 2.456e-13]

;; Extract specific metrics
(defn get-mean-time [result]
  (first (:mean result)))

(defn get-std-dev [result]
  (Math/sqrt (first (:variance result))))

;; Compare programmatically
(defn faster? [result1 result2]
  (< (get-mean-time result1)
     (get-mean-time result2)))

(def sum-reduce (quick-benchmark (reduce + (range 1000)) {}))
(def sum-apply (quick-benchmark (apply + (range 1000)) {}))

(when (faster? sum-reduce sum-apply)
  (println "reduce is faster"))
```

### Workflow 5: With Progress Reporting

Show progress during long benchmarks:

```clojure
(require '[criterium.core :refer [with-progress-reporting bench quick-bench]])

;; Enable progress output
(with-progress-reporting
  (bench (Thread/sleep 1) :verbose))

;; Output during benchmark:
;; Warming up for JIT optimisations.
;; Estimating sampling overhead.
;; Warming up for JIT optimisations.
;; Running 60 samples...
;; [Progress indicators]
;; Final GC...
;; Checking GC...

;; Works with quick-bench too
(with-progress-reporting
  (quick-bench (expensive-operation) :verbose))
```

**Use when:**
- Benchmark takes a long time
- Want to see progress
- Debugging benchmark issues
- Understanding what Criterium is doing

### Workflow 6: Custom Benchmark Options

Control benchmark parameters for specific needs:

```clojure
(require '[criterium.core :refer [benchmark]])

;; Full benchmark with custom options
(def result
  (benchmark 
    (reduce + (range 10000))
    {:samples 100                      ; More samples (default 60)
     :warmup-jit-period 5000000000    ; 5 second warmup (default 10s)
     :target-execution-time 100000000  ; 100ms per sample (default 1s)
     :verbose true}))

;; Quick benchmark with options
(def quick-result
  (quick-benchmark
    (reduce + (range 1000))
    {:samples 30                      ; Fewer samples
     :warmup-jit-period 1000000000    ; 1 second warmup
     :verbose false}))

;; Disable GC before each sample (not recommended)
(def no-gc-result
  (benchmark
    (my-function)
    {:gc-before-sample false}))
```

**Common options:**
- `:samples` - Number of samples to take
- `:warmup-jit-period` - Warmup duration in nanoseconds
- `:target-execution-time` - Target time per sample in nanoseconds
- `:verbose` - Print detailed progress
- `:gc-before-sample` - Force GC before each sample

### Workflow 7: Handling Measurement Overhead

Control overhead estimation for consistency:

```clojure
(require '[criterium.core :as crit])

;; View current overhead estimate
@crit/estimated-overhead-cache
;; => 2.1e-9 (nanoseconds)

;; Force overhead recalculation
(crit/estimated-overhead!)
;; Recalculates measurement overhead

;; Set fixed overhead for consistency across JVM processes
(reset! crit/estimated-overhead-cache 2.5e-9)

;; Now all benchmarks use this overhead value
(quick-bench (+ 1 2))

;; Useful for:
;; - Comparing benchmarks across different JVM runs
;; - Consistent CI/CD benchmarks
;; - When overhead estimation is inaccurate
```

**Why overhead matters:**
- Timing code takes time
- For fast operations, overhead can be significant
- Inaccurate overhead causes negative times
- Recalculate if seeing negative times

### Workflow 8: Interpreting Results

Understand what benchmark output means:

```clojure
;; Example output:
;; Execution time mean : 10.234 µs
;; Execution time std-deviation : 1.234 µs
;; Execution time lower quantile : 9.123 µs ( 2.5%)
;; Execution time upper quantile : 12.456 µs (97.5%)
;; Overhead used : 2.1 ns
;;
;; Found 3 outliers in 60 samples (5.0000 %)
;;   low-severe   2 (3.3333 %)
;;   low-mild     1 (1.6667 %)
;; Variance from outliers : 13.8889 % Variance is moderately inflated by outliers

;; Interpretation:
;; - Mean: 10.234 µs is the average execution time
;; - Std dev: 1.234 µs shows variability (12% of mean)
;; - Quantiles: 95% of runs between 9.123-12.456 µs
;; - Outliers: 3 unusually fast runs (GC or other noise)
;; - Variance: Outliers slightly affect result reliability

;; Good benchmark (low variance):
;; Execution time mean : 10.0 µs
;; Execution time std-deviation : 0.1 µs  ; < 1% of mean
;; Variance from outliers : 0% ; No outliers

;; Bad benchmark (high variance):
;; Execution time mean : 10.0 µs
;; Execution time std-deviation : 5.0 µs  ; 50% of mean!
;; Variance from outliers : 73.4567 % ; Many outliers

;; When variance is high:
;; - Run full bench instead of quick-bench
;; - Check for background processes
;; - Close other applications
;; - Run multiple times and average
```

## Best Practices

**Do:**
- Use `quick-bench` for exploration, `bench` for accurate results
- Let benchmarks complete (don't interrupt)
- Close background applications during benchmarking
- Run multiple times to verify consistency
- Check for outliers and variance
- Use `with-progress-reporting` for long benchmarks
- Benchmark in a clean JVM (restart REPL if needed)
- Test realistic input sizes

```clojure
;; Good: Realistic input size
(quick-bench (reduce + (range 10000)))

;; Good: Multiple approaches compared
(do
  (println "Approach 1:")
  (quick-bench (into [] (range 1000)))
  (println "\nApproach 2:")
  (quick-bench (vec (range 1000))))

;; Good: Check variance
(bench (my-function))
;; Look for: "Variance from outliers : 0%"
```

**Don't:**
- Don't interrupt benchmarks mid-run
- Don't benchmark in a busy JVM (lots of other work)
- Don't ignore high variance warnings
- Don't compare benchmarks from different JVM sessions
- Don't benchmark trivial operations (overhead dominates)
- Don't forget warmup (Criterium handles this)
- Don't trust single quick-bench for critical decisions

```clojure
;; Bad: Too trivial (dominated by overhead)
(quick-bench (+ 1 2))
;; => May show negative or zero time

;; Bad: Not comparable (different JVM states)
;; Session 1:
(quick-bench (func1))
;; Session 2 (after restart):
(quick-bench (func2))

;; Good: Compare in same session
(do
  (quick-bench (func1))
  (quick-bench (func2)))
```

## Understanding Output

### Time Units

Criterium automatically chooses appropriate units:

| Unit | Full Name | Conversion |
|------|-----------|------------|
| ns | nanosecond | 1 / 1,000,000,000 sec |
| µs | microsecond | 1 / 1,000,000 sec |
| ms | millisecond | 1 / 1,000 sec |
| sec | second | 1 sec |
| min | minute | 60 sec |

**Typical ranges:**
- `< 1 µs` - Very fast (simple arithmetic, lookups)
- `1-100 µs` - Fast (small collections, string ops)
- `100 µs - 10 ms` - Medium (I/O, larger collections)
- `> 10 ms` - Slow (network, database, complex algorithms)

### Outlier Types

**Outlier severity:**
- **low-severe** - Much faster than expected (rare, GC avoided)
- **low-mild** - Somewhat faster (slight variance)
- **high-mild** - Somewhat slower (minor GC or interruption)
- **high-severe** - Much slower (major GC or external event)

**Outlier effect on variance:**
- `< 1%` - Unaffected (excellent)
- `1-10%` - Slightly inflated (good)
- `10-50%` - Moderately inflated (acceptable)
- `> 50%` - Severely inflated (unreliable, rerun)

## Common Issues

### Issue: Negative execution times

```clojure
(quick-bench (+ 1 2))
;; => Execution time mean : -0.5 ns
```

**Cause**: Operation too fast, overhead estimate too high

**Solution**: 
```clojure
;; Recalculate overhead
(require '[criterium.core :as crit])
(crit/estimated-overhead!)

;; Or benchmark something less trivial
(quick-bench (reduce + (range 100)))
```

### Issue: High variance warning

```clojure
(bench (my-function))
;; => Variance from outliers : 75.1234 % Variance is severely inflated by outliers
```

**Cause**: Inconsistent execution time (GC, background processes, etc.)

**Solution**:
```clojure
;; 1. Close background applications
;; 2. Restart JVM for clean state
;; 3. Run full bench instead of quick-bench
(bench (my-function))

;; 4. Check for external factors
;; - System under heavy load?
;; - Network/disk I/O involved?
;; - Non-deterministic code?
```

### Issue: Benchmark takes too long

```clojure
(bench (expensive-operation))
;; Takes 10+ minutes
```

**Solution**: Use quick-bench or custom options:
```clojure
;; Option 1: Use quick-bench
(quick-bench (expensive-operation))

;; Option 2: Reduce samples
(benchmark (expensive-operation)
           {:samples 30               ; Default 60
            :warmup-jit-period 1000000000})  ; 1s instead of 10s

;; Option 3: Use progress reporting to see what's happening
(with-progress-reporting
  (bench (expensive-operation) :verbose))
```

### Issue: Inconsistent results across runs

```clojure
;; Run 1
(quick-bench (my-function))
;; => 10.5 µs

;; Run 2
(quick-bench (my-function))
;; => 15.2 µs  ; Different!
```

**Solution**: Use full bench for consistency:
```clojure
;; Full benchmark is more stable
(bench (my-function))

;; Or run multiple quick-benches and average
(dotimes [_ 5]
  (quick-bench (my-function)))
```

### Issue: Benchmarking with side effects

```clojure
(def counter (atom 0))

(quick-bench (swap! counter inc))
;; Problem: State changes each iteration
```

**Solution**: Reset state or use pure functions:
```clojure
;; Option 1: Benchmark creation, not mutation
(quick-bench (atom 0))

;; Option 2: Reset in each iteration (slower but accurate)
(quick-bench
  (let [a (atom 0)]
    (swap! a inc)))

;; Option 3: Use immutable alternatives
(quick-bench (inc 0))
```

## Advanced Topics

### Custom Report Options

```clojure
(require '[criterium.core :refer [benchmark report-result]])

;; Get benchmark data
(def result (benchmark (reduce + (range 1000)) {}))

;; Custom reporting
(report-result result :verbose)  ; Verbose output
(report-result result :os)       ; Include OS info
(report-result result :runtime)  ; Include JVM info

;; All options
(report-result result :verbose :os :runtime)
```

### Round-Robin Benchmarking

Benchmark multiple expressions fairly:

```clojure
(require '[criterium.core :refer [benchmark-round-robin]])

;; Benchmark multiple approaches in alternating fashion
(benchmark-round-robin
  [(fn [] (reduce + (range 1000)))
   (fn [] (apply + (range 1000)))
   (fn [] (loop [i 0 sum 0]
            (if (< i 1000)
              (recur (inc i) (+ sum i))
              sum)))]
  {})

;; Ensures fair comparison by alternating between approaches
;; Reduces impact of JVM warmup and GC on comparison
```

### Statistical Functions

Access underlying statistical functions:

```clojure
(require '[criterium.core :refer [bootstrap outliers scale-time]])

;; Bootstrap confidence intervals for custom statistics
(def data [1.2e-6 1.3e-6 1.1e-6 1.4e-6 1.2e-6])

;; Find outliers
(outliers data)
;; => {:low-severe [], :low-mild [], :high-mild [1.4e-6], :high-severe []}

;; Scale time for human-readable output
(scale-time 1.234e-6)
;; => [1.234 1.0e-6 "µs"]
```

## Benchmarking Patterns

### Pattern 1: Before/After Optimization

```clojure
;; Before optimization
(println "Before:")
(quick-bench (old-implementation))

;; After optimization
(println "\nAfter:")
(quick-bench (new-implementation))

;; Compare results to verify improvement
```

### Pattern 2: Scaling Analysis

```clojure
;; Test how performance scales with input size
(doseq [n [100 1000 10000]]
  (println (str "\nSize " n ":"))
  (quick-bench (my-function (range n))))

;; Look for O(n), O(n²), etc. characteristics
```

### Pattern 3: Hotspot Identification

```clojure
;; Benchmark entire operation
(println "Full operation:")
(quick-bench (full-pipeline data))

;; Benchmark components
(println "\nStep 1:")
(quick-bench (step-1 data))

(println "\nStep 2:")
(quick-bench (step-2 (step-1 data)))

;; Identify bottleneck
```

## Related Tools

- **VisualVM** - JVM profiler for detailed analysis
- **YourKit** - Commercial profiler
- **clj-async-profiler** - Flame graph profiling
- **clj-memory-meter** - Memory usage measurement
- **time** - Built-in simple timing (less accurate)

## Resources

- GitHub: https://github.com/hugoduncan/criterium
- API docs: http://hugoduncan.github.io/criterium/
- Elliptic Group benchmarking: http://www.ellipticgroup.com/html/benchmarkingArticle.html
- Criterion (Haskell inspiration): http://hackage.haskell.org/package/criterion

## Summary

Criterium provides rigorous performance benchmarking for Clojure:

1. **JVM-Aware** - Handles JIT compilation and GC properly
2. **Statistical** - Multiple samples with mean, variance, confidence intervals
3. **Accurate** - Warmup periods, overhead compensation, GC isolation
4. **Outlier Detection** - Identifies and reports anomalous measurements
5. **Easy to Use** - Simple macros for quick or rigorous benchmarking

**Most common patterns:**

```clojure
;; Quick estimate
(quick-bench (my-function))

;; Accurate measurement
(bench (my-function))

;; With progress
(with-progress-reporting
  (bench (slow-function) :verbose))

;; Compare implementations
(do
  (println "Approach 1:")
  (quick-bench (approach-1))
  (println "\nApproach 2:")
  (quick-bench (approach-2)))

;; Programmatic access
(def result (quick-benchmark (my-function) {}))
(first (:mean result))  ; Get mean time
```

Perfect for optimization work, algorithm comparison, and performance regression testing in Clojure projects.
