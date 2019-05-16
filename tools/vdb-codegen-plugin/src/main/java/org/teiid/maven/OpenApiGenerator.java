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
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.logging.Log;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.teiid.core.types.DataTypeManager;
import org.teiid.metadata.Column;
import org.teiid.metadata.ColumnSet;
import org.teiid.metadata.Database;
import org.teiid.metadata.Procedure;
import org.teiid.metadata.ProcedureParameter;
import org.teiid.metadata.Schema;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.swagger.models.HttpMethod;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

public class OpenApiGenerator {
    private File openApiFile;
    private File outputDirectory;
    private Log log;

    public OpenApiGenerator(File openApiFile, File outputDirectory, Log log) {
        this.openApiFile = openApiFile;
        this.outputDirectory = outputDirectory;
        this.log = log;
    }

    protected boolean isOpenApiDefinitionExists(Swagger swagger, Procedure p) {
        if (swagger == null) {
            return false;
        }
        for(Entry<String, Path> entry : swagger.getPaths().entrySet()) {
            Map<HttpMethod, Operation> operations = getOperationMap(entry.getValue());
            for (Operation op : operations.values()) {
                if (op.getOperationId().contentEquals(p.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    static class ApiOperation {
        HttpMethod method;
        String path;
        Operation op;
    }

    protected ApiOperation getOpenApiDefinition(Swagger swagger, Procedure p) {
        if (swagger == null) {
            return null;
        }
        for(Entry<String, Path> entry : swagger.getPaths().entrySet()) {
            Map<HttpMethod, Operation> operations = getOperationMap(entry.getValue());
            for (Entry<HttpMethod, Operation> e : operations.entrySet()) {
                if (e.getValue().getOperationId().contentEquals(p.getName())) {
                    ApiOperation oper = new ApiOperation();
                    oper.method = e.getKey();
                    oper.op = e.getValue();
                    oper.path = entry.getKey();
                    log.info("OpenApi definition found for procedure " + p.getFullName() );
                    return oper;
                }
            }
        }
        return null;
    }

    protected void generate(Configuration cfg, File javaSrcDir, Database database, HashMap<String, String> parentMap)
            throws Exception {

        Swagger swagger = null;
        if (this.openApiFile.exists()) {
            log.info("OpenApi definition file found: " + this.openApiFile.getCanonicalPath());
            SwaggerParser parser = new SwaggerParser();
            swagger =  parser.read(this.openApiFile.getAbsolutePath(), null, true);
            generateModels(this.openApiFile, parentMap.get("packageName"), outputDirectory.getAbsolutePath());
        } else {
            log.warn("No OpenApi definition file configured. Return types may be be defined in user friendly way");
        }

        // create swagger config file.
        createSwaggerConfig(cfg, javaSrcDir, database, parentMap);

        // create api
        for (Schema schema : database.getSchemas()) {
            if (schema.isPhysical()) {
                continue;
            }

            HashMap<String, String> replacementMap = new HashMap<String, String>(parentMap);
            replacementMap.put("modelName", schema.getName());

            Template template = cfg.getTemplate("Controller.java");
            FileWriter out = new FileWriter(new File(javaSrcDir, schema.getName() + ".java"));
            template.process(replacementMap, out);

            String servicePattern =
                    "    @RequestMapping(value = \"${uri}\", method = RequestMethod.${method}, produces = {\"${contentType}\"} <#if consumes??>, consumes = \"${consumes}\" </#if>)\n" +
                            "    @ResponseBody\n" +
                            "    @ApiOperation(value=\"${description}\"<#if responseClass??>, response=${responseClass}</#if>)\n" +
                            "    public ResponseEntity<InputStreamResource> ${procedureName}(${paramSignature}) {\n" +
                            "        setServer(this.server);\n"+
                            "        setVdb(this.vdb);\n"+
                            "        LinkedHashMap<String, Object> parameters = new LinkedHashMap<String, Object>();\n" +
                            "        ${paramMapping}\n" +
                            "        return execute(\"${procedureFullName}\", parameters, \"${charset}\", ${usingReturn});\n" +
                            "    }\n";
            StringTemplateLoader stl = new StringTemplateLoader();
            stl.putTemplate("service", servicePattern);
            cfg.setTemplateLoader(stl);

            Collection<Procedure> procedures = schema.getProcedures().values();
            for (Procedure procedure : procedures) {
                String uri = procedure.getProperty(REST_NAMESPACE + "URI", false);
                String method = procedure.getProperty(REST_NAMESPACE + "METHOD", false);
                if ((uri != null && method != null) || isOpenApiDefinitionExists(swagger, procedure)) {
                    buildRestService(swagger, procedure, replacementMap, cfg, out);
                }
            }
            out.write("}");
            out.flush();
            out.close();
        }
    }

    private void buildRestService(Swagger swagger, Procedure procedure, HashMap<String, String> replacementMap,
            Configuration cfg, FileWriter out) throws Exception {

        ApiOperation api = getOpenApiDefinition(swagger, procedure);
        String uri = procedure.getProperty(REST_NAMESPACE + "URI", false);
        if (uri == null && api != null) {
            uri = api.path;
        }

        String method = procedure.getProperty(REST_NAMESPACE + "METHOD", false);
        if (method == null && api != null) {
            method = api.method.name();
        }

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
        replacementMap.put("description", procedure.getAnnotation() == null ?
                (api == null ? "" : api.op.getDescription()) : procedure.getAnnotation());
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

        // return type
        String responseClass = createResponseClass(api);
        if (!responseClass.isEmpty()) {
            replacementMap.put("responseClass", responseClass);
        }

        Template template = cfg.getTemplate("service");
        template.process(replacementMap, out);
        out.write("\n");
    }

    private void createSwaggerConfig(Configuration cfg, File javaSrcDir, Database database, HashMap<String, String> props)
            throws Exception {
        Template template = cfg.getTemplate("SwaggerConfig.java");
        Writer out = new FileWriter(new File(javaSrcDir, "SwaggerConfig.java"));
        template.process(props, out);
        out.close();
    }

    private static HashSet<String> getPathParameters(String uri ) {
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

    private String findContentType(Procedure procedure) {
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

    private String createResponseClass(ApiOperation api) {
        boolean returnAdded = false;
        Response resp = null;

        if (api == null) {
            return "";
        }

        Map<String, Response> respMap = api.op.getResponses();
        for (String code : respMap.keySet()) {
            if (code.equalsIgnoreCase("default")) {
                continue;
            }
            int httpCode = Integer.valueOf(code);
            // Success codes
            if (httpCode > 100 && httpCode < 300) {
                resp = respMap.get(code);
                break;
            }
        }

        if (!returnAdded && respMap.get("default") != null) {
            resp = respMap.get("default");
        }

        if (resp != null) {
            Model m = resp.getResponseSchema();
            if (m.getReference() != null) {
                return m.getReference();
            }
            return "";
        }
        return "";
    }

    private void generateModels(File file, String packageName, String outputDir) {
        CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setPackageName(packageName);
        configurator.setModelPackage(packageName);
        configurator.addSystemProperty("models", "");
        configurator.setInputSpec(file.getAbsolutePath());
        configurator.setGeneratorName("org.openapitools.codegen.languages.JavaUndertowServerCodegen");

        final ClientOptInput input = configurator.toClientOptInput();
        configurator.setOutputDir(outputDir);
        new DefaultGenerator().opts(input).generate();

    }

    public static Map<HttpMethod, Operation> getOperationMap(Path path) {
        Map<HttpMethod, Operation> result = new LinkedHashMap<HttpMethod, Operation>();

        if (path.getGet() != null) {
            result.put(HttpMethod.GET, path.getGet());
        }
        if (path.getPut() != null) {
            result.put(HttpMethod.PUT, path.getPut());
        }
        if (path.getPost() != null) {
            result.put(HttpMethod.POST, path.getPost());
        }
        if (path.getDelete() != null) {
            result.put(HttpMethod.DELETE, path.getDelete());
        }
        if (path.getPatch() != null) {
            result.put(HttpMethod.PATCH, path.getPatch());
        }
        if (path.getHead() != null) {
            result.put(HttpMethod.HEAD, path.getHead());
        }
        if (path.getOptions() != null) {
            result.put(HttpMethod.OPTIONS, path.getOptions());
        }
        return result;
    }
}
