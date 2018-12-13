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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.server.api.ODataHttpHandler;
import org.teiid.adminapi.AdminException;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.VDB;
import org.teiid.core.TeiidProcessingException;
import org.teiid.metadata.Schema;
import org.teiid.odata.api.Client;
import org.teiid.olingo.service.OlingoBridge;
import org.teiid.olingo.web.ContextAwareHttpSerlvetRequest;
import org.teiid.olingo.web.ODataFilter;
import org.teiid.spring.autoconfigure.TeiidServer;
import org.teiid.vdb.runtime.VDBKey;

public class SpringODataFilter extends ODataFilter {

	private TeiidServer server;
	private VDB vdb;

	public SpringODataFilter(TeiidServer server, VDB vdb) {
		this.server = server;
		this.vdb = vdb;
	}

	@Override
    public void internalDoFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException, TeiidProcessingException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = ((HttpServletRequest) request).getRequestURI().toString();
        String fullURL = ((HttpServletRequest) request).getRequestURL().toString();
        if (uri.contains("/static/") || uri.contains("/keycloak/")){ //$NON-NLS-1$ //$NON-NLS-2$
            chain.doFilter(httpRequest, response);
            return;
        }

        // possible vdb and model names
        String contextPath = httpRequest.getContextPath();
        String subContext = null;
        if (contextPath.isEmpty()) {
            // if model name is defined in the URL
            if (uri.startsWith("/") && uri.indexOf('/', 1) != -1) {
                int idx = uri.indexOf('/', 1);
                contextPath = uri.substring(1, idx);
                if (uri.indexOf('/', idx+1) != -1) {
                    subContext = uri.substring(idx+1, uri.indexOf('/', idx+1));
                }
            }
        }

        // figure out vdbname and model name from paths are assume defaults
        VDB requestVDB = this.vdb;
        String vdbName = this.vdb.getName();
        String vdbVersion = this.vdb.getVersion();
        String modelName = null;
        if (contextPath != null && subContext != null) {
            int idx = contextPath.indexOf('.');
            if (idx != -1) {
                vdbName = contextPath.substring(0, idx);
                vdbVersion = contextPath.substring(idx+1);
            } else {
                vdbName = contextPath;
                vdbVersion = "1.0.0";
            }
            if (!requestVDB.getName().equals(vdbName) || !requestVDB.getVersion().equals(vdbVersion)) {
                try {
                    requestVDB = this.server.getAdmin().getVDB(vdbName, vdbVersion);
                } catch (AdminException e) {
                    httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Wrong VDB Name or version defined in the URL");
                    return;
                }
            }
            modelName = subContext;
        } else {
            modelName = contextPath;
        }

        modelName = modelName(modelName, requestVDB);
        if (modelName == null || modelName.isEmpty()) {
            httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Wrong Model/Schema name defined in the URL");
            return;
        }

        String baseURI = fullURL.substring(0, fullURL.indexOf(contextPath));
        ContextAwareHttpSerlvetRequest contextAwareRequest = new ContextAwareHttpSerlvetRequest(httpRequest);
        contextAwareRequest.setContextPath(subContext == null ? contextPath : contextPath+"/"+subContext);
        httpRequest = contextAwareRequest;

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
            ODataHttpHandler handler = context.getHandler(baseURI, client, modelName);
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

	public String modelName(String contextPath, VDB vdb) {
		Schema schema = server.getSchema("teiid");
        if (contextPath.isEmpty() && schema != null && !schema.getTables().isEmpty()) {
			return "teiid";
		}

		String modelName = null;
		for (Model m : vdb.getModels()) {
			if (m.getName().equals("file") || m.getName().equals("rest") || m.getName().equals("teiid")) {
				continue;
			}
			if (contextPath.isEmpty()) {
			    modelName = m.getName();
			    break;
			} else if (contextPath.equalsIgnoreCase(m.getName())) {
                modelName = m.getName();
                break;
			}
		}
		return modelName;
	}

    @Override
    public Client buildClient(String vdbName, String version, Properties props) {
        return new SpringClient(vdbName, version, props, server);
    }
}
