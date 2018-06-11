/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.cdi.itest;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class Counter {
    ConcurrentHashMap<String, AtomicInteger> map = new ConcurrentHashMap<>();

    public int increment(String key) {
        map.putIfAbsent(key, new AtomicInteger(0));
        return map.get(key).incrementAndGet();
    }

    public int get(String key) {
        map.putIfAbsent(key, new AtomicInteger(0));
        return map.get(key).get();
    }

    public void reset() {
        map.clear();
    }
}
