package org.jetbrains.plugins.clojure.psi;

import clojure.lang.Keyword;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.clojure.ClojureIcons;
import org.jetbrains.plugins.clojure.file.ClojureFileType;

import javax.swing.*;

/**
 * @author Colin Fleming
 */
public class ClojureConsoleElement extends LightElement implements PsiNamedElement
{
  @NotNull
  private final String name;
  private final Keyword resolveKey;

  public ClojureConsoleElement(PsiManager manager, @NotNull String name, Keyword resolveKey)
  {
    super(manager, ClojureFileType.CLOJURE_LANGUAGE);
    this.name = name;
    this.resolveKey = resolveKey;
  }

  @Override
  public String getText()
  {
    return name;
  }

  @Override
  public PsiElement copy()
  {
    return new ClojureConsoleElement(getManager(), name, resolveKey);
  }

  @Override
  @NotNull
  public String getName()
  {
    return name;
  }

  public Keyword getResolveKey() {
    return resolveKey;
  }

  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException
  {
    throw new UnsupportedOperationException("Can't set name for console elements");
  }

  @Override
  public String toString()
  {
    return "Console element " + name;
  }

  @Override
  public Icon getIcon(int flags) {
    return ClojureIcons.SYMBOL;
  }
}
