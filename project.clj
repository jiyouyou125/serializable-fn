(defproject yieldbot/serializable-fn "0.0.3-SNAPSHOT"
  :url "http://github.com/technomancy/serializable-fn"
  :description "Serializable functions in Clojure"
  :min-lein-version "2.0.0"
  :source-paths ["src/clj"]
  :java-source-paths ["src/jvm"]
  :javac-options ["-source" "1.6" "-target" "1.6"]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.twitter/chill-java "0.3.5"]
                 [com.twitter/carbonite "1.3.3-SNAPSHOT"]])
