package org.jetbrains.plugins.clojure.psi.impl;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.clojure.psi.stubs.ClojureShortNamesCache;

/**
 * @author ilyas
 */
public class ClojurePsiManager implements ProjectComponent {
  private final Project myProject;
  private ClojureShortNamesCache myCache;

  public ClojurePsiManager(Project project) {
    myProject = project;
  }

  public void projectOpened() {
  }

  public void projectClosed() {
  }

  @NotNull
  public String getComponentName() {
    return "ClojurePsiManager";
  }

  public void initComponent() {
    myCache = new ClojureShortNamesCache(myProject);
  }

  public void disposeComponent() {
  }

  public static ClojurePsiManager getInstance(Project project) {
    return project.getComponent(ClojurePsiManager.class);
  }

  public ClojureShortNamesCache getNamesCache() {
    return myCache;
  }
}
