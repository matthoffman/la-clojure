package org.jetbrains.plugins.clojure.psi.util;

import org.jetbrains.annotations.NotNull;

/**
 * @author ilyas
 */
public abstract class ClojureTextUtil {

  public  static  String getSymbolPrefix(@NotNull String sym, @NotNull String sep) {
    int index = sym.lastIndexOf(sep);
    return index > 0 && index < sym.length() - 1 ? sym.substring(0, index) : "";
  }

  public  static  String getSymbolPrefix(@NotNull String sym) {
    return getSymbolPrefix(sym, ".");
  }
}
