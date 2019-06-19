/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
package org.teiid.maven;

import static org.teiid.deployers.RestWarGenerator.REST_NAMESPACE;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.teiid.core.types.DataTypeManager;
import org.teiid.metadata.Column;
import org.teiid.metadata.ColumnSet;
import org.teiid.metadata.Database;
import org.teiid.metadata.Procedure;
import org.teiid.metadata.ProcedureParameter;
import org.teiid.metadata.Schema;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

class CustomApiGenerator {
    private File openApiFile;
    private File outputDirectory;
    private Log log;

    public CustomApiGenerator(File openApiFile, File outputDirectory, Log log) {
        this.openApiFile = openApiFile;
        this.outputDirectory = outputDirectory;
        this.log = log;
    }

    protected void generate(MustacheFactory mf, File javaSrcDir, Database database, HashMap<String, String> parentMap)
            throws Exception {

        // When OpenAPI file is not present it is assumed that metadata is on the VDB
        // then we try to generate from that.
        Swagger swagger = null;
        if (this.openApiFile.exists()) {
            log.info("OpenApi definition file found: " + this.openApiFile.getCanonicalPath());
            SwaggerParser parser = new SwaggerParser();
            swagger =  parser.read(this.openApiFile.getAbsolutePath(), null, true);

        } else {
            log.warn("No OpenApi definition file configured. Return types may be be defined in user friendly way");
        }

        // create swagger config file.
        if (swagger == null) {
            createSwaggerConfig(mf, javaSrcDir, database, parentMap);
        }

        // create api
        for (Schema schema : database.getSchemas()) {
            if (schema.isPhysical()) {
                continue;
            }

            HashMap<String, String> replacementMap = new HashMap<String, String>(parentMap);
            String schemaName = schema.getName();
            String modelName = schemaName.substring(0, 1).toUpperCase() + schemaName.substring(1);

            replacementMap.put("schemaName", schemaName);
            replacementMap.put("modelName", modelName);

            // try building the custom api
            generate(mf, javaSrcDir, replacementMap, schema);

            // try swagger based building now
            if (swagger != null) {
                //OpenApiGenerator openApi = new OpenApiGenerator(this.log);
                //openApi.generate(swagger, cfg, javaSrcDir, replacementMap, schema);
            }
        }
    }

    void generate(MustacheFactory mf, File javaSrcDir, HashMap<String, String> map, Schema schema)
            throws Exception {
        Mustache mustache = mf.compile(
                new InputStreamReader(getClass().getResourceAsStream("/templates/Controller.mustache")), "controller");
        File outFile = new File(javaSrcDir, map.get("modelName") + ".java");
        FileWriter out = new FileWriter(outFile);
        mustache.execute(out, map);

        mustache = mf.compile(new InputStreamReader(getClass().getResourceAsStream("/templates/api.mustache")),
                "service");

        boolean found = false;
        Collection<Procedure> procedures = schema.getProcedures().values();
        for (Procedure procedure : procedures) {
            String uri = procedure.getProperty(REST_NAMESPACE + "URI", false);
            String method = procedure.getProperty(REST_NAMESPACE + "METHOD", false);
            if ((uri != null && method != null)) {
                buildCustomRestService(procedure, map, mustache, out);
                found = true;
            }
        }
        out.write("}");
        out.flush();
        out.close();
        if (!found) {
            log.info("No DDL based metadata found for schema: " + schema.getName());
            outFile.delete();
        }
    }

    private void createSwaggerConfig(MustacheFactory mf, File javaSrcDir, Database database,
            HashMap<String, String> props) throws Exception {
        Mustache mustache = mf.compile(
                new InputStreamReader(getClass().getResourceAsStream("/templates/SwaggerConfig.mustache")), "config");
        Writer out = new FileWriter(new File(javaSrcDir, "SwaggerConfig.java"));
        mustache.execute(out, props);
        out.close();
    }

