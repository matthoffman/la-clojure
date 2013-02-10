package org.jetbrains.plugins.clojure.editor.braceHighlighter;

import com.intellij.concurrency.Job;
import com.intellij.concurrency.JobLauncher;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.Alarm;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.clojure.lexer.ClojureTokenTypes;
import org.jetbrains.plugins.clojure.psi.api.ClojureFile;
import org.jetbrains.plugins.clojure.settings.ClojureProjectSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author ilyas
 */
public class ClojureBraceHighlightingHandler {

  private final Project myProject;
  private final Editor myEditor;
  private final PsiFile myFile;
  private final Alarm myAlarm;
  //  private DocumentEx myDocument;
  private Stack<TextAttributes> myColorStack = new Stack<TextAttributes>();

  private static final Key<List<RangeHighlighter>> CLOJURE_BRACE_PAINTER_KEY = Key.create("ClojureBracePainter.CLOJURE_BRACE_PAINTER_KEY");

  public ClojureBraceHighlightingHandler(Project project, Editor newEditor, Alarm alarm, PsiFile file) {
    myProject = project;
    myEditor = newEditor;
    myFile = file;
    myAlarm = alarm;
//    myDocument = (DocumentEx) myEditor.getDocument();
  }

  private static boolean isReallyDisposed(Editor editor, Project project) {
    Project editorProject = editor.getProject();
    return editorProject == null ||
        editorProject.isDisposed() || project.isDisposed() || !editor.getComponent().isShowing() || editor.isViewer();
  }


  static void lookForInjectedAndHighlightInOtherThread(@NotNull final Editor editor, @NotNull final Alarm alarm, @NotNull final Processor<ClojureBraceHighlightingHandler> processor) {
    final Project project = editor.getProject();
    if (project == null) return;
    JobLauncher.getInstance().submitToJobThread(Job.DEFAULT_PRIORITY, new Runnable() {
      public void run() {
        if (isReallyDisposed(editor, project)) return;
        ApplicationManager.getApplication().invokeLater(new DumbAwareRunnable() {
          public void run() {
            final PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
            if (!isReallyDisposed(editor, project) && (psiFile instanceof ClojureFile)) {
              final ClojureFile file = (ClojureFile) psiFile;
              ClojureBraceHighlightingHandler handler = new ClojureBraceHighlightingHandler(project, editor, alarm, file);
              processor.process(handler);
            }
          }
        }, ModalityState.stateForComponent(editor.getComponent()));
      }
    });
  }

  private static PsiElement findTopElement(PsiElement elem) {
    if (elem == null) return null;
    if (elem instanceof PsiComment) return elem;
    final PsiElement parent = elem.getParent();
    if (parent instanceof ClojureFile) return elem;
    else return findTopElement(parent);
  }


  public void updateBraces(final DocumentEvent e) {
    if (myFile == null) return;

    myAlarm.addRequest(new Runnable() {
      public void run() {
        final PsiElement topElement = e != null ? findTopElement(PsiUtilBase.getElementAtCaret(myEditor)) : myFile;
        highlightImpl(e, topElement);
      }
    }, 300);
  }

  private void highlightImpl(DocumentEvent e, PsiElement topElement) {
    if (!myProject.isDisposed() && !myEditor.isDisposed()) {
      if (myProject.isDisposed() || myEditor.isDisposed()) return;
      final boolean paint = ClojureProjectSettings.getInstance(myEditor.getProject()).coloredParentheses;
      final int startOffset;
      final int endOffset;

      if (e != null) {
        final int eventEnd = e.getOffset() + e.getNewLength();
        startOffset = topElement == null ? e.getOffset() : Math.min(topElement.getTextRange().getStartOffset(), e.getOffset());
        endOffset = topElement == null ? eventEnd : Math.max(topElement.getTextRange().getEndOffset(), eventEnd);
      } else {
        startOffset = topElement.getTextRange().getStartOffset();
        endOffset = topElement.getTextRange().getEndOffset();
      }

      final HighlighterIterator iterator = ((EditorEx) myEditor).getHighlighter().createIterator(startOffset);
      final EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();

      int level = 0;

      while (!iterator.atEnd() && iterator.getEnd() <= endOffset) {

        if (ClojureTokenTypes.LEFT_PAREN.equals(iterator.getTokenType())) {
          final TextAttributes attributes = ClojureBraceAttributes.CLOJURE_BRACE_ATTRIBUTES[level % ClojureBraceAttributes.CLOJURE_BRACE_ATTRIBUTES.length];

          myColorStack.push(attributes);
          final int start = iterator.getStart();
          highlightBrace(start, attributes);
          level++;
        } else if (ClojureTokenTypes.RIGHT_PAREN.equals(iterator.getTokenType())) {
          if (myColorStack.isEmpty()) break;
          level--;
          final TextAttributes attributes = myColorStack.pop();
          final int start = iterator.getStart();
          highlightBrace(start, attributes);
        }
        iterator.advance();
      }
    }
  }

  private void registerHighlighter(RangeHighlighter highlighter) {
    List<RangeHighlighter> highlighters = myEditor.getUserData(CLOJURE_BRACE_PAINTER_KEY);
    if (highlighters == null) {
      highlighters = new ArrayList<RangeHighlighter>();
      myEditor.putUserData(CLOJURE_BRACE_PAINTER_KEY, highlighters);
    }

    highlighters.add(highlighter);
  }

  private void highlightBrace(int rBraceOffset, TextAttributes attributes) {
    RangeHighlighter highlighter =
        myEditor.getMarkupModel().addRangeHighlighter(
            rBraceOffset, rBraceOffset + 1, HighlighterLayer.LAST + 1, attributes, HighlighterTargetArea.EXACT_RANGE);
    highlighter.setGreedyToLeft(false);
    highlighter.setGreedyToRight(false);
    registerHighlighter(highlighter);
  }


}
