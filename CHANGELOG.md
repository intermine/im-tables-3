## 0.10.1 (2020-06-25)

- Fix interleaved booting causing performance issues and race conditions in specific scenarios [#92](https://github.com/intermine/im-tables-3/pull/92)

## 0.10.0 (2020-06-11)

- Make im-tables-3 more feature complete [#79](https://github.com/intermine/im-tables-3/pull/79)
    - Project setup
        - Update dependencies
        - Run tests against local biotestmine
        - Improve dev experience by not rebooting table on changes
        - Fix README code example
    - Constraints
        - New searchable dropdown and calendar components
        - Filter constraint ops to those applicable to data type
        - Add IS NULL and IS NOT NULL
        - Filter manager for adding and modifying constraints and logic
    - Interface
        - Handle overly wide tables by displaying scrollbar
        - Fix numerical column summary title pluralisation
        - Column summary scrollbar for tbody instead of entire body
        - Do not add href to cells until we are sure URL is complete
    - Error handling
        - Views for No Results, Invalid Query, Server Error and Im-tables Crashed (error boundary)
        - Use async-flow for booting and handle invalid responses
        - Add save-list-failure event to be intercepted by BlueGenes
- Fix and improve histogram in numeric column summary [#82](https://github.com/intermine/im-tables-3/pull/82)
    - Linear/log scale toggle buttons (also added to regular column summary)
    - Min and max labels for X axis
    - Average line (with tooltip showing average)
    - Tooltips for buckets showing interval and count of values
- Fix styling problems when used in Bluegenes [#86](https://github.com/intermine/im-tables-3/pull/86)
    - Buttons with unreadable text
- Modals need a max height and scrollbar [#84](https://github.com/intermine/im-tables-3/issues/84)

## 0.9.0 (2019-11-01)

- Only show summary fields instead of all attributes in cell popovers [#56](https://github.com/intermine/im-tables-3/pull/56)
- Remove bower dependency [d427da5](https://github.com/intermine/im-tables-3/commit/d427da5e2091f88d1f6853d27953c458c4a55f05)
- Avoid showing empty cell popovers [#65](https://github.com/intermine/im-tables-3/pull/65)
- Fix dropdown selection in filter column not changeable [#65](https://github.com/intermine/im-tables-3/pull/65)
- Fix sort column not working at all [#65](https://github.com/intermine/im-tables-3/pull/65)
- Colour sort column icon arrows when sorted [#65](https://github.com/intermine/im-tables-3/pull/65)
- Include references in Add Columns tree [#65](https://github.com/intermine/im-tables-3/pull/65)
- Remove column summary limit of 1000 unique values (it will now summarize the top 1000 rows regardless) [#70](https://github.com/intermine/im-tables-3/pull/70)
- Show descriptive title in column summary [#70](https://github.com/intermine/im-tables-3/pull/70)
- Wait with additional HTTP requests until table is interacted with [#59](https://github.com/intermine/im-tables-3/pull/59) [#60](https://github.com/intermine/im-tables-3/pull/60) [#70](https://github.com/intermine/im-tables-3/pull/70)
- Fix export modal close button not working [#72](https://github.com/intermine/im-tables-3/pull/72)
- Add a skeleton loader for column summary [#73](https://github.com/intermine/im-tables-3/pull/73)
