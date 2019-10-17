
# im-tables 3 dev

A result viewer for Intermine data.

Development goals
* Simplify im-tables for future developers
* Build in clojure for easy integration with Intermine's new UI
* Decrease file size (current im-tables is around 1.2mb uncompressed)
* See [dev approach](devapproach.md) before attempting to make code changes.

## Usage from within ClojureScript

To render an im-table, simply mount the component and pass in the relevant values:

```clojure
[im-tables.views.core/main {:location [:some-location :within-app-db :to-store-values]
                            :service {:root "beta.flymine.org/beta"
                                      :model some-intermine-model         ; Optional
                                      :summary-fields some-summary-fields ; Optional
                                      }
                            :settings {:pagination {:limit 10}}
                            :response some-response-of-tablerows          ; Optional
                            :query {:from "Gene"
                                    :select ["symbol"
                                             "secondaryIdentifier"
                                             "primaryIdentifier"
                                             "organism.name"
                                             "dataSets.name"]
                                    :where [{:path "Gene.symbol"
                                             :op "LIKE"
                                             :value "M*"}]}}]
```

Some of the values are optional, and if supplied they will not be fetched when the component is mounted. This allows you to fetch a resource once and share it across components, or to render a table of query results that have already been fetched.

## Development

There are just a few core concepts that must be understood before developing on the project. Seriously. Keeping these in mind will save you money spent on headache medication.

#### 1. im-tables is not just a React component, it's also an application., and `:location` is everything
It uses re-frame as an in-memory database to store its values, and integrating a re-frame application into a parent re-frame application can be tricky. Why? If a parent application (BlueGenes) mounts two table components, events fired from one table to store values in app-db will override the other's state.

This makes the `:location` key of the options map crucial. It is a vector of your choosing, and it tells any given table (or tables!) to only update values in that sublocation of app-db. The `:location` value should be passed to *every single view* in im-tables, and then passed to _every single event handler_ as its first argument. Same goes for subscriptions. This is a manual process and no checks can be made to make this happen. If an event-handler using the `sandbox` interceptor is not passed a location vector as its first argument then you will likely not see your updates because they're being made to the `nil` key in app-db.

The sandbox interceptor extracts the sublocation of app-db and provides it to your event handler as if it were at the top level. This allows you to write your event handler functions as if im-tables was a standalone application rather than repeatedly reaching into a specific location in app-db. For example:

Using the `sandbox` interceptor, the following event can be written as such:

```clojure
(reg-event-db
  :show-overlay
  (sandbox)
  (fn [db [_ location value]]
    (assoc-in db [:cache :overlay?] value)))
```

Whereas a version without the `sandbox` interceptor is more verbose:
```clojure
(reg-event-db
  :show-overlay
  (sandbox)
  (fn [db [_ location value]]
    (update-in db location assoc-in [:cache :overlay?] value)))
```
That's not a huge for simple handler functions, but it pays out when you have more complex ones that use `get-in` and `update-in` to access values in many parts of your app-db, and the function can be tested without having to prepare a mock app-db with nested values.

Subscriptions don't have middleware so they need to take the table's app-db location into account:

```
(defn glue [path remainder-vec]
  (reduce conj (or path []) remainder-vec))

(reg-sub
  :main/query-response
  (fn [db [_ location]]
    (get-in db (glue location [:response]))))
```

#### 2. The location is used by most subscriptions and is often bound in a component's outermost closure.

Another opportunity for a headache! Form 2 components are functions that return functions. When the component updates, the returned function is called to update the view. Values bound to the outermost function will never return anything but their original value. In this example `cake` **will never change**.

```clojure
; The component WILL NOT update when cake changes
(defn [cake]
  (fn []
    [:h1 cake]))

; ...whereas this component will
(defn []
  (fn [cake]
    [:h1 cake]))
```
You will often see location passed to the outermost function, but don't be caught in the trap of binding more volatile values there as well.

#### 3. The view is optimised to render quickly

When a table first loads it only renders the first page of the query results as bog standard html with minimal components. This allows many tables to be rendered to, say, a report page while reducing load on the browser. Mousing over the table triggers a change that forces the table into "React" mode when the table cells become more complex.

## Building

### Initial setup

If you want to run the unit tests, you'll need Node.js and npm. Once these have been installed, run the following.

```
npm install
```

### Quickstart

These commands are explained in more depth below, but if you know what you want here's a quick reference of the most useful ones.

```
lein dev         # start dev server with hot-reloading
lein repl        # start dev server with hot-reloading and nrepl (no clean or css)
lein deploy      # build prod release and deploy to clojars

lein format      # run cljfmt to fix code indentation
lein kaocha      # run unit tests
```

### Compile css:

Compile css file once.

```
lein less once
```

Automatically recompile css file on change.

```
lein less auto
```

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3448](http://localhost:3448).

### Run tests:

Make sure to first run `npm install` to install the dependencies required to run the unit tests. You can also add `--watch` to the command below to automatically re-run tests when saving files.

```
lein kaocha
```

## Production Build

### Deploying to Heroku

```
lein clean
lein uberjar
```

That should compile the clojurescript code first, and then create the standalone jar.

When you run the jar you can set the port the ring server will use by setting the environment variable PORT.
If it's not set, it will run on port 3000 by default.

To deploy to heroku, first create your app:

```
heroku create
```

Then deploy the application:

```
git push heroku master
```

To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```

### Releasing a new version

The release process is a combination of the above commands, with some additional steps. Generally, you'll want to do the following.

1. Update the version number in **project.clj**.
1. Commit this change and tag it using `git tag -a v1.0.0 -m "Release v1.0.0"`, replacing *1.0.0* with your version number.
1. Push your commit and tag using `git push origin` followed by `git push origin v1.0.0` (again replace *1.0.0* with your version number). Make sure that you push to the intermine repository, not just your fork!
1. Deploy a new uberjar to Clojars with `lein deploy`.
