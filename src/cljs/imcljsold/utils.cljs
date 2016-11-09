(ns imcljsold.utils)

(defn missing-http?- [val] (not (re-find #"^https?://" val)))

(defn missing-service?- [val] (not (re-find #"/service$" val)))

(defn append- [text val] (str val text))

(defn cleanse-url
  "Ensures that a url starts with an http protocol and ends with /service"
  [url]
  (cond->> url
           (missing-http?- url) (str "http://")
           (missing-service?- url) (append- "/service")))

(defn sterilize-query [query]
  (-> query
      (update :select
              (fn [paths]
                (if (contains? query :from)
                  (mapv (fn [path]
                          (if (= (:from query) (first (clojure.string/split path ".")))
                            path
                            (str (:from query) "." path))) paths)
                  paths)))
      (update :orderBy (partial map (fn [{:keys [path] :as order}]
                                      (if (= (:from query) (first (clojure.string/split path ".")))
                                        order
                                        (assoc order :path (str (:from query) "." path))))))))



(defn map->xmlstr
  "Return an EDN map {:key1 val1 key2 val2} as an XML string.
  (map->xlmstr constraint {:key1 val1 key2 val2})
  => <constraint key1=val1 key2=val2 />"
  [elem m]
  (str "<" elem " " (reduce (fn [total [k v]] (str total (if total " ") (name k) "=" (str \" v \"))) nil m) "/>"))

(defn query->xml
  "Returns the stringfied XML representation of an EDN intermine query."
  [model query]
  (let [query           (sterilize-query query)
        head-attributes {:model     "genomic"
                         :name      (:name model)
                         :view      (clojure.string/join " " (:select query))
                         :sortOrder (clojure.string/join " " (flatten (map (juxt :path :direction) (:orderBy query))))}]
    (str
      "<query "
      (reduce (fn [total [k v]] (str total (if total " ") (name k) "=" (str \" v \"))) nil head-attributes)
      ">"
      (apply str (map (partial map->xmlstr "constraint") (:where query)))
      "</query>")))