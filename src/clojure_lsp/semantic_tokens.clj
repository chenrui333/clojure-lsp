(ns clojure-lsp.semantic-tokens)

(def token-types
  [:function
   :macro])

(def token-types-str
  (->> token-types
       (map name)
       vec))

(def token-modifiers
  [])

(def token-modifier -1)

(defn ^:private usage->absolute-token
  [{:keys [row col end-col]}
   token-type]
  [(dec row)
   (dec col)
   (- end-col col)
   (.indexOf token-types token-type)
   token-modifier])

(defn ^:private full-absolute
  [usages]
  (->> usages
       (sort-by (juxt :row :col))
       (map
         (fn [{:keys [tags] :as usage}]
           (cond
             (contains? tags :macro)
             (usage->absolute-token usage :macro)

             (contains? tags :refered)
             (usage->absolute-token usage :function))))
       (remove nil?)))

(defn ^:private absolute-token->relative-token
  [tokens
   index
   [row col length token-type token-modifier :as token]]
  (let [[previous-row previous-col _ _ _] (nth tokens (dec index) nil)]
    (cond
      (nil? previous-row)
      token

      (= previous-row row)
      [0
       (- col previous-col)
       length
       token-type
       token-modifier]

      :else
      [(- row previous-row)
       col
       length
       token-type
       token-modifier])))

(defn full
  [usages]
  (let [absolute-tokens (full-absolute usages)]
    (->> absolute-tokens
         (map-indexed (partial absolute-token->relative-token absolute-tokens))
         flatten)))
