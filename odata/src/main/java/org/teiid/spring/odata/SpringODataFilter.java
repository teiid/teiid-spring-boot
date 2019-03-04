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

import java.lang.ref.SoftReference;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.server.api.ODataHandler;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.VDB;
import org.teiid.metadata.Schema;
import org.teiid.net.TeiidURL;
import org.teiid.odata.api.Client;
import org.teiid.olingo.service.OlingoBridge;
import org.teiid.olingo.service.OlingoBridge.HandlerInfo;
import org.teiid.olingo.web.OpenApiHandler;
import org.teiid.spring.autoconfigure.TeiidServer;
import org.teiid.spring.identity.SpringSecurityHelper;
import org.teiid.spring.identity.TeiidSecurityContext;
import org.teiid.vdb.runtime.VDBKey;

public class SpringODataFilter implements HandlerInterceptor {
    static final String CONTEXT_PATH = "__CONTEXT_PATH__";
    private TeiidServer server;
    private VDB vdb;
    protected OpenApiHandler openApiHandler;
    protected SoftReference<OlingoBridge> clientReference = null;
    protected Properties connectionProperties;
    private SpringSecurityHelper securityHelper;

    public SpringODataFilter(Properties props, TeiidServer server, VDB vdb, ServletContext servletContext,
            SpringSecurityHelper securityHelper) {
        this.connectionProperties = props;
        this.server = server;
        this.vdb = vdb;
        try {
            this.openApiHandler = new OpenApiHandler(servletContext);
        } catch (ServletException e) {
            throw new IllegalStateException(e);
        }
        this.securityHelper = securityHelper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object contentHandler) throws Exception {

        HttpServletRequest httpRequest = request;
        HttpServletResponse httpResponse = response;

        String uri = request.getRequestURI().toString();
        String fullURL = request.getRequestURL().toString();

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
            return false;
        }

        String baseURI = fullURL;
        if (subContext != null) {
            baseURI = fullURL.substring(0, fullURL.indexOf(subContext));
            contextPath = contextPath.isEmpty()?subContext:(contextPath+"/"+subContext);
        }

        OlingoBridge context = null;
        if (this.clientReference != null) {
            context = this.clientReference.get();
        }

        if (context == null) {
            context = new OlingoBridge();
            this.clientReference = new SoftReference<OlingoBridge>(context);
        }

        VDBKey key = new VDBKey(vdbName, vdbVersion);
        TeiidSecurityContext tsc = securityHelper.getSecurityContext();
        if (tsc != null && tsc.getSubject() != null) {
            this.connectionProperties.setProperty(TeiidURL.CONNECTION.USER_NAME, tsc.getUserName());
        }
        Client client = buildClient(vdbName, vdbVersion, this.connectionProperties);
        client.open();

        HandlerInfo handlerInfo = context.getHandlers(baseURI, client, modelName);
        ODataHandler handler = handlerInfo.oDataHttpHandler;

        if (openApiHandler.processOpenApiMetadata(httpRequest, key, uri, modelName,
                response, handlerInfo.serviceMetadata, null)) {
            return false;
        }

        httpRequest.setAttribute(ODataHttpHandler.class.getName(), handler);
        httpRequest.setAttribute(Client.class.getName(), client);
        httpRequest.setAttribute(CONTEXT_PATH, contextPath);
        return true;
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

            if (path == null && !m.isSource()) {
                modelName = m.getName();
                break;
            } else if (path != null && path.equalsIgnoreCase(m.getName())) {
                modelName = m.getName();
                break;
            }
        }

        if (path == null && modelName == null) {
            modelName = firstModel;
        }
        return modelName;
    }

    public Client buildClient(String vdbName, String version, Properties props) {
        return new SpringClient(vdbName, version, props, server);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            @Nullable ModelAndView modelAndView) throws Exception {
        Client client = (Client) request.getAttribute(Client.class.getName());
        client.close();
    }
}

