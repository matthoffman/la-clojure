(ns plugin.annotator
  (:import (com.intellij.lang.annotation Annotator AnnotationHolder)
           (com.intellij.openapi.diagnostic Logger)
           (org.jetbrains.plugins.clojure.psi.api ClList ClojureFile ClVector ClMetadata)
           (org.jetbrains.plugins.clojure.psi.api.symbols ClSymbol)
           (com.intellij.openapi.editor.colors CodeInsightColors)
           (com.intellij.psi PsiClass PsiElement PsiFile PsiWhiteSpace PsiComment)
           (org.jetbrains.plugins.clojure.psi.resolve ClojureResolveResult)
           (org.jetbrains.plugins.clojure.highlighter ClojureSyntaxHighlighter)
           (com.intellij.codeInsight.intention IntentionAction)
           (com.intellij.psi.search PsiShortNamesCache)
           (com.intellij.codeInsight.daemon.impl.quickfix ImportClassFixBase)
           (com.intellij.lang LanguageAnnotators)
           (org.jetbrains.plugins.clojure ClojureLanguage)
           (org.jetbrains.plugins.clojure.psi.api.defs ClDef)
           (org.jetbrains.plugins.clojure.parser ClojureSpecialFormTokens)
           (com.intellij.psi.util PsiTreeUtil)
           (com.intellij.psi.impl.source.tree LeafPsiElement)
           (org.jetbrains.plugins.clojure.psi.impl ClMetaForm))
  (:use [plugin.util :only [with-logging]]))

;(set! *warn-on-reflection* true)

(def ^Logger logger (Logger/getInstance "plugin.annotator"))

(def implicit-names #{"def" "new" "throw" "ns" "in-ns" "if" "do" "let"
                      "quote" "var" "fn" "loop" "recur" "try"
                      "monitor-enter" "monitor-exit" "." ".." "set!"
                      "%" "%1" "%2" "%3" "%4" "%5" "%6" "%7" "%8" "%9" "%&" "&"})

(def local-bindings #{"let", "with-open", "with-local-vars", "when-let", 
                      "when-first", "for", "if-let", "loop", "fn", "doseq"})

