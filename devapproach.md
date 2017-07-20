# im-tables 3 dev approach

IM tables 3 is designed to exist standalone or embedded inside another [re-frame](https://github.com/Day8/re-frame) application such as [BlueGenes](https://github.com/intermine/redgenes), InterMine's new UI.

From here on, we're assuming you have familiarity with re-frame.

Maintaining a re-frame app inside another app-db presented some challenges, particularly with regards to app-db. To work around the possibility of the embedded app-db over-writing the parent app-db or vice-versa, all db entries are "sandboxed" under a keyword specific to the im-table instance.

Practically speaking, this means two things for you:
- When you write event handlers, you'll always need to apply the `(sandbox)` interceptor. Example:


    (reg-event-db
      :show-overlay
      (sandbox) ;<---- you'll need to include this interceptor in all event handlers.
      (fn [db [_ loc]]
        (assoc-in db [:cache :overlay?] true)))
- The first argument of event handlers and subscriptions needs to be `loc`. This is simply passing around the sandbox location keyword we mentioned earlier on, and should be available as a variable in all existing functions. **Don't ignore it** - make sure to pass it on to all new functions and handlers / subs that you write. If you forget this rule and pass other things as the first argument, **Strange Things will happen**, and im-tables-3 won't work properly in RedGenes.

**Example event dispatch and function:**


    (dispatch [:prep-modal loc ;<--- loc is the first arg after the name
      (generate-dialog loc     ;<--- loc is *also* being passed as an argument to a function
                    {:query query
                     :type  class})])  

**Example subscription:**

    (subscribe [:assets/model loc])

## Initialising im-tables-3 inside another reframe app: 

When initialising an im-table, make sure to provide a keyword or set of keywords to sandbox im-tables with. For example, see the bluegenes list results page: https://github.com/intermine/redgenes/blob/87eaafcf8b8ed3ccbd4e36c0294752b88ccfcbd7/src/cljs/redgenes/sections/results/views.cljs#L48

It may look something like this: 

    [tables/main [:results :fortable]]]

In this case, the `loc` that gets passed around inside im-tables will be `:results :fortable`, and in bluegenes when you look in the app db, you'll see all the im-tables settings nested under these two keys. 
