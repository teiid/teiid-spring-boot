/*
 * Copyright 2012-2014 the original author or authors.
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

package org.teiid.spring.autoconfigure;

import org.springframework.context.ApplicationEvent;

/**
 * {@link ApplicationEvent} used internally to trigger {@link TeiidServer}
 * initialization. During the initialization {@literal teiid.ddl} file is loaded
 * if available on the classpath
 *
 * @see TeiidInitializer
 */
@SuppressWarnings("serial")
public class TeiidInitializedEvent extends ApplicationEvent {

    /**
     * Create a new {@link TeiidInitializedEvent}.
     *
     * @param source
     *            the source {@link TeiidServer}.
     */
    public TeiidInitializedEvent(TeiidServer source) {
        super(source);
    }
}
