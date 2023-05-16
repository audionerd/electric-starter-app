(ns app.todo-list
  (:require contrib.str
            #?(:clj [datascript.core :as d]) ; database on server
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]))

#?(:clj (defonce !conn (d/create-conn {}))) ; database on server
(e/def db) ; injected database ref; Electric defs are always dynamic

(e/defn TodoItem [id]
  (e/server
    (let [e (d/entity db id)
          status (:task/status e)]
      (e/client
        (dom/div
          (ui/checkbox
            (case status :active false, :done true)
            (e/fn [v]
              (e/server
                (d/transact! !conn [{:db/id id
                                     :task/status (if v :done :active)}])
                nil))
            (dom/props {:id id :class "mr-2"}))
          (dom/label (dom/props {:for id}) (dom/text (e/server (:task/description e)))))))))

(e/defn InputSubmit [F]
  ; Custom input control using lower dom interface for Enter handling
  (dom/input (dom/props {:class "my-2 border px-2 py-1 rounded" :placeholder "Buy milk"})
    (dom/on "keydown" (e/fn [e]
                        (when (= "Enter" (.-key e))
                          (when-some [v (contrib.str/empty->nil (-> e .-target .-value))]
                            (new F v)
                            (set! (.-value dom/node) "")))))))

(e/defn TodoCreate []
  (e/client
    (InputSubmit. (e/fn [v]
                    (e/server
                      (d/transact! !conn [{:task/description v
                                           :task/status :active}])
                      nil)))))

#?(:clj (defn todo-count [db]
          (count
            (d/q '[:find [?e ...] :in $ ?status
                   :where [?e :task/status ?status]] db :active))))

#?(:clj (defn todo-records [db]
          (->> (d/q '[:find [(pull ?e [:db/id :task/description]) ...]
                      :where [?e :task/status]] db)
            (sort-by :task/description))))

(e/defn Todo-list []
  (e/server
    (binding [db (e/watch !conn)]
      (e/client
        (dom/div
          (dom/props {:class "w-96 mx-auto py-3 px-3 pt-12"})
          (dom/h1
            (dom/props {:class "text-xl font-bold"})
            (dom/text "Minimal Todo List"))
          (dom/p
            (dom/props {:class "text-sm text-gray-400"})
            (dom/text "It’s multiplayer! Try two tabs 😀"))
          (dom/div
            (dom/props {:class "my-4"})
            (TodoCreate.)
            (dom/div (dom/props {:class "my-4"})
              (e/server
                (e/for-by :db/id [{:keys [db/id]} (todo-records db)]
                  (TodoItem. id))))
            (dom/p (dom/props {:class "counter text-sm text-gray-400"})
              (dom/span (dom/props {:class "count"}) (dom/text (e/server (todo-count db))))
              (dom/text " items left"))))
        (dom/script (dom/props {:src "https://cdn.tailwindcss.com"}))))))