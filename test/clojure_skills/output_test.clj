(ns clojure-skills.output-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure-skills.output :as output]
            [clojure.data.json :as json]
            [clojure.string :as str]))

(defn parse-json-output
  "Parse JSON output string into Clojure data structure."
  [output-str]
  (when-not (str/blank? output-str)
    (json/read-str output-str :key-fn keyword)))

(defn json-output?
  "Check if string is valid JSON."
  [s]
  (try
    (parse-json-output s)
    true
    (catch Exception _
      false)))

(deftest test-output-data
  (testing "output-data produces valid JSON"
    (let [test-data {:type :test-output
                     :count 2
                     :items [{:id 1 :name "item1"}
                             {:id 2 :name "item2"}]}
          output (with-out-str (output/output-data test-data))
          parsed (parse-json-output output)]
      (is (json-output? output)
          "Output should be valid JSON")
      (is (= "test-output" (:type parsed))
          "Type field should be preserved as string")
      (is (= 2 (:count parsed))
          "Count field should be preserved")
      (is (= 2 (count (:items parsed)))
          "Items array should have correct length")))

  (testing "output-data handles empty collections"
    (let [test-data {:type :empty-list
                     :count 0
                     :items []}
          output (with-out-str (output/output-data test-data))
          parsed (parse-json-output output)]
      (is (= "empty-list" (:type parsed)))
      (is (= 0 (:count parsed)))
      (is (empty? (:items parsed)))))

  (testing "output-data handles nested structures"
    (let [test-data {:type :nested
                     :data {:metadata {:created-at "2025-01-01"
                                       :updated-at "2025-01-02"}
                            :content "test content"}}
          output (with-out-str (output/output-data test-data))
          parsed (parse-json-output output)]
      (is (= "nested" (:type parsed)))
      (is (= "test content" (get-in parsed [:data :content])))
      (is (= "2025-01-01" (get-in parsed [:data :metadata :created-at])))))

  (testing "output-data handles nil values"
    (let [test-data {:type :with-nil
                     :nullable-field nil
                     :present-field "value"}
          output (with-out-str (output/output-data test-data))
          parsed (parse-json-output output)]
      (is (= "with-nil" (:type parsed)))
      (is (nil? (:nullable-field parsed)))
      (is (= "value" (:present-field parsed)))))

  (testing "json-output is an alias for output-data"
    (let [test-data {:type :alias-test}
          output1 (with-out-str (output/output-data test-data))
          output2 (with-out-str (output/json-output test-data))]
      (is (= output1 output2)
          "Both functions should produce identical output"))))
