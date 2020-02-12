(ns metabase.query-processor.middleware.parameters-test
  "Testings to make sure the parameter substitution middleware works as expected. Even though the below tests are
  SQL-specific, they still confirm that the middleware itself is working correctly."
  (:require [clojure.test :refer :all]
            [metabase.driver :as driver]
            [metabase.mbql.normalize :as normalize]
            [metabase.query-processor.middleware.parameters :as parameters]
            [metabase.test.data :as data]))

(deftest move-top-level-params-to-inner-query-test
  (is (= {:type   :native
          :native {:query "WOW", :parameters ["My Param"]}}
         (#'parameters/move-top-level-params-to-inner-query
          {:type :native, :native {:query "WOW"}, :parameters ["My Param"]}))))

(defn- substitute-params [query]
  (driver/with-driver :h2
    ((parameters/substitute-parameters identity) (normalize/normalize query))))

(deftest expand-mbql-top-level-params-test
  (testing "can we expand MBQL params if they are specified at the top level?"
    (is (= (data/mbql-query venues
             {:aggregation [[:count]]
              :filter      [:= $price 1]})
           (substitute-params
            (data/mbql-query venues
             {:aggregation [[:count]]
              :parameters  [{:name "price", :type :category, :target $price, :value 1}]}))))))

(deftest expand-native-top-level-params-test
  (testing "can we expand native params if they are specified at the top level?"
    (is (= (data/query nil
             {:type   :native
              :native {:query "SELECT * FROM venues WHERE price = 1;", :params []}})
           (substitute-params
            (data/query nil
              {:type       :native
               :native     {:query         "SELECT * FROM venues WHERE price = {{price}};"
                            :template-tags {"price" {:name "price", :display-name "Price", :type :number}}}
               :parameters [{:type "category", :target [:variable [:template-tag "price"]], :value "1"}]}))))))

(deftest expand-mbql-source-query-params-test
  (testing "can we expand MBQL params in a source query?"
    (is (= (data/mbql-query venues
             {:source-query {:source-table $$venues
                             :filter       [:= $price 1]}
              :aggregation  [[:count]]})
           (substitute-params
            (data/mbql-query venues
              {:source-query {:source-table $$venues
                              :parameters   [{:name "price", :type :category, :target $price, :value 1}]}
               :aggregation  [[:count]]}))))))

(deftest expand-native-source-query-params-test
  (testing "can we expand native params if in a source query?"
    (is (= (data/mbql-query nil
             {:source-query {:native "SELECT * FROM categories WHERE name = ?;"
                             :params ["BBQ"]}})
           (substitute-params
            (data/mbql-query nil
              {:source-query {:native         "SELECT * FROM categories WHERE name = {{cat}};"
                              :template-tags {"cat" {:name "cat", :display-name "Category", :type :text}}
                              :parameters    [{:type "category", :target [:variable [:template-tag "cat"]], :value "BBQ"}]}}))))))

(deftest expand-mbql-join-params-test
  (testing "can we expand MBQL params in a JOIN?"
    (is (= (data/mbql-query venues
             {:aggregation [[:count]]
              :joins       [{:source-query {:source-table $$categories
                                            :filter       [:= $categories.name "BBQ"]}
                             :alias        "c"
                             :condition    [:= $category_id &c.categories.id]}]})
           (substitute-params
            (data/mbql-query venues
              {:aggregation [[:count]]
               :joins       [{:source-table $$categories
                              :alias        "c"
                              :condition    [:= $category_id &c.categories.id]
                              :parameters   [{:type "category", :target $categories.name, :value "BBQ"}]}]}))))))

(deftest expand-native-join-params-test
  (testing "can we expand native params in a JOIN?"
    (is (= (data/mbql-query venues
             {:aggregation [[:count]]
              :joins       [{:source-query {:native "SELECT * FROM categories WHERE name = ?;"
                                            :params ["BBQ"]}
                             :alias        "c"
                             :condition    [:= $category_id &c.*categories.id]}]})
           (substitute-params
            (data/mbql-query venues
              {:aggregation [[:count]]
               :joins       [{:source-query {:native        "SELECT * FROM categories WHERE name = {{cat}};"
                                             :template-tags {"cat" {:name "cat", :display-name "Category", :type :text}}
                                             :parameters    [{:type "category", :target [:variable [:template-tag "cat"]], :value "BBQ"}]}
                              :alias        "c"
                              :condition    [:= $category_id &c.*categories.id]}]}))))))

(deftest expand-multiple-mbql-params-test
  (testing "can we expand multiple sets of MBQL params?"
    (is (=
         (data/mbql-query venues
           {:source-query {:source-table $$venues
                           :filter       [:= $price 1]}
            :aggregation  [[:count]]
            :joins        [{:source-query {:source-table $$categories
                                           :filter       [:= $categories.name "BBQ"]}
                            :alias        "c"
                            :condition    [:= $category_id &c.categories.id]}]})
         (substitute-params
          (data/mbql-query venues
            {:source-query {:source-table $$venues
                            :parameters   [{:name "price", :type :category, :target $price, :value 1}]}
             :aggregation  [[:count]]
             :joins        [{:source-table $$categories
                             :alias        "c"
                             :condition    [:= $category_id &c.categories.id]
                             :parameters   [{:type "category", :target $categories.name, :value "BBQ"}]}]}))))))

(deftest expand-multiple-mbql-params-in-joins-test
  ;; (This is dumb. Hopefully no one is creating queries like this.  The `:parameters` should go in the source query
  ;; instead of in the join.)
  (testing "can we expand multiple sets of MBQL params with params in a join and the join's source query?"
    (is (= (data/mbql-query venues
             {:aggregation [[:count]]
              :joins       [{:source-query {:source-table $$categories
                                            :filter       [:and
                                                           [:= $categories.name "BBQ"]
                                                           [:= $categories.id 5]]}
                             :alias        "c"
                             :condition    [:= $category_id &c.categories.id]}]})
           (substitute-params
            (data/mbql-query venues
              {:aggregation [[:count]]
               :joins       [{:source-query {:source-table $$categories
                                             :parameters   [{:name "id", :type :category, :target $categories.id, :value 5}]}
                              :alias        "c"
                              :condition    [:= $category_id &c.categories.id]
                              :parameters   [{:type "category", :target $categories.name, :value "BBQ"}]}]}))))))
