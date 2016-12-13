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

import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class Benchmark {
    private final Set<Thread> allWatchedThreads = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Thread> currentlyWatchedThreads = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Element element = new Element();
    private final Supplier<Long> delaySupplier;
    private final AtomicBoolean stopped = new AtomicBoolean();

    public static Supplier<Long> makeRandomSupplier(int maxValue) {
        Random random = new Random();
        return () -> Long.valueOf(random.nextInt(maxValue));
    }

    public Benchmark(Supplier<Long> delaySupplier) {
        this.delaySupplier = delaySupplier;
    }

    public void enter(Thread thread) {
        currentlyWatchedThreads.add(thread);
        if (allWatchedThreads.add(thread)) {
            Thread benchmarkThread = new Thread(() -> {
                try {
                    long lastRunTime = System.currentTimeMillis();
                    while (!stopped.get()) {
                        Thread.sleep(Math.max(0, (lastRunTime + delaySupplier.get()) - System.currentTimeMillis()));
                        Iterator<String> iterator = Lists.reverse(Arrays.asList(thread.getStackTrace())).stream().map(Object::toString).iterator();
                        if (currentlyWatchedThreads.contains(thread)) {
                            element.accept(iterator);
                        }
                        lastRunTime = System.currentTimeMillis();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    allWatchedThreads.remove(thread);
                }
            });
            benchmarkThread.start();
        }
    }

    public void exit(Thread thread) {
        currentlyWatchedThreads.remove(thread);
    }

    public void stop() {
        currentlyWatchedThreads.clear();
        stopped.set(true);
        currentlyWatchedThreads.clear();
    }

    public Element getElement() {
        return element;
    }
}
