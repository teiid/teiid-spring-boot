/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.spring.data.util;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.teiid.spring.common.ExternalSource;
import org.teiid.spring.common.ExternalSources;
import org.teiid.translator.ExecutionFactory;

public class VerifySourcesTest {

    @Test public void verifyTranslators() {
        ExternalSources sources = new ExternalSources();
        for (ExternalSource source : sources.getItems().values()) {
            Class<? extends ExecutionFactory<?, ?>> clazz = ExternalSource
                    .translatorClass(source.getTranslatorName(), "org.teiid");
            assertNotNull("Missing implementation for " + source.getTranslatorName(), clazz);
        }

    }

}
