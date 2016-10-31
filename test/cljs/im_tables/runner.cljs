(ns im-tables.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [im-tables.core-test]))

(doo-tests 'im-tables.core-test)
