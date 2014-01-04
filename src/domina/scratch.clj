; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns domina.scratch
  (:require 
    [clj-commons-exec :as commons-exec]
    ))

(def sleeping-exec nil)

(defn create-sleeping-exec []
  (def sleeping-exec 
    (commons-exec/sh ["sleep" "10"])))
 
(defn deref-sleeping-exec []
  (deref sleeping-exec 1000 {}))



;(into {} (map (fn [[x y]] [x y] ) {:a "x" :b 7}))
