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
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.teiid.core.types.DataTypeManager;
import org.teiid.metadata.Procedure;
import org.teiid.metadata.ProcedureParameter;
import org.teiid.metadata.Schema;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

class CustomApiGenerator {
    private Log log;

    public CustomApiGenerator(Log log) {
        this.log = log;
    }

    void generate(Configuration cfg, File javaSrcDir, HashMap<String, String> map, Schema schema)
            throws Exception {
        Template template = cfg.getTemplate("Controller.java");
        File outFile = new File(javaSrcDir, map.get("modelName") + ".java");
        FileWriter out = new FileWriter(outFile);
        template.process(map, out);

        String servicePattern =
                "    @RequestMapping(value = \"${uri}\", method = RequestMethod.${method}, produces = {\"${contentType}\"} <#if consumes??>, consumes = \"${consumes}\" </#if>)\n" +
                        "    @ResponseBody\n" +
                        "    @ApiOperation(value=\"${description}\", nickname=\"${procedureName}\"<#if responseClass??>, response=${responseClass}</#if>)\n" +
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

        boolean found = false;
        Collection<Procedure> procedures = schema.getProcedures().values();
        for (Procedure procedure : procedures) {
            String uri = procedure.getProperty(REST_NAMESPACE + "URI", false);
            String method = procedure.getProperty(REST_NAMESPACE + "METHOD", false);
            if ((uri != null && method != null)) {
                buildCustomRestService(procedure, map, cfg, out);
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

    private void buildCustomRestService(Procedure procedure, HashMap<String, String> replacementMap,
            Configuration cfg, FileWriter out) throws Exception {

        String uri = procedure.getProperty(REST_NAMESPACE + "URI", false);
        String method = procedure.getProperty(REST_NAMESPACE + "METHOD", false);

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

        HashSet<String> pathParms = ApiGenerator.getPathParameters(uri);
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

        Template template = cfg.getTemplate("service");
        template.process(replacementMap, out);
        out.write("\n");
    }

}

