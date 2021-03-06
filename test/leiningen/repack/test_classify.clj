(ns leiningen.repack.test-classify
  (:use midje.sweet)
  (:require [leiningen.repack.classify :refer :all]
            [clojure.java.io :as io]))

(def ^:dynamic *hara-src-dir*
  (.getAbsolutePath (io/as-file "example/hara/src")))

(fact "name->path"
  (name->path "hello.world")
  => "hello/world"

  (name->path "clj-time.core")
  => "clj_time/core")

(fact "list-clojure-files"
  (count (list-clojure-files *hara-src-dir*)) => 18)

(fact "submodule-file?"
  (submodule-file? (io/as-file (str *hara-src-dir* "/hara/common.clj"))
                   *hara-src-dir*
                   "hara"
                   ["common"])
  => false

  (submodule-file? (io/as-file (str *hara-src-dir* "/hara/checkers.clj"))
                   *hara-src-dir*
                   "hara"
                   ["common"])
  => true)

(fact "classify-file"
  (classify-file (io/as-file (str *hara-src-dir* "/hara/common.clj"))
                 *hara-src-dir*
                 "hara"
                 1)
  => "common"

  (classify-file (io/as-file (str *hara-src-dir* "/hara/common/control.clj") )
                 *hara-src-dir*
                 "hara"
                 1)
  => "common"

  (classify-file (io/as-file (str *hara-src-dir* "/hara/common/control.clj"))
                 *hara-src-dir*
                 "hara"
                 2)
  => "common.control")

(fact "grab-namespaces"
  (grab-namespaces
   '(:require [clojure.core]))
  => '(clojure.core))

(fact "grab-classes"
  (grab-classes
   '(:import [clojure.core ILookup]
             [java.util Date]))
  => '(clojure.core.ILookup java.util.Date))

(fact "read-file-namespace"
  (read-file-namespace
   (io/as-file (str *hara-src-dir* "/hara/common.clj")))
  => (just {:ns 'hara.common
            :file anything
            :dep-namespaces '(hara.import),
            :dep-classes ()})

  (read-file-namespace
   (io/as-file (str *hara-src-dir* "/hara/common/collection.clj")))
  => (just {:ns 'hara.common.collection
            :file anything
            :dep-namespaces '(clojure.set hara.common.error hara.common.fn hara.common.types),
            :dep-classes ()}))

(fact "split-project-files"
  (keys (second (split-project-files *hara-src-dir* "hara" 1 [])))
  => '("checkers" "collection" "common" "import" "state")

  (keys (second (split-project-files *hara-src-dir* "hara" 1 ["common"])))
  => '("checkers" "collection" "import" "state"))

(fact "classify-modules"
  (-> (split-project-files *hara-src-dir* "hara" 1 [])
      (second)
      (classify-modules)
      (get "common")
      :files
      (->> (map (fn [x] (.getPath x))))
      sort)
  =>
  (cons
   (str *hara-src-dir* "/hara/common.clj")
   (map #(str *hara-src-dir* "/hara/common/" %)
        ["collection.clj" "constructor.clj" "control.clj" "debug.clj" "error.clj" "fn.clj"
         "interop.clj" "keyword.clj" "lettering.clj" "string.clj" "thread.clj" "types.clj"]))

  (-> (split-project-files *hara-src-dir* "hara" 1 [])
      (second)
      (classify-modules)
      (get "state")
      (dissoc :items :files))
  => '{:package "state",
       :namespaces #{hara.state},
       :dep-namespaces #{hara.common.types hara.common.fn clojure.string},
       :dep-classes #{}}

  (-> (split-project-files *hara-src-dir* "hara" 1 [])
      (second)
      (classify-modules)
      (get "common")
      (dissoc :items :files))
  => '{:package "common", :namespaces #{hara.common.control hara.common.types hara.common.lettering hara.common.fn hara.common.keyword hara.common.collection hara.common.error hara.common hara.common.constructor hara.common.thread hara.common.debug hara.common.interop hara.common.string},
       :dep-namespaces #{clojure.string clojure.set hara.import}, :dep-classes #{}}

  (-> (split-project-files *hara-src-dir* "hara" 1 [])
      (second)
      (classify-modules)
      (create-package-lookup))
  => '{hara.common.control "common", hara.common.types "common", hara.checkers "checkers", hara.common.lettering "common", hara.common.fn "common", hara.common.keyword "common", hara.common.collection "common", hara.state "state", hara.common.error "common", hara.common "common", hara.collection.hash-map "collection", hara.common.constructor "common", hara.common.thread "common", hara.import "import", hara.common.debug "common", hara.common.interop "common", hara.collection.data-map "collection", hara.common.string "common"}


)
(:dependencies (leiningen.core.project/unmerge-profiles
                (leiningen.core.project/read "project.clj") [:default]))
'([im.chit/korra "0.1.0"])
