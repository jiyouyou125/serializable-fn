(defproject yieldbot/serializable-fn "0.1.2"
  :url "http://github.com/sorenmacbeth/serializable-fn"
  :description "Serializable functions in Clojure"
  :min-lein-version "2.0.0"
  :source-paths ["src/clj"]
  :java-source-paths ["src/jvm"]
  :javac-options ["-source" "1.6" "-target" "1.6"]
  :jvm-opts ^:replace ["-server" "-Xmx512m"]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.twitter/chill-java "0.8.0"]
                 [com.twitter/carbonite "1.5.0"]]
  :profiles {:dev
             {:dependencies [[ch.qos.logback/logback-classic "1.1.2"]
                             [criterium "0.4.3"]]}
             :provided
             {:dependencies [[ch.qos.logback/logback-classic "1.1.2"]]}})
