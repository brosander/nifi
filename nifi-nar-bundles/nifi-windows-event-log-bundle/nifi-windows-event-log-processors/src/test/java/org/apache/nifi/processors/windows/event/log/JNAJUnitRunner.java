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

package org.apache.nifi.processors.windows.event.log;

import com.sun.jna.Native;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;

import java.net.URLClassLoader;

public class JNAJUnitRunner extends Runner {
    public static final String NATIVE_CANONICAL_NAME = Native.class.getCanonicalName();
    public static final ClassLoader jnaMockClassloader = new URLClassLoader(((URLClassLoader) JNAJUnitRunner.class.getClassLoader()).getURLs(), null) {
        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals(NATIVE_CANONICAL_NAME)) {
                ClassPool classPool = ClassPool.getDefault();
                try {
                    CtClass ctClass = classPool.get(name);
                    for (CtMethod loadLibrary : ctClass.getDeclaredMethods("loadLibrary")) {
                        loadLibrary.setBody(null);
                    }

                    byte[] bytes = ctClass.toBytecode();
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (Exception e) {
                    throw new ClassNotFoundException(name, e);
                }
            } else if (name.startsWith("org.junit.")) {
                return JNAJUnitRunner.class.getClassLoader().loadClass(name);
            }
            return super.loadClass(name, resolve);
        }
    };

    private final Runner delegate;

    public JNAJUnitRunner(Class<?> klass) throws InitializationError {
        try {
            delegate = new JUnit4(jnaMockClassloader.loadClass(klass.getCanonicalName()));
        } catch (ClassNotFoundException e) {
            throw new InitializationError(e);
        }
    }

    @Override
    public Description getDescription() {
        return delegate.getDescription();
    }

    @Override
    public void run(RunNotifier notifier) {
        delegate.run(notifier);
    }
}
