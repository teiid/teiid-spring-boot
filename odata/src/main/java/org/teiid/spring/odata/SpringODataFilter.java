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
import java.lang.ref.SoftReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.server.api.ODataHandler;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.VDB;
import org.teiid.core.TeiidProcessingException;
import org.teiid.metadata.Schema;
import org.teiid.odata.api.Client;
import org.teiid.olingo.service.OlingoBridge;
import org.teiid.olingo.service.OlingoBridge.HandlerInfo;
import org.teiid.olingo.web.ContextAwareHttpSerlvetRequest;
import org.teiid.olingo.web.ODataFilter;
import org.teiid.olingo.web.OpenApiHandler;
import org.teiid.spring.autoconfigure.TeiidServer;
import org.teiid.vdb.runtime.VDBKey;

public class SpringODataFilter extends ODataFilter {

    private TeiidServer server;
    private VDB vdb;
    private String[] alternatePaths;
    protected OpenApiHandler openApiHandler;

    public SpringODataFilter(TeiidServer server, VDB vdb, String[] redirectedPaths, ServletContext servletContext) {
        this.server = server;
        this.vdb = vdb;
        this.alternatePaths = redirectedPaths;
        try {
            this.openApiHandler = new OpenApiHandler(servletContext);
        } catch (ServletException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean skipPath(String uri) {
        if (this.alternatePaths == null) {
            return false;
        }
        for (int i = 0; i < this.alternatePaths.length; i++) {
            String path = this.alternatePaths[i];
            if (uri.contains(path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void internalDoFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException, TeiidProcessingException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = ((HttpServletRequest) request).getRequestURI().toString();
        String fullURL = ((HttpServletRequest) request).getRequestURL().toString();
        if (uri.contains("/static/") || skipPath(uri)){ //$NON-NLS-1$
            chain.doFilter(httpRequest, response);
            return;
        }

        // possible vdb and model names
        String contextPath = httpRequest.getContextPath();
        if (!contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        String subContext = null;
        // if model name is defined in the URL
        if (uri.startsWith("/") && uri.indexOf('/', 1) != -1) {
            int idx = uri.indexOf('/', 1);
            subContext = uri.substring(1, idx);
        }

        // figure out vdbname and model name from paths are assume defaults
        VDB requestVDB = this.vdb;
        String vdbName = this.vdb.getName();
        String vdbVersion = this.vdb.getVersion();
        boolean implicitVdb = vdb.getPropertyValue("implicit") != null && vdb.getPropertyValue("implicit").equals("true");

        String modelName = modelName(subContext, requestVDB, implicitVdb);
        if (modelName == null || modelName.isEmpty()) {
            httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Wrong Model/Schema name defined in the URL");
            return;
        }

        String baseURI = fullURL;
        if (subContext != null) {
            baseURI = fullURL.substring(0, fullURL.indexOf(subContext));
            ContextAwareHttpSerlvetRequest contextAwareRequest = new ContextAwareHttpSerlvetRequest(httpRequest);
            contextAwareRequest.setContextPath(contextPath+"/"+subContext);
            httpRequest = contextAwareRequest;
        }

        VDBKey key = new VDBKey(vdbName, vdbVersion);
        SoftReference<OlingoBridge> ref = this.contextMap.get(key);
        OlingoBridge context = null;
        if (ref != null) {
            context = ref.get();
        }

        if (context == null) {
            context = new OlingoBridge();
            ref = new SoftReference<OlingoBridge>(context);
            this.contextMap.put(key, ref);
        }

        Client client = buildClient(key.getName(), key.getVersion(), this.initProperties);
        try {
            Connection connection = client.open();
            registerVDBListener(client, connection);
            HandlerInfo handlerInfo = context.getHandlers(baseURI, client, modelName);
            ODataHandler handler = handlerInfo.oDataHttpHandler;

            if (openApiHandler.processOpenApiMetadata(httpRequest, key, uri, modelName,
                    response, handlerInfo.serviceMetadata, null)) {
                return;
            }

            httpRequest.setAttribute(ODataHttpHandler.class.getName(), handler);
            httpRequest.setAttribute(Client.class.getName(), client);
            chain.doFilter(httpRequest, response);
        } catch(SQLException e) {
            throw new TeiidProcessingException(e);
        } finally {
            try {
                client.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    public String modelName(String path, VDB vdb, boolean implicitVdb) {
        if (path != null && path.isEmpty()) {
            path = null;
        }
        Schema schema = server.getSchema("teiid");
        if (path == null && schema != null && !schema.getTables().isEmpty() && implicitVdb) {
            return "teiid";
        }

        String modelName = null;
        String firstModel = null;
        for (Model m : vdb.getModels()) {

            // these models are implicit models, skip these
            if (m.getName().equals("file") || m.getName().equals("rest")) {
                continue;
            }

            if (firstModel == null) {
                firstModel = m.getName();
            }

            if (path == null) {
                modelName = m.getName();
                break;
            } else if (path.equalsIgnoreCase(m.getName())) {
                modelName = m.getName();
                break;
            }
        }

        if (modelName == null) {
            modelName = firstModel;
        }
        return modelName;
    }

    @Override
    public Client buildClient(String vdbName, String version, Properties props) {
        return new SpringClient(vdbName, version, props, server);
    }
}
