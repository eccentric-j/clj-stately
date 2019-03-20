(ns stately.core
  "Simple library for manipulating deeply nested collections in large trees
  such as re-frame."
  (:require [clojure.string :refer [split]]
            ;; For demo purposes
            [clojure.pprint :refer [pprint]]))

(defn- dynamic-key?
  [key]
  (re-find #"^[a-z]+:[_\w]+$" key))

(defn- find-key-for-value
  "Search an indexed collection for an item with a matching key-value pair.
  Takes a keyword to compare, a value to compare against and a collection.
  Returns either the index of the matching item or nil."
  [search-key search-value coll]
  (some->> coll
           (map-indexed #(vector %1 (get %2 search-key)))
           (filter #(= (second %) search-value))
           (first)
           (first)))

(defn- int-or-raw
  "Takes a value
  Returns a parsed integer or the value itself"
  [x]
  (try (Integer/parseInt x)
    (catch Exception e
      x)))

(defn- find-dynamic-path
  "Takes current path building state {:current map :paths [:key]}
  and a dynamic key string like id:5
  Returns updated path building state with updated paths and current state."
  [{:keys [paths current] :as state} dynamic-key]
  (let [[key value] (split dynamic-key #":")
        path (find-key-for-value (keyword key) (int-or-raw value) current)]
    (if (contains? current path)
      {:paths (conj paths path)
       :current (get current path)}
      (assoc state :paths nil))))

(defn- build-path
  "Takes current path building state {:current map :paths [:key]} and \"key\"
  string.
  Returns updated path building state.

  {:paths [:key|int]} when key-str could be located
  {:paths nil} when key-str could not be found"
  [{:keys [paths current] :as state} key-str]
  (let [key (keyword key-str)]
    (cond (nil? paths)           state
          (dynamic-key? key-str) (find-dynamic-path state key-str)
          (contains? current key)  {:paths (conj paths key)
                                    :current (get current key)}
          :else                    {:current nil
                                    :paths nil})))

(defn- path-str->vector
  "Takes a db map and a path-str like \"pets/id:5\".
  Returns "
  [db path-str]
  (->> (split path-str #"/")
       (reduce #(build-path %1 %2) {:paths [] :current db})
       (:paths)))

(defn- remove-at-index
  "Takes a collection and an index.
  Removes item at the given index.
  Returns new, updated collection."
  [coll idx]
  (->> coll
       (map-indexed vector)
       (remove #(= (first %) idx))
       (map second)))

(defn- smart-remove
  "Intelligently removes an item depending on type.
  Takes a collection or map and a target index or keyword.
  Returns the collection with the target removed."
  [coll target]
  (cond (sequential? coll) (remove-at-index coll target)
        (map? coll)        (dissoc coll target)
        ;; Add more strategies as needed
        :else              coll))


;; Public API

(defn append-at
 "Append an item to a deep collection.
  Takes a db map with nested collections, a path-str like \"pets/id:5\", and
  an entry to append.
  Returns updated db state

  Example:
  (append-at {:pets [{:id 1 :name \"Kitty\"}]} \"pets\" {:id 2 :name \"Puppy\"})
  ;; => {:pets [{:id 1 :name \"Kitty\"}
                {:id 2 :name \"Puppy\"}]}"
 [db path-str entry]
 (if-let [paths (path-str->vector db path-str)]
  (update-in db paths conj entry)
  db))

(defn set-at
 "Replaces entry at the path-str with the new entry.
  Takes a db map with nested collections, a path-str like \"pets/id:5\", and
  a value to replace the entry with.
  Returns updated db state.

  Example:
  (set-at {:pets [{:id 1 :name \"Kitty\"}]} \"pets/id:1/name\" \"Cat\")
  ;; => {:pets [{:id 1 :name \"Cat\"}]}"
 [db path-str entry]
 (if-let [paths (path-str->vector db path-str)]
  (update-in db paths (constantly entry))
  db))

(defn update-at
 "Merges a map into the map at a given path string.
  Takes a db map, a path-str to point to a collection, and some data to merge.

  Example:
  (update-at {:pets [{:id 1 :name \"Kitty\"}]} \"pets/id:1\" {:name \"Cat\"
                                                              :color :black})
  ;; => {:pets [{:id 1 :name \"Cat\" :color :black}]}"
  [db path-str data]
  (if-let [paths (path-str->vector db path-str)]
    (update-in db paths merge data)
    db))

(defn remove-at
 "Removes an item from the collection at a given target path string.
  Takes a db map and a path-str pointing to an item to remove.

  Example:
  (remove-at {:pets [{:id 1 :name \"Bad Dog\"}]} \"pets/id:1\")
  ;; => {:pets []}"
  [db path-str]
  (if-let [paths (path-str->vector db path-str)]
    (let [coll-paths (butlast paths)
          item-idx (last paths)]
      (update-in db coll-paths smart-remove item-idx))
    db))

;; Usage

(def app-state {:pets [{:id 1
                        :name "Red"
                        :type "cat"
                        :gender :f}
                       {:id 2
                        :name "Blue"
                        :type "dog"
                        :gender :m}
                       {:id 3
                        :name "Pikachu"
                        :type "pokemon"
                        :gender nil}]})


(defn -main
  [& args]
  (println "Append:")
  (pprint (append-at app-state "pets" {:id 4
                                       :name "Gary"
                                       :type "yeti"
                                       :gender nil}))

  (println "\nSet:")
  (pprint (set-at app-state "pets/id:2/name" " +PURPLE+ "))

  (println "\nUpdate:")
  (pprint (update-at app-state "pets/id:3" {:name "Charmander"
                                            :gender "F"}))
  (println "\nRemove:")
  (pprint (remove-at app-state "pets/type:pokemon")))

