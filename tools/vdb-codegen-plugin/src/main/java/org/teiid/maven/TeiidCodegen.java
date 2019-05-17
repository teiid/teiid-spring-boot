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

        //  Only added @ResponseBody in this, needs update in future perhaps
        apiTemplateFiles.put("teiidApi.mustache", "Api.java");
        apiTemplateFiles.put("teiidDelegate.mustache", "Delegate.java");

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
}
