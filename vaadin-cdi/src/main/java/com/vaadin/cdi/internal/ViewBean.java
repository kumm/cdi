/*
 * Copyright 2000-2013 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.cdi.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import com.vaadin.ui.UI;

public class ViewBean extends ViewContextual implements Bean, PassivationCapable {

    private static final String PASSIVATION_ID_PREFIX = "com.vaadin.cdi.internal.ViewBean#";

    public ViewBean(Bean delegate, long sessionId, int uiId,
                    String viewIdentifier) {
        super(delegate, sessionId, uiId, viewIdentifier);
    }

    public ViewBean(Bean delegate, int uiId, String viewName) {
        super(delegate, CDIUtil.getSessionId(), uiId, viewName);
    }

    public ViewBean(Bean delegate, String viewName) {
        super(delegate, CDIUtil.getSessionId(), UI.getCurrent().getUIId(), viewName);
    }

    private Bean getDelegate() {
        return (Bean) delegate;
    }
    
    @Override
    public Set<Type> getTypes() {
        return getDelegate().getTypes();
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return getDelegate().getQualifiers();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return getDelegate().getScope();
    }

    @Override
    public String getName() {
        return getDelegate().getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return getDelegate().getStereotypes();
    }

    @Override
    public Class<?> getBeanClass() {
        return getDelegate().getBeanClass();
    }

    @Override
    public boolean isAlternative() {
        return getDelegate().isAlternative();
    }

    @Override
    public boolean isNullable() {
        return getDelegate().isNullable();
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return getDelegate().getInjectionPoints();
    }
    
    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        
        if(o == null || !(o instanceof ViewBean)) {
            return false;
        }
        
        return super.equals(o);
    }

    @Override
    public String getId() {
        StringBuilder sb = new StringBuilder(PASSIVATION_ID_PREFIX);
        sb.append(uiId);
        sb.append("#");
        sb.append(sessionId);
        sb.append("#");
        sb.append(viewIdentifier);

        if (delegate instanceof PassivationCapable) {
            String delegatePassivationID = ((PassivationCapable) delegate)
                    .getId();
            if (delegatePassivationID != null
                    && !delegatePassivationID.isEmpty()) {
                sb.append("#");
                sb.append(delegatePassivationID);
            } else {
                sb.append("#null#");
                sb.append(getBeanClass().getCanonicalName());
            }
        } else {
            // Even if the bean itself is not passivation capable, we're still
            // using ViewBean.getid() as a key in ContextualStorage. It may mix
            // up beans in some cases, specifically if we're injecting
            // non-passivation-capable beans as @ViewScoped
            sb.append("#");
            sb.append(getBeanClass().getCanonicalName());
        }
        return sb.toString();
    }

    private Logger getLogger() {
        return Logger.getLogger(ViewBean.class.getCanonicalName());
    }

}
