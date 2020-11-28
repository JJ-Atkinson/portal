(ns portal.ui.object
  (:require [cognitect.transit :as t]
            [reagent.core :as r]))

(defn- cleanup [held-value]
  (js/console.log "cleanup" (pr-str held-value)))

(defonce registry (js/FinalizationRegistry. #(cleanup %)))

(defonce value-cache (r/atom {}))
(defonce instance-cache (atom {}))

(deftype PortalObject [rep]
  IMeta
  (-meta [this]
    (:meta rep))

  IWithMeta
  (-with-meta [this m]
    (PortalObject. (assoc rep :meta m)))

  IDeref
  (-deref [this]
    (get @value-cache (:id rep) rep)))

(defn portal-object? [x] (instance? PortalObject x))

(def readers
  {"portal.transit/object"
   (t/read-handler
    (fn [{:keys [id] :as rep}]
      (let [weak-ref (get @instance-cache id)
            obj      (if weak-ref
                       (.deref weak-ref)
                       js/undefined)]
        (if-not (undefined? obj)
          obj
          (let [obj (PortalObject. rep)]
            (.register registry obj rep)
            (swap! instance-cache assoc id (js/WeakRef. obj))
            obj)))))})

(def writers
  {PortalObject
   (t/write-handler
    (constantly "portal.transit/object") #(.-rep %))})