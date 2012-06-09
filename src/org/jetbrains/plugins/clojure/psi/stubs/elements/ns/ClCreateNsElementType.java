package org.jetbrains.plugins.clojure.psi.stubs.elements.ns;

import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.plugins.clojure.psi.impl.ns.ClInNsImpl;
import org.jetbrains.plugins.clojure.psi.api.ns.ClNs;
import org.jetbrains.plugins.clojure.psi.stubs.api.ClNsStub;
import org.jetbrains.plugins.clojure.psi.stubs.impl.ClNsStubImpl;
import org.jetbrains.plugins.clojure.parser.ClojureElementTypes;

/**
 * @author ilyas
 */
public class ClCreateNsElementType extends ClNsElementTypeBase {
  public ClCreateNsElementType() {
    super("create-ns");
  }

  public ClNs createPsi(ClNsStub stub) {
    return new ClInNsImpl(stub, ClojureElementTypes.CREATE_NS);
  }

  public ClNsStub createStub(ClNs psi, StubElement parentStub) {
    return new ClNsStubImpl(parentStub, StringRef.fromString(psi.getDefinedName()), ClojureElementTypes.CREATE_NS);
  }


}
