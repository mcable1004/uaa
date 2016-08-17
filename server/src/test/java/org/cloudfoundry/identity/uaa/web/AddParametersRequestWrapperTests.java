/*
 * *****************************************************************************
 *      Cloud Foundry
 *      Copyright (c) [2009-2016] Pivotal Software, Inc. All Rights Reserved.
 *      This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *      You may not use this product except in compliance with the License.
 *
 *      This product includes a number of subcomponents with
 *      separate copyright notices and license terms. Your use of these
 *      subcomponents is subject to the terms and conditions of the
 *      subcomponent's license, as noted in the LICENSE file.
 * *****************************************************************************
 */

package org.cloudfoundry.identity.uaa.web;

import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddParametersRequestWrapperTests {

    HttpServletRequest request = mock(HttpServletRequest.class);
    String name0 = "name0";
    String[] value0 = {"value0a","value0b"};

    @Before
    public void setup() {
        Map<String, String[]> defaultValues = new HashMap<>();
        defaultValues.put(name0, value0);

        when(request.getParameterMap()).thenReturn(defaultValues);
        when(request.getParameterNames()).thenReturn(new IteratorEnumeration(defaultValues.keySet().iterator()));
        when(request.getParameter(eq(name0))).thenReturn(value0[0]);
        when(request.getParameter(not(eq(name0)))).thenReturn(null);
        when(request.getParameterValues(eq(name0))).thenReturn(value0);
        when(request.getParameterValues(not(eq(name0)))).thenReturn(null);
    }

    @Test
    public void testParameterAdded(){
        Map<String, String[]> addedValues = new HashMap<>();
        String name1 = "name1", name2 = "name2";
        String[] value1 = {"value1a","value1b"}, value2 = {"value2"};
        addedValues.put(name1, value1);
        addedValues.put(name2, value2);
        HttpServletRequest req = new AddParametersRequestWrapper(addedValues, this.request);

        assertSame(value1, req.getParameterValues(name1));
        assertSame(value2, req.getParameterValues(name2));
        assertEquals(value1[0], req.getParameter(name1));
        assertEquals(value2[0], req.getParameter(name2));

        Map<String,String[]> values = req.getParameterMap();
        assertEquals(3, values.size());
        assertTrue(values.containsKey(name0));
        assertTrue(values.containsKey(name1));
        assertTrue(values.containsKey(name2));
        assertTrue(values.containsValue(value0));
        assertTrue(values.containsValue(value1));
        assertTrue(values.containsValue(value2));

        Enumeration<String> names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String s = names.nextElement();
            assertTrue(s, Arrays.asList(name0, name1, name2).contains(s));
        }

    }


}