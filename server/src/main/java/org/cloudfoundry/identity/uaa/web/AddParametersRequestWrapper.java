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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AddParametersRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String,String[]> addedParameters;

    public AddParametersRequestWrapper(Map<String, String[]> parameters, HttpServletRequest request) {
        super(request);
        addedParameters = Collections.unmodifiableMap(parameters);
    }

    @Override
    public String getParameter(String name) {
        String[] result = getParameterValues(name);
        return result==null || result.length==0 ? null : result[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> result = new HashMap<>(super.getParameterMap());
        result.putAll(addedParameters);
        return result;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return new ConcatenateEnumeration<String>(
            Arrays.asList(
                new IteratorEnumeration(addedParameters.keySet().iterator()),
                super.getParameterNames()
            )
        );
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] result = addedParameters.get(name);
        return result == null ? super.getParameterValues(name) : result;
    }

    public static class ConcatenateEnumeration<T> implements Enumeration<T> {

        private final List<Enumeration<T>> enumerations;

        public ConcatenateEnumeration(List<Enumeration<T>> enumerations) {
            this.enumerations = enumerations;
        }

        @Override
        public boolean hasMoreElements() {
            for (Enumeration e : enumerations) {
                if (e.hasMoreElements()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public T nextElement() {
            for (Enumeration<T> e : enumerations) {
                if (e.hasMoreElements()) {
                    return e.nextElement();
                }
            }
            throw new IllegalStateException("No more elements");
        }
    }
}
