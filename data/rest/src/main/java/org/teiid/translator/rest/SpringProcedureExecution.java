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

package org.teiid.translator.rest;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.teiid.core.types.BlobType;
import org.teiid.language.Argument;
import org.teiid.language.Call;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.spring.data.rest.RestConnection;
import org.teiid.translator.DataNotAvailableException;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ProcedureExecution;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.ws.BinaryWSProcedureExecution.StreamingBlob;

@SuppressWarnings("unused")
public class SpringProcedureExecution implements ProcedureExecution {

    private RuntimeMetadata metadata;
    private ExecutionContext context;
    private Call procedure;
    private RestConnection conn;
    private SpringRestExecutionFactory executionFactory;
    private BlobType returnValue;

    public SpringProcedureExecution(Call command, RuntimeMetadata metadata, ExecutionContext executionContext,
            SpringRestExecutionFactory springRestExecutionFactory, RestConnection connection) {
        this.metadata = metadata;
        this.context = executionContext;
        this.procedure = command;
        this.conn = connection;
        this.executionFactory = springRestExecutionFactory;
    }

    @Override
    public List<?> next() throws TranslatorException, DataNotAvailableException {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void cancel() throws TranslatorException {
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void execute() throws TranslatorException {
        List<Argument> arguments = this.procedure.getArguments();
        String beanName = (String)arguments.get(0).getArgumentValue().getValue();
        Function f = (Function)this.conn.getBeanFactory().getBean(beanName);
        Object result = f.apply(this.context);
        if (result instanceof byte[]) {
            this.returnValue = new BlobType((byte[])result);
        } else if (result instanceof String) {
            this.returnValue = new BlobType(((String)result).getBytes());
        } else if (result instanceof InputStream) {
            this.returnValue = new BlobType(new StreamingBlob((InputStream)result));
        } else {
            throw new TranslatorException("Failed to get results from REST based call in bean " + beanName);
        }
    }

    @Override
    public List<?> getOutputParameterValues() throws TranslatorException {
        return Arrays.asList(returnValue);
    }
}
