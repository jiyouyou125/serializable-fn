(ns serializable.fn
  "Serializable functions! Check it out."
  (:require [clojure.tools.logging :as log])
  (:import [serializable.fn Utils]
           [java.io Writer])
  (:refer-clojure :exclude [fn]))

(def ^:dynamic *serialize* #(Utils/serialize %))
(def ^:dynamic *deserialize* #(Utils/deserialize %))

(defn- save-env [bindings form]
  (let [form (with-meta (cons `fn (rest form)) ; serializable/fn, not core/fn
               (meta form))
        namespace (str *ns*)
        savers (for [^clojure.lang.Compiler$LocalBinding b bindings]
                 [(str (.sym b)) (.sym b)])
        env-form `(into {} ~(vec savers))]
    ;; without the print-dup, it sometimes serializes invalid code strings (with subforms replaced with "#")
    [env-form namespace (binding [*print-dup* false] (pr-str form))]))

(defmacro ^{:doc (str (:doc (meta #'clojure.core/fn))
                      "\n\n  Oh, but it also allows serialization!!!111eleven")}
  fn [& sigs]
  (let [[env-form namespace source] (save-env (vals &env) &form)
        form-meta (meta &form)]
    `(with-meta (clojure.core/fn ~@sigs)
       {:type ::serializable-fn
        ::line (get ~form-meta :line)
        ::column (get ~form-meta :column)
        ::env ~env-form
        ::namespace ~namespace
        ::source ~source})))

(defn- try-parse-num [^String s]
  (try
    (Long/parseLong s)
    (catch NumberFormatException _
      nil )))

(defn- recent-eval? [v]
  (let [m (meta v)
        ^String name (-> m :name str)]
    (and (= "clojure.core" (:ns m))
         (.startsWith name "*")
         (try-parse-num (.substring name 1)))))

(defn search-for-var [val]
  ;; get all of them, filter out *1, *2, and *3, sort by static -> dynamic
  (->> (all-ns)
       (map ns-map)
       (mapcat identity)
       (map second)
       (filter #(and (var? %) (= (var-get %) val)))
       (filter (complement recent-eval?))
       (sort-by (fn [v] (if (-> v meta :dynamic) 1 0)))
       first))

(def ^{:const true} SERIALIZED-TYPES
  {:find-var 1
   :serfn 2
   :kryo 3
   :var 4
   :multifn 5})

(defn type->token [type]
  (SERIALIZED-TYPES type))

(let [reversed (into {} (for [[k v] SERIALIZED-TYPES] [v k]))]
  (defn token->type [token]
    (reversed token)))

(defn serialize-type [val]
  (cond (var? val) :var
        (instance? clojure.lang.MultiFn val) :multifn
        (fn? val) (if (= ::serializable-fn (-> val meta :type))
                    :serfn
                    :find-var)
        :else :kryo))

(defmulti serialize-val serialize-type)

(defn serialize [val]
  (let [type (serialize-type val)
        serialized (serialize-val val)]
    (log/trace "serializing:" val "of type:" type)
    (*serialize* {:token (type->token type) :val-ser serialized})))

(defmethod serialize-val :kryo [val]
  (log/trace "serializing kryo:" val)
  (*serialize* val))

(defn ns-fn-name-pair [v]
  (let [m (meta v)]
    [(str (:ns m)) (str (:name m))]))

(defn serialize-find [val]
  (let [avar (search-for-var val)]
    (when-not avar
      (throw (RuntimeException. "Cannot serialize regular functions that are not bound to vars")))
    (serialize-val avar)))

(defmethod serialize-val :find-var [val]
  (serialize-find val))

(defmethod serialize-val :multifn [val]
  (serialize-find val))

(defmethod serialize-val :var [avar]
  (let [[ns fn-name] (ns-fn-name-pair avar)]
    (log/trace "serializing var:" ns "/" fn-name)
    (*serialize* {:ns ns :fn-name fn-name})))

(defn best-effort-map-val [amap afn]
  (into {}
        (mapcat
         (fn [[name val]]
           (try
             [[name (afn val)]]
             (catch Exception e
               (log/trace (format "caught exception: %s" e))
               (log/trace (format "unable to call %s on %s in %s" afn val amap))
               []
               )))
         amap)))

(defmethod serialize-val :serfn [val]
  (log/trace "serializing serfn:" val)
  (let [[env namespace source] ((juxt ::env ::namespace ::source) (meta val))
        _ (log/trace "serfn env" env)
        _ (log/trace "serfn meta" (-> (meta val)
                                      (dissoc ::env ::namespace ::source)))
        ser-meta (-> (meta val)
                     (dissoc ::env ::namespace ::source)
                     (best-effort-map-val serialize)
                     *serialize*)
        ser-env (-> env (best-effort-map-val serialize) *serialize*)]
    (*serialize* {:ser-meta ser-meta :ser-env ser-env :ns namespace :source source})))

(defmulti deserialize-val (fn [token serialized]
                            (token->type token)))

(defn deserialize [serialized]
  (let [{:keys [token val-ser]} (*deserialize* serialized)]
    (deserialize-val token val-ser)))

(defmethod deserialize-val :find-var [_ serialized]
  (let [{:keys [ns fn-name]} (*deserialize* serialized)]
    (log/trace "deserializing find-var:" ns "/" fn-name)
    (Utils/bootSimpleFn ns fn-name)))

(defmethod deserialize-val :multifn [_ serialized]
  (let [{:keys [ns fn-name]} (*deserialize* serialized)]
    (log/trace "deserializing multifn:" ns "/" fn-name)
    (Utils/bootSimpleMultifn ns fn-name)))

(defmethod deserialize-val :var [_ serialized]
  (let [{:keys [ns fn-name]} (*deserialize* serialized)]
    (log/trace "deserializing var:" ns "/" fn-name)
    (Utils/bootSimpleVar ns fn-name)))

(defmethod deserialize-val :kryo [_ serialized]
  (log/trace "deserializing kryo")
  (*deserialize* serialized))

(def ^:dynamic *global-env* {})

(defmethod deserialize-val :serfn [_ serialized]
  (let [{:keys [ser-meta ser-env ns source]} (*deserialize* serialized)
        _ (log/trace "deserializing serfn:" source)
        rest-meta (best-effort-map-val (*deserialize* ser-meta) deserialize)
        _ (log/trace "meta:" rest-meta)
        env (best-effort-map-val (*deserialize* ser-env) deserialize)
        _ (log/trace "env:" env)
        source-form (try (read-string source) (catch Exception e
                                                (throw (RuntimeException. (str "Could not deserialize " source)))))
        namespace (symbol ns)
        old-ns (-> *ns* str symbol)
        bindings (mapcat (fn [[name val]] [(symbol name) `(*global-env* ~name)]) env)
        to-eval `(let ~(vec bindings) ~source-form)
        _ (log/trace "to-eval:" to-eval)]
    (Utils/tryRequire (str namespace))
    (vary-meta (binding [*ns* (create-ns namespace) *global-env* env]
                 (eval to-eval))
               merge
               rest-meta)))

(defmethod print-method ::serializable-fn [o ^Writer w]
  (let [m (meta o)]
    (print-method (str (::namespace m) ":" (::line m) ":" (::column m) " " (::source m))  w)))
