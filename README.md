# Stately

A simple library for dealing with nested collections in global app state like re-frame written in Clojure.

> *DISCLAIMER* This library is in a very alpha state. API may break frequently until release.

## Usage

```clojure
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
```
