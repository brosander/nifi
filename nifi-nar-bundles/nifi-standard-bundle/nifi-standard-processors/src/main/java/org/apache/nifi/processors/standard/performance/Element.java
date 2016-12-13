/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.nifi.processors.standard.performance;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class Element implements Consumer<Iterator<String>> {
    private final ConcurrentMap<String, Element> children = new ConcurrentHashMap<>();
    private final AtomicLong count = new AtomicLong(0);

    @Override
    public void accept(Iterator<String> stackElements) {
        count.incrementAndGet();
        if (stackElements.hasNext()) {
            String next = stackElements.next();
            children.computeIfAbsent(next, s -> new Element()).accept(stackElements);
        }
    }

    public Map<String, Element> getChildren() {
        return new TreeMap<>(children);
    }

    public long getCount() {
        return count.get();
    }
}
