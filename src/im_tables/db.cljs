(ns im-tables.db)

(def subclassquery
  {:description "Returns MP terms whose names match the specified search terms.",
   :tags ["im:aspect:Phenotype" "im:frontpage" "im:public"],
   :where [{:path "MPTerm.obsolete",
            :op "=",
            :code "B",
            :editable "false",
            :switchable "false",
            :switched "LOCKED",
            :value "false"}
           {:path "MPTerm.name",
            :op "CONTAINS",
            :code "A",
            :editable "true",
            :switchable "false",
            :switched "LOCKED",
            :value "hemoglobin"}],
   :name "Lookup_MPhenotype",
   :title "Lookup --> Mammalian phenotypes (MP terms)",
   :constraintLogic "A and B",
   :select ["MPTerm.name" "MPTerm.identifier" "MPTerm.description"],
   :orderBy [{:MPTerm.name "ASC"}],
   :model {:name "genomic"}})

(def outer-join-query {:from "Gene"
                       :select ["symbol"
                                "secondaryIdentifier"
                                "primaryIdentifier"
                                "organism.name"
                                "publications.firstAuthor"
                                "dataSets.name"]
                       :joins ["Gene.publications"]
                       :size 10
                       :sortOrder [{:path "symbol"
                                    :direction "ASC"}]
                       :where [
                               {:path "secondaryIdentifier"
                                :op "="
                                :value "AC3.1*" ;AC3*
                                :code "A"}
                               ]})

(def list-query {:title "esyN demo list"
                 :from "Gene"
                 :select ["Gene.secondaryIdentifier" "Gene.symbol" "Gene.primaryIdentifier" "Gene.organism.name"]
                 :where [{:path "Gene", :op "IN", :value "esyN demo list"}]})

(def default-db
  {

   ;:service  {:root "www.flymine.org/query"}
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

   ;:service {:model "TESTMODEL"}

   :settings {:buffer 2
              :cdn "www.intermine.org/cdn2"
              :pagination {:start 0
                           :limit 20}
              :codegen {:lang "js"}
              :data-out {:selected-format :tsv
                         :accepted-formats {:tsv :all
                                            :csv :all
                                            :fasta [:Gene :Protein]}}
              :links {:vocab {:mine "flymine"}
                      :on-click nil
                      :url (fn [vocab] (str "#/reportpage/"
                                            (:mine vocab) "/"
                                            (:class vocab) "/"
                                            (:objectId vocab)))}}
   :cache {:summaries {}
           :summary {}
           :selection {}
           :overlay? false
           :filters {}
           :tree-view {:selection #{}}}})
