(ns im-tables.db)

(def default-db
  {:name     "im-tables"
   :service  {:root "www.flymine.org/query"}
   :query    {:from   "Gene"
              :select ["secondaryIdentifier"
                       "symbol"
                       "primaryIdentifier"
                       "organism.name"]
              :where  [{:path  "Gene"
                        :op    "IN"
                        :value "FlyMine_AlzheimersUseCase"}]}
   :settings {:pagination {:start 0
                           :limit 10}}
   :cache    {:summaries {}
              :summary   {}
              :selection {}
              :tree-view {:selection #{}}}})
