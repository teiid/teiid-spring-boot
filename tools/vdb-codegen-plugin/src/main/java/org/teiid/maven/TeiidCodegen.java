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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.SpringCodegen;

import io.swagger.v3.oas.models.Operation;

public class TeiidCodegen extends SpringCodegen {

    @Override
    public void processOpts() {
        super.processOpts();
        apiTemplateFiles.remove("api.mustache");
        apiTemplateFiles.remove("apiDelegate.mustache");
        //apiTemplateFiles.remove("apiController.mustache");


        //  Only added @ResponseBody in this, needs update in future perhaps
        apiTemplateFiles.put("teiidApi.mustache", ".java");
        apiTemplateFiles.put("teiidDelegate.mustache", "Delegate.java");
        apiTemplateFiles.put("teiidApiController.mustache", "Controller.java");

        // remove the verbose files
        removeSupportingFile("OpenAPI2SpringBoot.java");
        removeSupportingFile("pom.xml");
        removeSupportingFile("README.md");

        //  this not working
        removeSupportingFile(".openapi-generator-ignore");
        removeSupportingFile(".openapi-generator");

    }

    private void removeSupportingFile(String name) {
        SupportingFile found = null;
        for (SupportingFile f : supportingFiles) {
            if(f.destinationFilename.contentEquals(name)) {
                found = f;
            }
        }
        if (found != null) {
            supportingFiles.remove(found);
        }
    }
    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co,
            Map<String, List<CodegenOperation>> operations) {
        super.addOperationToGroup(tag, resourcePath, operation, co, operations);

        System.out.println("tag = "+tag +" resourcePath = "+ resourcePath);
    }

    @Override
    public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> objs, List<Object> allModels) {
        super.postProcessOperationsWithModels(objs, allModels);
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        List<CodegenOperation> os = (List<CodegenOperation>) operations.get("operation");
        List<TeiidCodegenOperation> newOs = new ArrayList<>();
        for (CodegenOperation o : os) {
            TeiidCodegenOperation tco = new TeiidCodegenOperation(o);
            if (o.returnType == null || o.returnType.contentEquals("Void")) {
                tco.setHasReturn(false);
                o.returnType = "Void";
            }
            newOs.add(tco);
        }
        operations.put("operation", newOs);
        return objs;
    }

    class TeiidCodegenOperation extends CodegenOperation {
        public boolean hasReturn = true;

        public TeiidCodegenOperation(CodegenOperation o) {
            super();

            // Copy all fields of CodegenOperation
            this.responseHeaders.addAll(o.responseHeaders);
            this.hasAuthMethods = o.hasAuthMethods;
            this.hasConsumes = o.hasConsumes;
            this.hasProduces = o.hasProduces;
            this.hasParams = o.hasParams;
            this.hasOptionalParams = o.hasOptionalParams;
            this.returnTypeIsPrimitive = o.returnTypeIsPrimitive;
            this.returnSimpleType = o.returnSimpleType;
            this.subresourceOperation = o.subresourceOperation;
            this.isMapContainer = o.isMapContainer;
            this.isListContainer = o.isListContainer;
            this.isMultipart = o.isMultipart;
            this.hasMore = o.hasMore;
            this.isResponseBinary = o.isResponseBinary;
            this.hasReference = o.hasReference;
            this.isRestfulIndex = o.isRestfulIndex;
            this.isRestfulShow = o.isRestfulShow;
            this.isRestfulCreate = o.isRestfulCreate;
            this.isRestfulUpdate = o.isRestfulUpdate;
            this.isRestfulDestroy = o.isRestfulDestroy;
            this.isRestful = o.isRestful;
            this.path = o.path;
            this.operationId = o.operationId;
            this.returnType = o.returnType;
            this.httpMethod = o.httpMethod;
            this.returnBaseType = o.returnBaseType;
            this.returnContainer = o.returnContainer;
            this.summary = o.summary;
            this.unescapedNotes = o.unescapedNotes;
            this.notes = o.notes;
            this.baseName = o.baseName;
            this.defaultResponse = o.defaultResponse;
            this.discriminator = o.discriminator;
            this.consumes = o.consumes;
            this.produces = o.produces;
            this.bodyParam = o.bodyParam;
            this.allParams = o.allParams;
            this.bodyParams = o.bodyParams;
            this.pathParams = o.pathParams;
            this.queryParams = o.queryParams;
            this.headerParams = o.headerParams;
            this.formParams = o.formParams;
            this.requiredParams = o.requiredParams;
            this.optionalParams = o.optionalParams;
            this.authMethods = o.authMethods;
            this.tags = o.tags;
            this.responses = o.responses;
            this.imports = o.imports;
            this.examples = o.examples;
            this.externalDocs = o.externalDocs;
            this.vendorExtensions = o.vendorExtensions;
            this.nickname = o.nickname;
            this.operationIdLowerCase = o.operationIdLowerCase;
            this.operationIdCamelCase = o.operationIdCamelCase;
        }

        public boolean isHasReturn() {
            return hasReturn;
        }

        public void setHasReturn(boolean hasReturn) {
            this.hasReturn = hasReturn;
        }
    }
}