(def instantiators #{"proxy" "reify" "definterface" "deftype" "defrecord"})

(defn impl-method?
  "Checks to see if an element is a method implementation for proxy et al"
  [^PsiElement element]
  (if-let [parent (.getParent element)]
    (and (instance? ClList element)
         (instance? ClList parent)
         (instantiators (.getHeadText parent)))
    false))

(defn significant? [element]
  (not (or (nil? element)
           (instance? LeafPsiElement element)
           (instance? PsiWhiteSpace element)
           (instance? PsiComment element)
           (instance? ClMetadata element)
           (instance? ClMetaForm element))))

(defn significant-children [element]
  (filter significant? (.getChildren element)))

(defn ancestor?
  ([ancestor element]
   (ancestor? ancestor element true))
  ([ancestor element strict]
   (PsiTreeUtil/isAncestor ancestor element strict)))

(defn find-context-ancestor [^PsiElement element pred strict]
  (if-not (nil? element)
    (loop [current (if strict (.getContext element) element)]
      (if-not (nil? current)
        (if (pred current)
          current
          (recur (.getContext current)))))))

(defn local-def? [^PsiElement element]
  (if-let [let-block (find-context-ancestor element
                                            (fn [element]
                                              (and (instance? ClList element)
                                                   (local-bindings (.getHeadText ^ClList element))))
                                            true)]
    (let [params (second (significant-children let-block))
          definitions (take-nth 2 (significant-children params))]
      (some #(ancestor? % element) definitions))))

(defn should-resolve? [^ClSymbol element]
  (let [parent (.getParent element)
        grandparent (.getParent parent)]
    (cond
      ; names of def/defn etc
      (and (instance? ClDef parent)
           (= element (.getNameSymbol parent))) false
      ; parameters of implementation methods
      (and (instance? ClVector parent)
           (impl-method? grandparent)
           ;(= parent (.getSecondNonLeafElement grandparent)) TODO - some need third, eg proxy
           ) false
      (local-def? element) false
      :else true)))

(defn annotate-list [^ClList element ^AnnotationHolder holder]
  (let [first (.getFirstSymbol element)]
    (if (not (nil? first))
      (do
        (if (or (< 0 (alength (.multiResolve first false)))
                (implicit-names (.getHeadText element)))
          (let [annotation (.createInfoAnnotation holder first nil)]
            (.setTextAttributes annotation ClojureSyntaxHighlighter/DEF)))))))

(defn resolves-to? [^ClojureResolveResult result type]
  (instance? type (.getElement result)))

(defn annotate-import [^ClSymbol element ^AnnotationHolder holder]
  (if (not (.isQualified element))
    (let [cache (PsiShortNamesCache/getInstance (.getProject element))
          scope (.getResolveScope element)
          ref-name (.getReferenceName element)
          name (cond
                 (nil? ref-name) nil
                 (.endsWith ref-name ".") (.substring ref-name 0 (dec (.length ref-name)))
                 :else ref-name)
          classes (if-not (nil? name) (.getClassesByName cache name scope))]
      (if (and (not (nil? classes))
               (< 0 (alength classes)))
        (let [annotation (.createInfoAnnotation holder
                                                element
                                                (str (.getText element) " can be imported"))]
          (.setTextAttributes annotation CodeInsightColors/WARNINGS_ATTRIBUTES)
          (.registerFix annotation
                        (proxy [ImportClassFixBase] [element]
                          (getReferenceName [^ClSymbol reference]
                            (if-let [ref-name (.getReferenceName element)]
                              (if (.endsWith ref-name ".")
                                (.substring ref-name 0 (dec (.length ref-name)))
                                ref-name)))
                          (getReferenceNameElement [^ClSymbol reference]
                            (.getReferenceNameElement reference))
                          (hasTypeParameters [reference] false)
                          (getQualifiedName [^ClSymbol reference]
                            (.getText reference))
                          (isQualified [^ClSymbol reference]
                            (.isQualified reference))
                          (hasUnresolvedImportWhichCanImport [file name] false)
                          (isAccessible [class reference] true)))
          true)))))

(defn annotate-unresolved [^ClSymbol element ^AnnotationHolder holder]
  (if-not (annotate-import element holder)
    (let [annotation (.createInfoAnnotation holder
                                            element
                                            (str (.getText element) " cannot be resolved"))]
      (.setTextAttributes annotation CodeInsightColors/WARNINGS_ATTRIBUTES))))

(defn annotate-selfresolve [^ClSymbol element ^AnnotationHolder holder]
  (let [annotation (.createInfoAnnotation holder
                                          element
                                          (str (.getText element) " resolves to itself"))]
    (.setTextAttributes annotation CodeInsightColors/ERRORS_ATTRIBUTES)))

(defn process-element [^PsiElement element pred action]
  (if (pred element)
    (action element))
  (doseq [child (seq (.getChildren element))]
    (process-element child pred action)))

(defn import-fully-qualified [project editor ^ClojureFile psi-file ^ClSymbol element target]
  (let [ns (.findOrCreateNamespaceElement psi-file)
        element-text (.getText element)]
    (.addImportForClass ns element target)
    (process-element (.getContainingFile element)
                     #(and (instance? ClSymbol %)
                           (= element-text (.getText ^ClSymbol %)))
                     #(let [qualifier (.getQualifierSymbol ^ClSymbol %)
                            separator (.getSeparatorToken ^ClSymbol %)]
                        (.delete qualifier)
                        (.delete separator)))))

(defn annotate-fqn [^ClSymbol element target ^AnnotationHolder holder]
  (let [annotation (.createInfoAnnotation holder
                                          element
                                          (str (.getText element) " is fully qualified"))]
    (.setTextAttributes annotation CodeInsightColors/WEAK_WARNING_ATTRIBUTES)
    (.registerFix annotation
                  (reify IntentionAction
                    (getText [this] "Import Class")
                    (getFamilyName [this] (.getText this))
                    (isAvailable [this project editor psi-file] true)
                    (invoke [this project editor psi-file]
                      (import-fully-qualified project editor psi-file element target))
                    (startInWriteAction [this] true)))))

(defn annotate-symbol [^ClSymbol element ^AnnotationHolder holder]
  (let [result (.multiResolve element false)]
    (cond
      (and (= 0 (alength result))
           (not (implicit-names (.getText element)))
           (should-resolve? element)) (annotate-unresolved element holder)
      (.isQualified element) (if-let [target ^ClojureResolveResult (first (filter #(resolves-to? % PsiClass) (seq result)))]
                               (annotate-fqn element (.getElement target) holder))
      (some #(= element (.getElement %)) (seq result)) (annotate-selfresolve element holder))))

(defn annotate [element holder]
  (cond
    (instance? ClList element) (annotate-list element holder)
    (instance? ClSymbol element) (annotate-symbol element holder)))

(defn initialise []
  (.addExplicitExtension
    LanguageAnnotators/INSTANCE
    (ClojureLanguage/getInstance)
    (reify Annotator
      (annotate [this element holder]
        (with-logging
          (annotate element holder))))))
