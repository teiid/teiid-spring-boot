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
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.teiid.core.TeiidProcessingException;
import org.teiid.core.util.ObjectConverterUtil;
import org.teiid.json.simple.JSONParser;
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
                    && pathInfo.contains("/static")) { //$NON-NLS-1$
                int idx = pathInfo.indexOf("/static");
                pathInfo = pathInfo.substring(idx+7);
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
        String format = request.getParameter("$format"); //$NON-NLS-1$
        if (format == null) {
            //TODO: could also look at the accepts header
            ContentType contentType = ContentType.parse(request.getContentType());
            if (contentType == null || contentType.isCompatible(ContentType.APPLICATION_JSON)) {
                format = "json"; //$NON-NLS-1$
            } else {
                format = "xml"; //$NON-NLS-1$
            }
        }
        PrintWriter writer = httpResponse.getWriter();
        String code = e.getCode()==null?"":e.getCode(); //$NON-NLS-1$
        String message = e.getMessage()==null?"":e.getMessage(); //$NON-NLS-1$
        if (format.equalsIgnoreCase("json")) { //$NON-NLS-1$
            httpResponse.setHeader(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_JSON.toContentTypeString());
            writer.write("{ \"error\": { \"code\": \""); //$NON-NLS-1$
            JSONParser.escape(code, writer);
            writer.write("\", \"message\": \""); //$NON-NLS-1$
            JSONParser.escape(message, writer);
            writer.write("\" } }"); //$NON-NLS-1$
        } else {
            try {
                httpResponse.setHeader(HttpHeader.CONTENT_TYPE, ContentType.APPLICATION_XML.toContentTypeString());
                XMLStreamWriter xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(writer);
                xmlStreamWriter.writeStartElement("m", "error", "http://docs.oasis-open.org/odata/ns/metadata"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                xmlStreamWriter.writeNamespace("m", "http://docs.oasis-open.org/odata/ns/metadata"); //$NON-NLS-1$ //$NON-NLS-2$
                xmlStreamWriter.writeStartElement("m", "code", "http://docs.oasis-open.org/odata/ns/metadata"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                xmlStreamWriter.writeCharacters(code);
                xmlStreamWriter.writeEndElement();
                xmlStreamWriter.writeStartElement("m", "message", "http://docs.oasis-open.org/odata/ns/metadata"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                xmlStreamWriter.writeCharacters(message);
                xmlStreamWriter.writeEndElement();
                xmlStreamWriter.writeEndElement();
                xmlStreamWriter.flush();
            } catch (XMLStreamException x) {
                throw new IOException(x);
            }
        }
        writer.close();
    }
}
