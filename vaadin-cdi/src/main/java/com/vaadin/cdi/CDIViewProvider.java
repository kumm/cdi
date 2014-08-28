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

package com.vaadin.cdi;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;

import com.vaadin.cdi.access.AccessControl;
import com.vaadin.cdi.internal.Conventions;
import com.vaadin.cdi.internal.ViewBean;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.ui.UI;

public class CDIViewProvider implements ViewProvider {

    private static final Annotation QUALIFIER_ANY = new AnnotationLiteral<Any>() {
    };

    @Inject
    private BeanManager beanManager;

    @Inject
    private AccessControl accessControl;
    private transient CreationalContext<?> currentViewCreationalContext;

    public final static class ViewChangeListenerImpl implements
            ViewChangeListener {

        private BeanManager beanManager;

        public ViewChangeListenerImpl(BeanManager beanManager) {
            this.beanManager = beanManager;
        }

        @Override
        public boolean beforeViewChange(ViewChangeEvent event) {
            return true;
        }

        @Override
        public void afterViewChange(ViewChangeEvent event) {
            getLogger().fine(
                    "Changing view from " + event.getOldView() + " to "
                            + event.getNewView());
            beanManager.fireEvent(event);
        }
    }

    private ViewChangeListener viewChangeListener;

    @PostConstruct
    private void postConstruct() {
        viewChangeListener = new ViewChangeListenerImpl(beanManager);
    }

    @Override
    public String getViewName(String viewAndParameters) {
        getLogger().log(Level.FINE,
                "Attempting to retrieve view name from string \"{0}\"",
                viewAndParameters);

        String name = parseViewName(viewAndParameters);
        ViewBean viewBean = getViewBean(name);

        if (viewBean == null) {
            return null;
        }

        if (isUserHavingAccessToView(viewBean)) {
            if (viewBean.getBeanClass().isAnnotationPresent(CDIView.class)) {
                String specifiedViewName = Conventions
                        .deriveMappingForView(viewBean.getBeanClass());
                if (!specifiedViewName.isEmpty()) {
                    return specifiedViewName;
                }
            }
            return name;
        } else {
            getLogger().log(Level.INFO,
                    "User {0} did not have access to view \"{1}\"",
                    new Object[] { accessControl.getPrincipalName(), viewBean });
        }

        return null;
    }

    protected boolean isUserHavingAccessToView(Bean<?> viewBean) {

        if (viewBean.getBeanClass().isAnnotationPresent(CDIView.class)) {
            if (!viewBean.getBeanClass()
                    .isAnnotationPresent(RolesAllowed.class)) {
                // No roles defined, everyone is allowed
                return true;
            } else {
                RolesAllowed rolesAnnotation = viewBean.getBeanClass()
                        .getAnnotation(RolesAllowed.class);
                boolean hasAccess = accessControl
                        .isUserInSomeRole(rolesAnnotation.value());
                getLogger().log(
                        Level.FINE,
                        "Checking if user {0} is having access to {1}: {2}",
                        new Object[] { accessControl.getPrincipalName(),
                                viewBean, Boolean.toString(hasAccess) });

                return hasAccess;
            }
        }

        // No annotation defined, everyone is allowed
        return true;
    }

