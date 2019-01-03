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
package org.teiid.spring.views;

import static org.teiid.spring.autoconfigure.TeiidConstants.VDBNAME;
import static org.teiid.spring.autoconfigure.TeiidConstants.VDBVERSION;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;

import org.hibernate.boot.Metadata;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.core.types.DataTypeManager;
import org.teiid.metadata.FunctionMethod;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Procedure;
import org.teiid.metadata.ProcedureParameter;
import org.teiid.metadata.Schema;
import org.teiid.query.metadata.DDLStringVisitor;
import org.teiid.query.metadata.SystemMetadata;
import org.teiid.spring.annotations.JavaFunction;
import org.teiid.spring.annotations.SourceFunction;
import org.teiid.spring.annotations.UserDefinedFunctions;

public class UDFProcessor {
    @SuppressWarnings("unused")
    private Metadata metadata;
    private VDBMetaData vdb;
    private Map<String, List<Procedure>> functionMap = new LinkedHashMap<>();

    public UDFProcessor(Metadata metadata, VDBMetaData vdb) {
        this.metadata = metadata;
        this.vdb = vdb;
    }

    public void buildFunctions(Class<?> clazz, MetadataFactory mf, UserDefinedFunctions parent) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getAnnotation(SourceFunction.class) != null) {
                SourceFunction f = method.getAnnotation(SourceFunction.class);
                Procedure p = createSourceFunction(method);
                register(f.source(), p);
            } else if (method.getAnnotation(JavaFunction.class) != null) {
                createJavaFunction(method, mf);
            } else {
                // ignore
            }
        }
    }

    private void register(String source, Procedure p) {
        List<Procedure> list = functionMap.get(source);
        if (list == null) {
            list = new ArrayList<>();
            functionMap.put(source, list);
        }
        list.add(p);
    }

    private void createJavaFunction(Method method, MetadataFactory mf) {
        JavaFunction f = method.getAnnotation(JavaFunction.class);
        FunctionMethod fm = MetadataFactory.createFunctionFromMethod(method.getName(), method);
        if (f.nullOnNull()) {
            fm.setNullOnNull(true);
        }
        fm.setDeterminism(f.determinism());
        fm.setPushdown(f.pushdown());
        mf.addFunction(fm);
    }

    private Procedure createSourceFunction(Method method) {
        SourceFunction func = method.getAnnotation(SourceFunction.class);
        if (vdb.getModel(func.source()) == null) {
            throw new IllegalArgumentException(
                    "The source name " + " used on function " + method.getName() + "does not exist");
        }

        ModelMetaData model = new ModelMetaData();
        model.setName(func.source());
        model.setModelType(Model.Type.PHYSICAL);
        MetadataFactory mf = new MetadataFactory(VDBNAME, VDBVERSION, SystemMetadata.getInstance().getRuntimeTypeMap(),
                model);

        Procedure p = mf.addProcedure(method.getName());
        p.setFunction(true);
        p.setVirtual(false);
        for (Parameter param : method.getParameters()) {
            mf.addProcedureParameter(param.getName(),
                    DataTypeManager.getDataTypeName(DataTypeManager.getRuntimeType(param.getType())),
                    ProcedureParameter.Type.In, p);
        }
        mf.addProcedureParameter("return",
                DataTypeManager.getDataTypeName(DataTypeManager.getRuntimeType(method.getReturnType())),
                ProcedureParameter.Type.ReturnValue, p);
        if (func.nativequery() != null) {
            p.setProperty("teiid_rel:native-query", func.nativequery());
        }
        return p;
    }

    public void buildSequence(Class<?> clazz, MetadataFactory viewMF, Entity entityAnnotation) {
        for (Field f : clazz.getDeclaredFields()) {
            GeneratedValue gv = f.getAnnotation(GeneratedValue.class);
            if (gv != null && gv.strategy().equals(GenerationType.SEQUENCE)) {
                SequenceGenerator sg = f.getAnnotation(SequenceGenerator.class);
                if (sg != null && sg.sequenceName() != null) {

                    String[] seqNames = sg.sequenceName().split("\\.");
                    if (seqNames.length != 2) {
                        throw new IllegalArgumentException("The sequence name on " + clazz.getName()
                                + " must in the format \"datasourceName.sequenceName\" where sequence is defined.");
                    }
                    ModelMetaData model = vdb.getModelMetaDatas().get(seqNames[0]);
                    if (model == null) {
                        throw new IllegalArgumentException(
                                "The sequence name on " + clazz.getName() + " referenced data source by name "
                                        + seqNames[0] + " which is not found in configuration");
                    }
                    MetadataFactory mf = new MetadataFactory(VDBNAME, VDBVERSION,
                            SystemMetadata.getInstance().getRuntimeTypeMap(), model);

                    Procedure p = mf.addProcedure(seqNames[1]);
                    p.setFunction(true);
                    p.setVirtual(false);
                    mf.addProcedureParameter("return",
                            DataTypeManager.getDataTypeName(DataTypeManager.getRuntimeType(f.getType())),
                            ProcedureParameter.Type.ReturnValue, p);

                    String nativeQuery = "NEXTVAL('" + seqNames[1] + "');";
                    String translatorName = model.getSources().values().iterator().next().getTranslatorName();
                    if (translatorName.equalsIgnoreCase("oracle")) {
                        nativeQuery = seqNames[1] + ".NEXTVAL FROM DUAL;";
                    } else if (translatorName.equalsIgnoreCase("sqlserver")) {
                        nativeQuery = " NEXT VALUE FOR " + seqNames[1] + ";";
                    } else if (translatorName.equalsIgnoreCase("db2")) {
                        nativeQuery = seqNames[1] + ".NEXTVAL;";
                    }
                    p.setProperty("teiid_rel:native-query", nativeQuery);
                    register(seqNames[0], p);

                    String returnType = DataTypeManager.getDataTypeName(DataTypeManager.getRuntimeType(f.getType()));
                    Procedure viewP = viewMF.addProcedure(seqNames[1] + "_nextval");
                    viewP.setVirtual(true);
                    viewP.setFunction(true);
                    viewMF.addProcedureParameter("return", returnType, ProcedureParameter.Type.ReturnValue, viewP);
                    viewP.setQueryPlan("" + "BEGIN\n" + "DECLARE " + returnType + " VARIABLES.X = SELECT " + seqNames[0]
                            + "." + seqNames[1] + "();\n" + "RETURN X;\n" + "END");
                } else {
                    throw new IllegalArgumentException("The sequence generation on " + clazz.getName()
                            + " does not have sequence defined. Define @SequenceGenerator "
                            + "annotation with sequence name");
                }
            }
        }
    }

    public void finishProcessing() {
        for (ModelMetaData model : vdb.getModelMetaDatas().values()) {
            if (this.functionMap.get(model.getName()) != null) {
                Schema s = new Schema();
                List<Procedure> list = this.functionMap.get(model.getName());
                for (Procedure p : list) {
                    s.addProcedure(p);
                }
                String ddl = DDLStringVisitor.getDDLString(s, null, null);
                model.addSourceMetadata("DDL", ddl);
            }
        }
    }
}
