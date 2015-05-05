(defproject yieldbot/serializable-fn "0.0.7"
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
                 [org.clojure/tools.logging "0.2.6"]
                 [com.twitter/chill-java "0.3.5"]
                 [com.twitter/carbonite "1.3.3"]]
  :profiles {:dev
             {:dependencies [[ch.qos.logback/logback-classic "1.1.2"]
                             [criterium "0.4.3"]]}
             :provided
             {:dependencies [[ch.qos.logback/logback-classic "1.1.2"]]}})
