## 0.13.0 (2021-03-26)

- Fix bug allowing a filter to duplicate itself and replace one for other column
- Add column controls (remove, filters, summary) to outer join subtables
- Remove controls that shouldn't apply to outer joined paths
- Fix column filter menu closing when selecting option in dropdown
- Fix displaying and filtering of boolean value
- Fix crash when inputting invalid regexp character into column summary search input

## 0.12.0 (2021-02-11)

- Make cells in outer join subtables clickable
- Add input for description when saving list
- Support exporting with compression and Frictionless
    - Changes how exports are done from blobs and JS to anchor element with href GET that returns `content-disposition: attachment` (same as used by original im-tables)

## 0.11.0 (2020-11-25)

[#106](https://github.com/intermine/im-tables-3/pull/106)
- Default to Python for codegen instead of JS
- Handle subclasses specified with type constraints
    - Fix empty column headers for paths unique to subclass
    - Show and allow editing of type constraints in filter manager
    - Fix column summary crashing for subclass attribute
- Filter suggestions are now fetched with active constraints (meaning all should give results)
- Make count-deconstruction more robust (not failing due to irrelevant sortOrder)
- Improvements to *Add columns* modal
    - Show attributes/references/collections of type constrained class
    - Pre-expand tree to active views
- UI fixes
    - Fix constraint selector disappearing at edges of modal
    - Use text input instead of selector with no options, when there are too many possible values
- Add XML to *Generate code* modal
- Fix typing enter in column summary filter causing page reload
- Fix suggestions not fetched when first opening filter manager
- Support picking items to save to list
- Fix JS code in *Generate code* modal containing frivolous query keys for templates
- Improve styling of dashboard and clean up messy markup
- `:compact` setting to hide dashboard buttons until expanded
- Rename CSS to `im-tables.css` so dependants can pick it up (removes the need for manually copying CSS)
- Pagination fixes
    - Fix rows per page not showing a custom limit passed through settings
    - Fix jump to last page sending you to nonexistent page with no results, when total results is a multiple of page limit
    - Fix buggy behaviour when page limit is set to number not multiple of 10
    - Fix changing page limit (rows per page) sometimes leading to *decimal* pages

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