    private void buildCustomRestService(Procedure procedure, HashMap<String, String> replacementMap,
            Mustache mustache, FileWriter out) throws Exception {

        String uri = procedure.getProperty(REST_NAMESPACE + "URI", false);
        String method = procedure.getProperty(REST_NAMESPACE + "METHOD", false);

        String contentType = procedure.getProperty(REST_NAMESPACE + "PRODUCES", false);
        if (contentType == null) {
            contentType = findContentType(procedure);
        }

        String charSet = procedure.getProperty(REST_NAMESPACE + "CHARSET", false);
        if (charSet == null) {
            charSet = Charset.defaultCharset().name();
        }

        List<ProcedureParameter> params = new ArrayList<ProcedureParameter>(procedure.getParameters().size());
        boolean usingReturn = false;
        boolean hasLobInput = false;
        for (ProcedureParameter p : procedure.getParameters()) {
            if (p.getType() == ProcedureParameter.Type.In || p.getType() == ProcedureParameter.Type.InOut) {
                params.add(p);
            } else if (p.getType() == ProcedureParameter.Type.ReturnValue && procedure.getResultSet() == null) {
                usingReturn = true;
            }
            if (!hasLobInput) {
                String runtimeType = p.getRuntimeType();
                hasLobInput = DataTypeManager.isLOB(runtimeType);
            }
        }

        boolean useMultipart = false;
        if (method.toUpperCase().equals("POST") && hasLobInput) {
            useMultipart = true;
        }

        replacementMap.put("contentType", contentType);
        replacementMap.put("uri", uri);
        replacementMap.put("method", method);
        replacementMap.put("charset", charSet);
        replacementMap.put("description", procedure.getAnnotation() == null ? "" : procedure.getAnnotation());
        replacementMap.put("procedureName", procedure.getName());
        replacementMap.put("procedureFullName", procedure.getFullName());
        replacementMap.put("usingReturn", usingReturn?"true":"false");

        // handle parameters
        StringBuilder paramSignature = new StringBuilder();
        StringBuilder paramMapper = new StringBuilder();
        int paramsSize = params.size();

        if (useMultipart) {
            replacementMap.put("consumes", "multipart/form-data");
        } else {
            // post only accepts Form inputs, not path params
            boolean get = method.toUpperCase().equals("GET");

            if (paramsSize > 0 && !get) {
                replacementMap.put("consumes", "application/x-www-form-urlencoded");
            }
        }

        HashSet<String> pathParms = getPathParameters(uri);
        for (int i = 0; i < paramsSize; i++) {
            String runtimeType = params.get(i).getRuntimeType();
            String paramType = "@RequestParam(name=\"" + params.get(i).getName() + "\")";
            if (pathParms.contains(params.get(i).getName())) {
                paramType = "@PathVariable(name=\"" + params.get(i).getName() + "\")";
            }
            if (i > 0) {
                paramSignature.append(", ");
            }
            String type = params.get(i).getJavaType().getName();
            if (DataTypeManager.isLOB(runtimeType)) {
                type = "MultipartFile";
            }
            paramSignature.append(paramType).append(" ").append(type).append(" ")
            .append(params.get(i).getName());
            paramMapper.append("parameters.put(\"").append(params.get(i).getName()).append("\",")
            .append(params.get(i).getName()).append(");\n    ");
        }
        replacementMap.put("paramSignature", paramSignature.toString());
        replacementMap.put("paramMapping", paramMapper.toString());

        mustache.execute(out, replacementMap);
        out.write("\n");
    }

    static String findContentType(Procedure procedure) {
        String contentType = "plain";
        ColumnSet<Procedure> rs = procedure.getResultSet();
        if (rs != null) {
            Column returnColumn = rs.getColumns().get(0);
            String type = returnColumn.getDatatype().getRuntimeTypeName();
            if (type.equals(DataTypeManager.DefaultDataTypes.XML)) {
                contentType = "xml"; //$NON-NLS-1$
            }
            else if (type.equals(DataTypeManager.DefaultDataTypes.CLOB)
                    || type.equals(DataTypeManager.DefaultDataTypes.JSON)) {
                contentType = "json";
            }
        }
        else {
            for (ProcedureParameter pp:procedure.getParameters()) {
                if (pp.getType().equals(ProcedureParameter.Type.ReturnValue)) {
                    String type = pp.getDatatype().getRuntimeTypeName();
                    if (type.equals(DataTypeManager.DefaultDataTypes.XML)) {
                        contentType = "xml"; //$NON-NLS-1$
                    }
                    else if (type.equals(DataTypeManager.DefaultDataTypes.CLOB)
                            || type.equals(DataTypeManager.DefaultDataTypes.JSON)) {
                        contentType = "json"; //$NON-NLS-1$
                    }
                }
            }
        }
        contentType = contentType.toLowerCase().trim();
        if (contentType.equals("xml")) {
            contentType = "application/xml";
        } else if (contentType.equals("json")) {
            contentType = "application/json;charset=UTF-8";
        } else if (contentType.equals("plain")) {
            contentType = "text/plain";
        }
        return contentType;
    }

    static HashSet<String> getPathParameters(String uri ) {
        HashSet<String> pathParams = new HashSet<String>();
        String param;
        if (uri.contains("{")) {
            while (uri.indexOf("}") > -1) {
                int start = uri.indexOf("{");
                int end = uri.indexOf("}");
                param = uri.substring(start + 1, end);
                uri = uri.substring(end + 1);
                pathParams.add(param);
            }
        }
        return pathParams;
    }
}

