/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.teiid.translator.rest;

import org.teiid.language.Call;
import org.teiid.metadata.BaseColumn.NullType;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Procedure;
import org.teiid.metadata.ProcedureParameter;
import org.teiid.metadata.ProcedureParameter.Type;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.spring.data.rest.RestConnection;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ProcedureExecution;
import org.teiid.translator.Translator;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.TypeFacility;
import org.teiid.translator.ws.WSConnection;
import org.teiid.translator.ws.WSExecutionFactory;

@Translator(name="rest", description="A translator for making Web Service calls")
public class SpringRestExecutionFactory extends WSExecutionFactory{

    private static final String SPRING_HTTP = "springHttp";

    public SpringRestExecutionFactory() {
        this.setDefaultBinding(Binding.HTTP);
    }

    @Override
    public void getMetadata(MetadataFactory metadataFactory, WSConnection conn) throws TranslatorException {
        super.getMetadata(metadataFactory, conn);

        //only expose invokeHttp.  technically invoke with http binding is supportable
        metadataFactory.getSchema().removeProcedure("invoke");

        Procedure p = metadataFactory.addProcedure(SPRING_HTTP);
        p.setAnnotation("Invokes a webservice that returns an binary result"); //$NON-NLS-1$

        metadataFactory.addProcedureParameter("result", TypeFacility.RUNTIME_NAMES.BLOB, Type.ReturnValue, p); //$NON-NLS-1$

        ProcedureParameter param = metadataFactory.addProcedureParameter("bean", TypeFacility.RUNTIME_NAMES.STRING, //$NON-NLS-1$
                Type.In, p);
        param.setAnnotation("Sets the name of the bean"); //$NON-NLS-1$
        param.setNullType(NullType.No_Nulls);
    }

    @Override
    public ProcedureExecution createProcedureExecution(Call command, ExecutionContext executionContext, RuntimeMetadata metadata, WSConnection connection)
            throws TranslatorException {
        if (command.getProcedureName().equalsIgnoreCase(SPRING_HTTP)) {
            return new SpringProcedureExecution(command, metadata, executionContext, this, (RestConnection) connection);
        }
        return super.createProcedureExecution(command, executionContext, metadata, connection);
    }
}
