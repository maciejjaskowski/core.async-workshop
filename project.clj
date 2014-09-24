(defproject core.async-workshop "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha2"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  :target-path "target/%s"
  :repl-options {
                  ;; If nREPL takes too long to load it may timeout,
                  ;; increase this to wait longer before timing out.
                  ;; Defaults to 30000 (30 seconds)
                  :timeout 120000
                  })