    private ViewBean getViewBean(String viewName) {
        getLogger().log(Level.FINE, "Looking for view with name \"{0}\"",
                viewName);
        Set<Bean<?>> matching = new HashSet<Bean<?>>();
        Set<Bean<?>> all = beanManager.getBeans(View.class, QUALIFIER_ANY);
        if (all.isEmpty()) {
            getLogger()
                    .severe("No Views found! Please add at least one class implementing the View interface.");
            return null;
        }
        for (Bean<?> bean : all) {
            Class<?> beanClass = bean.getBeanClass();
            CDIView viewAnnotation = beanClass.getAnnotation(CDIView.class);
            if (viewAnnotation == null) {
                continue;
            }

            String mapping = Conventions.deriveMappingForView(beanClass);
            getLogger().log(Level.FINER,
                    "{0} is annotated, the viewName is \"{1}\"",
                    new Object[] { beanClass.getName(), mapping });

            // In the case of an empty fragment, use the root view.
            // Note that the root view should not support parameters if other
            // views are used.

            if (viewAnnotation.supportsParameters()
                    && viewName.startsWith(mapping)) {
                matching.add(bean);
                getLogger()
                        .log(Level.FINER,
                                "Bean {0} with viewName \"{1}\" is one alternative for viewAndParameters \"{2}\"",
                                new Object[] { bean, mapping, viewName });
            } else if (viewName.equals(mapping)) {
                matching.add(bean);
                getLogger().log(Level.FINER,
                        "Bean {0} with viewName \"{1}\" is one alternative",
                        new Object[] { bean, mapping });
            }
        }

        Set<Bean<?>> viewBeansForThisProvider = getViewBeansForCurrentUI(matching);
        if (viewBeansForThisProvider.isEmpty()) {
            getLogger()
                    .log(Level.WARNING, "No view beans found for current UI");
            return null;
        }

        if (viewBeansForThisProvider.size() > 1) {
            throw new RuntimeException(
                    "Multiple views mapped with same name for same UI");
        }

        return new ViewBean(viewBeansForThisProvider.iterator().next(),
                viewName);
    }

    private Set<Bean<?>> getViewBeansForCurrentUI(Set<Bean<?>> beans) {
        Set<Bean<?>> viewBeans = new HashSet<Bean<?>>();

        for (Bean<?> bean : beans) {
            CDIView viewAnnotation = bean.getBeanClass().getAnnotation(
                    CDIView.class);

            if (viewAnnotation == null) {
                continue;
            }

            List<Class<? extends UI>> uiClasses = Arrays.asList(viewAnnotation
                    .uis());

            if (uiClasses.contains(UI.class)) {
                viewBeans.add(bean);
            } else {
                Class<? extends UI> currentUI = UI.getCurrent().getClass();
                for (Class<? extends UI> uiClass : uiClasses) {
                    if (uiClass.isAssignableFrom(currentUI)) {
                        viewBeans.add(bean);
                        break;
                    }
                }
            }
        }

        return viewBeans;
    }

    @Override
    public View getView(String viewName) {
        getLogger().log(Level.FINE,
                "Attempting to retrieve view with name \"{0}\"",
                viewName);
        ViewBean viewBean = getViewBean(viewName);
        if (viewBean != null) {
            if (!isUserHavingAccessToView(viewBean)) {
                getLogger().log(
                        Level.INFO,
                        "User {0} did not have access to view {1}",
                        new Object[] { accessControl.getPrincipalName(),
                                viewBean });
                return null;
            }

            if (currentViewCreationalContext != null) {
                getLogger().log(Level.FINER,
                        "Releasing creational context for current view {0}",
                        currentViewCreationalContext);
                currentViewCreationalContext.release();
            }

            currentViewCreationalContext = beanManager
                    .createCreationalContext(viewBean);
            getLogger().log(Level.FINER,
                    "Created new creational context for current view {0}",
                    currentViewCreationalContext);

            View view = (View) beanManager.getReference(viewBean,
                    viewBean.getBeanClass(), currentViewCreationalContext);
            getLogger().log(Level.FINE, "Returning view instance {0}", view.toString());

            UI currentUI = UI.getCurrent();
            if (currentUI != null) {
                Navigator navigator = currentUI.getNavigator();
                if (navigator != null) {
                    // This is a fairly dumb way of making sure that there is
                    // one and only one CDI viewChangeListener for this
                    // Navigator.
                    navigator.removeViewChangeListener(viewChangeListener);
                    navigator.addViewChangeListener(viewChangeListener);
                }
            }
            return view;
        }

        throw new RuntimeException("Unable to instantiate view");
    }

    @PreDestroy
    protected void destroy() {
        if (currentViewCreationalContext != null) {
            getLogger()
                    .log(Level.FINE,
                            "CDIViewProvider is being destroyed, releasing creational context for current view");
            currentViewCreationalContext.release();
        }
    }

    private String parseViewName(String viewAndParameters) {

        String viewName = viewAndParameters;
        if (viewName.startsWith("!")) {
            viewName = viewName.substring(1);
        }

        if (viewName.contains("/")) {
            viewName = viewName.split("/")[0];
        }

        return viewName;
    }

    private static Logger getLogger() {
        return Logger.getLogger(CDIViewProvider.class.getCanonicalName());
    }
}
