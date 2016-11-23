(ns im-tables.db)

(def subclassquery
  {:description     "Returns MP terms whose names match the specified search terms.",
   :tags            ["im:aspect:Phenotype" "im:frontpage" "im:public"],
   :where           [{:path       "MPTerm.obsolete",
                      :op         "=",
                      :code       "B",
                      :editable   "false",
                      :switchable "false",
                      :switched   "LOCKED",
                      :value      "false"}
                     {:path       "MPTerm.name",
                      :op         "CONTAINS",
                      :code       "A",
                      :editable   "true",
                      :switchable "false",
                      :switched   "LOCKED",
                      :value      "hemoglobin"}],
   :name            "Lookup_MPhenotype",
   :title           "Lookup --> Mammalian phenotypes (MP terms)",
   :constraintLogic "A and B",
   :select          ["MPTerm.name" "MPTerm.identifier" "MPTerm.description"],
   :orderBy         [{:MPTerm.name "ASC"}],
   :model           {:name "genomic"}})

(def default-db
  {

   :service  {:root "www.flymine.org/query"}
   ;:query    {:from   "Gene"
   ;           :size   10
   ;           :select ["secondaryIdentifier"
   ;                    "symbol"
   ;                    "primaryIdentifier"
   ;                    "organism.name"
   ;                    "homologues.homologue.symbol"]
   ;           :where  [{:path  "Gene"
   ;                     :op    "IN"
   ;                     :value "esyN demo list"}
   ;                    {:path  "Gene.symbol"
   ;                     :op    "="
   ;                     :value "*a*"
   ;                     :code  "B"}]}

   :settings {:pagination {:start 0
                           :limit 10}
              :links {:vocab {:mine "flymine"}
                      :on-click nil
                      :url (fn [vocab] (str "#/reportpage/"
                                            (:mine vocab) "/"
                                            (:class vocab) "/"
                                            (:objectId vocab)))}}
   :cache    {:summaries {}
              :summary   {}
              :selection {}
              :overlay?  false
              :filters   {}
              :tree-view {:selection #{}}}})



