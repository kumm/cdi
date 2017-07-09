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

package com.wcs.vaadin.cdi.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import com.wcs.vaadin.cdi.views.OneAndOnlyViewWithPath;
import org.junit.Test;

import com.wcs.vaadin.cdi.uis.ConventionalUI;
import com.wcs.vaadin.cdi.uis.PlainColidingAlternativeUI;
import com.wcs.vaadin.cdi.views.OneAndOnlyViewWithoutPath;
import com.wcs.vaadin.cdi.views.OneAndOnlyViewWithoutPathAndAnnotation;

/**
 */
public class ConventionsTest {

    @Test
    public void extractViewNameUsingPath() {
        String expected = "customTest";
        String actual = Conventions.deriveMappingForView(OneAndOnlyViewWithPath.class);
        assertThat(actual, is(expected));
    }

    @Test
    public void extractViewNameUsingConvention() {
        String expected = "one-and-only-view-without-path";
        String actual = Conventions.deriveMappingForView(OneAndOnlyViewWithoutPath.class);
        assertThat(actual, is(expected));
    }

    @Test
    public void extractViewNameUsingConventionWithoutAnnotation() {
        String expected = null;
        String actual = Conventions.deriveMappingForView(OneAndOnlyViewWithoutPathAndAnnotation.class);
        assertThat(actual, is(expected));
    }

    @Test
    public void extractUIPathUsingConvention() {
        String expected = "conventional";
        String actual = Conventions.deriveMappingForUI(ConventionalUI.class);
        assertThat(actual, is(expected));
    }

    @Test
    public void extractUIPathUsingAnnotation() {
        String expected = "PlainUI";
        String actual = Conventions.deriveMappingForUI(PlainColidingAlternativeUI.class);
        assertThat(actual, is(expected));
    }

    @Test
    public void uiAnnotationNotPresent() {
        final String uiPath = Conventions.deriveMappingForUI(String.class);
        assertNull(uiPath);
    }
    
    @Test
    public void upperCamelCaseToLowerHyphenatedTest() {
        String original = "AlphaBetaGamma";
        String expected = "alpha-beta-gamma";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "alphaBetaGamma";
        expected = "alpha-beta-gamma";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "";
        expected = "";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "a";
        expected = "a";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "A";
        expected = "a";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "ABC";
        expected = "abc";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "alllowercase";
        expected = "alllowercase";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "main/sub";
        expected = "main/sub";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "MyCDITest";
        expected = "my-cdi-test";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "MyATest";
        expected = "my-a-test";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "aB";
        expected = "a-b";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "Ab";
        expected = "ab";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));

        original = "MyCDI";
        expected = "my-cdi";
        assertThat(Conventions.upperCamelToLowerHyphen(original), is(expected));
    }

}
