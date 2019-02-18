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
package org.teiid.spring.odata;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.teiid.core.TeiidProcessingException;
import org.teiid.core.util.ObjectConverterUtil;
import org.teiid.olingo.ODataPlugin;

@RestController
@RequestMapping("/static")
public class StaticContentController {

    @RequestMapping(value = "**")
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getRequestURI();

        try {
            if (pathInfo.endsWith(".xml") //$NON-NLS-1$
                    && !pathInfo.endsWith("pom.xml") //$NON-NLS-1$
                    && !pathInfo.contains("META-INF") //$NON-NLS-1$
                    && !pathInfo.contains("WEB-INF") //$NON-NLS-1$
                    && !pathInfo.substring(1).contains("/static")) { //$NON-NLS-1$
                pathInfo = pathInfo.substring(7);
                InputStream contents = getClass().getResourceAsStream(pathInfo);
                if (contents != null) {
                    writeContent(response, contents);
                    response.flushBuffer();
                    return;
                }
            }
            throw new TeiidProcessingException(ODataPlugin.Util.gs(ODataPlugin.Event.TEIID16055, pathInfo));
        } catch (TeiidProcessingException e) {
            writeError(request, e, response, 404);
        }
    }

    private void writeContent(HttpServletResponse response, InputStream contents) throws IOException {
        ObjectConverterUtil.write(response.getOutputStream(), contents, -1);
    }

    static void writeError(ServletRequest request, TeiidProcessingException e,
            HttpServletResponse httpResponse, int statusCode) throws IOException {
        httpResponse.setStatus(statusCode);
        ContentType contentType = ContentType.parse(request.getContentType());
        PrintWriter writer = httpResponse.getWriter();
        String code = e.getCode()==null?"":e.getCode(); //$NON-NLS-1$
        String message = e.getMessage()==null?"":e.getMessage(); //$NON-NLS-1$
        if (contentType == null || contentType.isCompatible(ContentType.APPLICATION_JSON)) {
            httpResponse.setHeader(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_JSON.toContentTypeString());
            writer.write("{ \"error\": { \"code\": \""+StringEscapeUtils.escapeJson(code)+"\", \"message\": \""+StringEscapeUtils.escapeJson(message)+"\" } }"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } else {
            httpResponse.setHeader(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_XML.toContentTypeString());
            writer.write("<m:error xmlns:m=\"http://docs.oasis-open.org/odata/ns/metadata\"><m:code>"+StringEscapeUtils.escapeXml10(code)+"</m:code><m:message>"+StringEscapeUtils.escapeXml10(message)+"</m:message></m:error>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        writer.close();
    }
}
