(ns im-tables.db)

(def default-db
  {:name     "im-tables"
   :service {:root "www.flymine.org/query"}
   :settings {:pagination {:start 0
                           :limit 10}}
   :query    {:from   "Gene"
              :select ["secondaryIdentifier"
                       "symbol"
                       "primaryIdentifier"
                       "organism.name"]
              :where  [{:path  "Gene"
                        :op    "IN"
                        :value "FlyMine_AlzheimersUseCase"}]}
   :cache {:summaries {}}})
