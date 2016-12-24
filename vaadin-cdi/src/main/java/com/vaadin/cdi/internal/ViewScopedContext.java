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
import java.util.Map;
import java.util.logging.Logger;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.BeanManager;
import com.vaadin.util.CurrentInstance;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.apache.deltaspike.core.util.context.ContextualStorage;

import com.vaadin.cdi.ViewScoped;
import com.vaadin.ui.UI;

/**
 * ViewScopedContext is the context for @ViewScoped beans.
 */
public class ViewScopedContext extends AbstractVaadinContext {

    private ActiveViewContextHolder activeViewContextHolder;

    private static class ViewStorageKey extends StorageKey {
        private final String viewName;

        public ViewStorageKey(int uiId, String viewName) {
            super(uiId);
            this.viewName = viewName;
        }

        public String getViewName() {
            return viewName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ViewStorageKey)) return false;
            if (!super.equals(o)) return false;

            ViewStorageKey that = (ViewStorageKey) o;

            return viewName.equals(that.viewName);

        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (viewName != null ? viewName.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ViewStorageKey{" +
                    "uiId=" + uiId + "," +
                    "viewName='" + viewName + '\'' +
                    '}';
        }
    }


    public ViewScopedContext(final BeanManager beanManager) {
        super(beanManager);
        getLogger().fine("Instantiating ViewScoped context");
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ViewScoped.class;
    }

    @Override
    protected StorageKey getStorageKey(Contextual<?> contextual) {
        UI currentUI = UI.getCurrent();
        if (currentUI == null) {
            throw new IllegalStateException("Unable to resolve " + contextual + ", current UI not set.");
        }
        String viewName = getActiveViewContextHolder().getActiveViewName();
        if (viewName == null) {
            throw new IllegalStateException("Could not determine active View for " + contextual);
        }

        return new ViewStorageKey(currentUI.getUIId(), viewName);
    }

    private ActiveViewContextHolder getActiveViewContextHolder() {
        if (activeViewContextHolder == null) {
            activeViewContextHolder = BeanProvider
                    .getContextualReference(getBeanManager(), ActiveViewContextHolder.class, false);
        }
        return activeViewContextHolder;
    }

    synchronized void viewChangeCleanup(long sessionId, int uiId, String viewName) {
        getLogger().fine("ViewChangeCleanup for " + sessionId + " " + uiId);
        SessionData sessionData = getSessionData(sessionId, true);
        Map<StorageKey, ContextualStorage> map = sessionData.getStorageMap();

        for (Map.Entry<StorageKey, ContextualStorage> entry : map.entrySet()) {
            ViewStorageKey key = (ViewStorageKey) entry.getKey();
            if (key.getUiId() == uiId && !key.getViewName().equals(viewName)) {
                ContextualStorage storage = entry.getValue();
                getLogger().fine("dropping " + key + " : " + storage);
                map.remove(key);
                destroyAllActive(storage);
            }
        }
    }

    @Override
    protected Logger getLogger() {
        return Logger.getLogger(ViewScopedContext.class.getCanonicalName());
    }
}
