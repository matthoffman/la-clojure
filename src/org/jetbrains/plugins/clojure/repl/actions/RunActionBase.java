package org.jetbrains.plugins.clojure.repl.actions;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.content.Content;
import org.jetbrains.plugins.clojure.psi.api.ClojureFile;
import org.jetbrains.plugins.clojure.repl.ClojureConsole;
import org.jetbrains.plugins.clojure.repl.LanguageConsoleImpl;
import org.jetbrains.plugins.clojure.repl.REPL;
import org.jetbrains.plugins.clojure.repl.Response;
import org.jetbrains.plugins.clojure.repl.toolwindow.REPLToolWindowFactory;
import org.jetbrains.plugins.clojure.utils.Editors;

public abstract class RunActionBase extends AnAction
{
  private static final Logger LOG = Logger.getInstance(RunActionBase.class.getName());

  public static void executeCommand(Project project, String command)
  {
    REPL activeRepl = findActiveRepl(project);
    LOG.assertTrue(activeRepl != null);

    ClojureConsole languageConsole = activeRepl.getConsoleView().getConsole();
//    languageConsole.printToHistory(languageConsole.getPrompt(), ConsoleViewContentType.USER_INPUT.getAttributes());
    languageConsole.printToHistory(command + "\n", ConsoleViewContentType.NORMAL_OUTPUT.getAttributes());
    Editors.scrollDown(languageConsole.getHistoryViewer());

    if (!StringUtil.isEmptyOrSpaces(command))
    {
      languageConsole.getHistoryModel().addToHistory(command);
    }

    Response response = activeRepl.execute(command);
    languageConsole.printResponse(response, true);
  }

  protected static void executeTextRange(Editor editor, TextRange textRange)
  {
    REPL activeRepl = findActiveRepl(editor.getProject());
    LOG.assertTrue(activeRepl != null);

    ClojureConsole languageConsole = activeRepl.getConsoleView().getConsole();
    String command = languageConsole.addTextRangeToHistory((EditorEx) editor, textRange);

    if (!StringUtil.isEmptyOrSpaces(command))
    {
      languageConsole.getHistoryModel().addToHistory(command);
    }

    Response response = activeRepl.execute(command);
    languageConsole.printResponse(response, true);
  }

  @Override
  public void update(AnActionEvent e)
  {
    Presentation presentation = e.getPresentation();

    Editor editor = e.getData(PlatformDataKeys.EDITOR);

    if (editor == null)
    {
      presentation.setEnabled(false);
      return;
    }
    Project project = editor.getProject();
    if (project == null)
    {
      presentation.setEnabled(false);
      return;
    }

    Document document = editor.getDocument();
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if ((psiFile == null) || (!(psiFile instanceof ClojureFile)))
    {
      presentation.setEnabled(false);
      return;
    }

    VirtualFile virtualFile = psiFile.getVirtualFile();
    if ((virtualFile == null) || ((virtualFile instanceof LightVirtualFile)))
    {
      presentation.setEnabled(false);
      return;
    }
    String filePath = virtualFile.getPath();
    if (filePath == null)
    {
      presentation.setEnabled(false);
      return;
    }

    REPL activeRepl = findActiveRepl(project);
    if (activeRepl == null || !activeRepl.isActive())
    {
      presentation.setEnabled(false);
      return;
    }

    LanguageConsoleImpl console = activeRepl.getConsoleView().getConsole();
    if (!(console instanceof ClojureConsole))
    {
      presentation.setEnabled(false);
      return;
    }

    presentation.setEnabled(true);
  }

  protected static REPL findActiveRepl(Project project)
  {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = toolWindowManager.getToolWindow(REPLToolWindowFactory.TOOL_WINDOW_ID);
    Content content = toolWindow.getContentManager().getSelectedContent();

    if (content != null)
    {
      return content.getUserData(REPL.REPL_KEY);
    }

    return null;
  }
}
