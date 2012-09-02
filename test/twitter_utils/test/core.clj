(ns twitter-utils.test.core
  (:use [twitter-utils.core]
        [midje.sweet]
        [twitter.oauth :only [make-oauth-creds]]
        [twitter.api.restful :only [show-followers]])
  (:require [twitter-utils.test.fixtures :as fixtures])
  (:import [java.util Properties]))

(defn load-config-file
  "this loads a config file from the classpath"
  [file-name]
  (let [file-reader (.. (Thread/currentThread)
                        (getContextClassLoader)
                        (getResourceAsStream file-name))
        props (Properties.)]
    (.load props file-reader)
    (into {} props)))

(def ^:dynamic *config* (load-config-file "test.config"))

(defn assert-get
  "get a value from the config, otherwise throw an exception detailing the problem"
  [key-name]
  
  (or (get *config* key-name) 
      (throw (Exception. (format "please define %s in the resources/test.config file" key-name)))))

(def ^:dynamic *app-token* (assert-get "app.consumer.key"))
(def ^:dynamic *app-secret* (assert-get "app.consumer.secret"))

(def ^:dynamic *user-screen-name* (assert-get "user.screen.name"))
(def ^:dynamic *user-token* (assert-get "user.access.token"))
(def ^:dynamic *user-secret* (assert-get "user.access.token.secret"))

(defn refer-private [ns] 
     (doseq [[symbol var] (ns-interns ns)] 
       (when (:private (meta var)) 
         (intern *ns* symbol var))))

(refer-private 'twitter-utils.core)

(fact (seq-to-comma-separated [1 2 3]) => "1,2,3")
(fact (seq-to-comma-separated #{1 2 3}) => "1,2,3")

(defn seq-with-maps-with-screennames? [s]
  (every? #(:screen_name %) s))

(fact (seq-with-maps-with-screennames? [{:screen_name "Michiel"} {:screen_name "Borkdude"}]) => true)

(fact
 "Userinfos delivers a sequence of map and every map is supposed to have a :screenname key"
 (userinfos fixtures/idset-example-small) => seq-with-maps-with-screennames?)

(defn set-of-ints? [v]
  (and (set? v)
       (every? integer? v)))

(fact (set-of-ints? #{}) => true)
(fact (set-of-ints? #{1 2 3}) => true)
(fact (set-of-ints? nil) => false)
(fact (set-of-ints? #{1.0 2 3}) => false)

(fact (idset show-followers :params {:screen-name *user-screen-name*}) => set-of-ints?)

(defn seq-of-strings? [s]
  (every? string? s))

(fact (followers-minus-friends *user-screen-name*) => seq-of-strings?)

(def ^:dynamic *creds* (make-oauth-creds *app-token* *app-secret* *user-token* *user-secret*))

(print *app-token*)

(fact (friend-ids-by-auth *creds*) => set-of-ints?)

(fact (doesnt-follow-me-back *app-token* *app-secret* *user-token* *user-secret*) => seq-of-strings?)