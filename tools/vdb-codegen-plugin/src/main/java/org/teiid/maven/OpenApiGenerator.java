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
import org.teiid.core.types.DataTypeManager;
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
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.RefParameter;

class OpenApiGenerator {
    private Log log;

    static class ApiOperation {
        HttpMethod method;
        String path;
        Operation op;
    }

    public OpenApiGenerator(Log log) {
        this.log = log;
    }

    public void generate(Swagger swagger, Configuration cfg, File javaSrcDir, HashMap<String, String> map, Schema schema)
            throws Exception {
        Template template = cfg.getTemplate("OpenApiController.java");
        File outFile = new File(javaSrcDir, map.get("modelName") + "Api.java");
        FileWriter out = new FileWriter(outFile);
        template.process(map, out);

        String servicePattern =
                "    @RequestMapping(value = \"${uri}\", method = RequestMethod.${method}, produces = {\"${contentType}\"} <#if consumes??>, consumes = \"${consumes}\" </#if>)\n" +
                        "    @ResponseBody\n" +
                        "    @ApiOperation(value=\"${description}\"<#if responseClass??>, response=${responseClass}</#if>)\n" +
                        "    public ResponseEntity<${returnClass}> ${procedureName}(${paramSignature}) {\n" +
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
            buildOpenApiService(swagger, procedure, map, cfg, out);
        }
        out.write("}");
        out.flush();
        out.close();
    }

    private void buildOpenApiService(Swagger swagger, Procedure procedure, HashMap<String, String> replacementMap,
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
            contentType = ApiGenerator.findContentType(procedure);
        }

        String charSet = procedure.getProperty(REST_NAMESPACE + "CHARSET", false);
        if (charSet == null) {
            charSet = Charset.defaultCharset().name();
        }

        List<ProcedureParameter> params = new ArrayList<ProcedureParameter>(procedure.getParameters().size());
        boolean usingReturn = false;
        boolean hasLobInput = false;
        String lobType = null;
        for (ProcedureParameter p : procedure.getParameters()) {
            if (p.getType() == ProcedureParameter.Type.In || p.getType() == ProcedureParameter.Type.InOut) {
                params.add(p);
            } else if (p.getType() == ProcedureParameter.Type.ReturnValue && procedure.getResultSet() == null) {
                usingReturn = true;
            }
            if (!hasLobInput) {
                lobType = p.getRuntimeType();
                hasLobInput = DataTypeManager.isLOB(lobType);
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
            if (DataTypeManager.DefaultDataTypes.XML.equals(lobType)) {
                replacementMap.put("consumes", "application/xml");
            } else if (DataTypeManager.DefaultDataTypes.JSON.equals(lobType)) {
                replacementMap.put("consumes", "application/json");
            } else {
                replacementMap.put("consumes", "multipart/form-data");
            }
        } else {
            // post only accepts Form inputs, not path params
            boolean get = method.toUpperCase().equals("GET");

            if (paramsSize > 0 && !get) {
                replacementMap.put("consumes", "application/x-www-form-urlencoded");
            }
        }

        HashSet<String> pathParms = ApiGenerator.getPathParameters(uri);
        for (int i = 0; i < paramsSize; i++) {
            String runtimeType = params.get(i).getRuntimeType();

            Parameter param = api.op.getParameters().get(i);
            String apiDescription = "@ApiParam(value=\""+param.getDescription()+"\",required="+param.getRequired()+") ";

            String paramType = null;
            if (param instanceof QueryParameter) {
                paramType = "@RequestParam(name=\"" + params.get(i).getName() + "\")";
            } else if (param instanceof PathParameter) {
                paramType = "@PathVariable(name=\"" + params.get(i).getName() + "\")";
            } else if (param instanceof BodyParameter) {
                paramType = "@RequestBody";
            } else if (param instanceof FormParameter) {
                // TODO:??
                paramType = "";
            }

            if (i > 0) {
                paramSignature.append(", ");
            }

            String type = params.get(i).getJavaType().getName();
            if (DataTypeManager.isLOB(runtimeType)) {
                if (param.getIn().contentEquals("body")) {
                    if (param instanceof RefParameter) {
                        type = ((RefParameter)param).get$ref();
                    }
                } else {
                    type = "MultipartFile";
                }
            }

            paramSignature.append(apiDescription).append(paramType).append(" ").append(type).append(" ").append(params.get(i).getName());
            paramMapper.append("parameters.put(\"").append(params.get(i).getName()).append("\",").append(params.get(i).getName()).append(");\n    ");
        }
        replacementMap.put("paramSignature", paramSignature.toString());
        replacementMap.put("paramMapping", paramMapper.toString());

        // return type
        String responseClass = createResponseClass(api);
        if (responseClass.isEmpty()) {
            replacementMap.put("returnClass", "Void");
            replacementMap.put("responseClass", "Void.class");
        } else {
            replacementMap.put("returnClass", responseClass);
            replacementMap.put("responseClass", responseClass+".class");
        }

        Template template = cfg.getTemplate("service");
        template.process(replacementMap, out);
        out.write("\n");
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

    private boolean isOpenApiDefinitionExists(Swagger swagger, Procedure p) {
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

    private static Map<HttpMethod, Operation> getOperationMap(Path path) {
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
