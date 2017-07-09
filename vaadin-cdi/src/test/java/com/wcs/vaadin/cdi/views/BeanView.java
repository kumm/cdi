package com.wcs.vaadin.cdi.views;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.wcs.vaadin.cdi.internal.MyBean;
import com.vaadin.ui.Label;

public class BeanView extends Label {
    @Inject
    MyBean bean;

    @PostConstruct
    private void populate() {
        setValue("Bean " + bean.getBeanId());
    }
}
