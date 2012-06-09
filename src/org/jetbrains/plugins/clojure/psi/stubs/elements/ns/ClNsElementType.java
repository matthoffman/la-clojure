package org.jetbrains.plugins.clojure.psi.stubs.elements.ns;

import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.plugins.clojure.parser.ClojureElementTypes;
import org.jetbrains.plugins.clojure.psi.api.ns.ClNs;
import org.jetbrains.plugins.clojure.psi.impl.ns.ClNsImpl;
import org.jetbrains.plugins.clojure.psi.stubs.api.ClNsStub;
import org.jetbrains.plugins.clojure.psi.stubs.impl.ClNsStubImpl;

/**
 * @author ilyas
 */
public class ClNsElementType extends ClNsElementTypeBase {
  public ClNsElementType() {
    super("ns");
  }

  public ClNs createPsi(ClNsStub stub) {
    return new ClNsImpl(stub, ClojureElementTypes.NS);
  }

  public ClNsStub createStub(ClNs psi, StubElement parentStub) {
    return new ClNsStubImpl(parentStub, StringRef.fromString(psi.getDefinedName()), ClojureElementTypes.NS);
  }



}