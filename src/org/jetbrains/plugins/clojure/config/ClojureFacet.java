/*
 * Copyright 2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.clojure.config;

import com.intellij.facet.*;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

/**
 * @author ilyas
 * @author <a href="mailto:ianp@ianp.org">Ian Phillips</a>
 */
public class ClojureFacet extends Facet<ClojureFacetConfiguration> {

  public static final String FACET_TYPE_ID_STRING = "clojure";
  public final static FacetTypeId<ClojureFacet> ID = new FacetTypeId<ClojureFacet>(FACET_TYPE_ID_STRING);

  public ClojureFacet(final FacetType facetType, final Module module, final String name, final ClojureFacetConfiguration configuration, final Facet underlyingFacet) {
    super(facetType, module, name, configuration, underlyingFacet);
  }

  public static ClojureFacet getInstance(@NotNull Module module){
    return FacetManager.getInstance(module).getFacetByType(ID);
  }

  @Override
  public void initFacet() {
    super.initFacet();
  }

  public String getJvmOptions() {
    return getConfiguration().getState().myJvmOpts;
  }

  public String getReplOptions() {
    return getConfiguration().getState().myReplOpts;
  }
}
