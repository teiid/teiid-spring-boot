/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.teiid.spring.data.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import javax.activation.DataSource;
import javax.resource.ResourceException;
import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Binding;
import javax.xml.ws.Dispatch;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Response;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.WebServiceException;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.teiid.resource.spi.BasicConnection;
import org.teiid.translator.WSConnection;

public class RestConnection extends BasicConnection implements WSConnection {

	private static final class HttpDataSource implements DataSource {
		private final URL url;
		private InputStream content;
		private String contentType;

		private HttpDataSource(URL url, InputStream entity, String contentType) {
			this.url = url;
			this.content = entity;
			this.contentType = contentType;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getName() {
			return this.url.getPath();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return this.content;
		}

		@Override
		public String getContentType() {
			return this.contentType;
		}
	}

	private static final class HttpDispatch implements Dispatch<DataSource> {
        private HashMap<String, Object> requestContext = new HashMap<String, Object>();
		private HashMap<String, Object> responseContext = new HashMap<String, Object>();
		private String endpoint;
		private RestTemplate template;
		
		public HttpDispatch(String endpoint, RestTemplate template, String binding) {
		    this.endpoint = endpoint;
		    this.template = template;
		}

		@Override
		public DataSource invoke(DataSource msg) {
			try {
				final URL url = new URL(this.endpoint);
				url.toURI(); //ensure this is a valid uri
				
				//TODO: need to set headers and payload and security pass through
				
				InputStream payload = null;
				if (msg != null) {
					payload = msg.getInputStream();
				}

				//TODO: this materializes the response to memory, there needs to be better memory sensitive 
				//way to do this.
                ResponseExtractor<BufferingClientHttpResponseWrapper> contentExtractor = 
                    new ResponseExtractor<BufferingClientHttpResponseWrapper>() {
                    @Override
                    public BufferingClientHttpResponseWrapper extractData(ClientHttpResponse response)
                            throws IOException {
                        BufferingClientHttpResponseWrapper buf=  new BufferingClientHttpResponseWrapper(response);
                        buf.getBody(); // input stream is closed once this method exits.
                        return buf;
                    }
                };
                
                BufferingClientHttpResponseWrapper response = template.execute(url.toURI(), HttpMethod.GET, null,
                        contentExtractor);
				
				String contentType = response.getHeaders().getContentType().toString();
				getResponseContext().put(WSConnection.STATUS_CODE, response.getStatusCode().value());
				return new HttpDataSource(url, response.getBody(), contentType);
			} catch (IOException e) {
				throw new WebServiceException(e);
			} catch (URISyntaxException e) {
				throw new WebServiceException(e);
			}
		}

		@Override
		public Map<String, Object> getRequestContext() {
			return this.requestContext;
		}

		@Override
		public Map<String, Object> getResponseContext() {
			return this.responseContext;
		}

		@Override
		public Binding getBinding() {
			throw new UnsupportedOperationException();
		}

		@Override
		public EndpointReference getEndpointReference() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Response<DataSource> invokeAsync(DataSource msg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Future<?> invokeAsync(DataSource msg,AsyncHandler<DataSource> handler) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void invokeOneWay(DataSource msg) {
			throw new UnsupportedOperationException();
		}
	}

	public <T> Dispatch<T> createDispatch(Class<T> type, Mode mode) throws IOException {
	    throw new IOException("SOAP calling not supported yet.");
	}

	@SuppressWarnings("unchecked")
	public <T> Dispatch<T> createDispatch(String binding, String endpoint, Class<T> type, Mode mode) {
		Dispatch<T> dispatch = (Dispatch<T>) new HttpDispatch(endpoint, this.template, binding);
		return dispatch;
	}

	private RestTemplate template;
	
	public RestConnection(RestTemplate template) {
	    this.template = template;
	}
	
    @Override
	public void close() throws ResourceException {
	}

	@Override
	public URL getWsdl() {
	    return null;
	}

	@Override
	public QName getServiceQName() {
	    throw new UnsupportedOperationException();
	}

	@Override
	public QName getPortQName() {
	    throw new UnsupportedOperationException();
	}
	
	@Override
	public String getStatusMessage(int status) {
		return null;
	}
}
