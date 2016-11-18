(ns im-tables.db)

(def default-db
  {:service  {:root "www.flymine.org/query"}
   :query    {:from   "Gene"
              :size   10
              :select ["secondaryIdentifier"
                       "symbol"
                       "primaryIdentifier"
                       "organism.name"]
              :where  [{:path  "Gene"
                        :op    "IN"
                        :value "PL FlyTF_trusted_TFs"
                        :code  "A"}
                       {:path  "Gene.symbol"
                        :op    "="
                        :value "*a*"
                        :code  "B"}]}
   :settings {:pagination {:start 0
                           :limit 10}
              :links {:vocab {:mine "flymine"}
                      :url (fn [vocab] (str "/#/reportpage/"
                                            (:mine vocab) "/"
                                            (:class vocab) "/"
                                            (:objectId vocab) ))}}
   :cache    {:summaries {}
              :summary   {}
              :selection {}
              :overlay?  false
              :filters   {}
              :tree-view {:selection #{}}}})
