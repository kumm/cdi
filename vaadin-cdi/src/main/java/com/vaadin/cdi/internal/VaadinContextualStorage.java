package com.vaadin.cdi.internal;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.deltaspike.core.util.context.ContextualStorage;

/**
 * Customized version of ContextualStorage to also handle beans that are not
 * PassivationCapable. Such beans are used as their own keys, which is not ideal
 * but should work in most single-JVM environments.
 * 
 * @see ContextualStorage
 */
public class VaadinContextualStorage extends ContextualStorage {
    private final BeanManager beanManager;

    public VaadinContextualStorage(BeanManager beanManager, boolean concurrent) {
        super(beanManager, concurrent, false);
        this.beanManager = beanManager;
    }

    @Override
    public <T> Object getBeanKey(Contextual<T> bean) {
        if ((bean instanceof UIContextual)) {
            // Need the bean to destroy the contextual instance it properly.
            // Even if the delegate bean itself is not passivation capable,
            // we're still generating a dummy passivation id.
            // Since we cannot rely on passivation id to restore it,
            // use delegate bean as a key.
            return ((UIContextual) bean).delegate;
        } else {
            return super.getBeanKey(bean);
        }
    }

}
