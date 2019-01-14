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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.activation.DataSource;
import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Binding;
import javax.xml.ws.Dispatch;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Response;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.teiid.core.types.InputStreamFactory;
import org.teiid.core.util.ObjectConverterUtil;
import org.teiid.spring.data.BaseConnection;
import org.teiid.translator.ws.WSConnection;

public class RestConnection extends BaseConnection implements WSConnection {

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
    private BeanFactory beanFactory;

    public HttpDispatch(String endpoint, RestTemplate template, BeanFactory beanFactory, String binding) {
        this.endpoint = endpoint;
        this.template = template;
        this.beanFactory = beanFactory;

            Map<String, List<String>> httpHeaders = new HashMap<String, List<String>>();
            httpHeaders.put("Content-Type", Collections.singletonList("text/xml; charset=utf-8"));//$NON-NLS-1$ //$NON-NLS-2$
            httpHeaders.put("User-Agent", Collections.singletonList("Teiid Server"));//$NON-NLS-1$ //$NON-NLS-2$
            getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, httpHeaders);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataSource invoke(DataSource msg) {
      try {
        final URL url = new URL(this.endpoint);
        url.toURI(); //ensure this is a valid uri

        String method = (String)this.requestContext.get(MessageContext.HTTP_REQUEST_METHOD);
        HttpMethod httpMethod = HttpMethod.resolve(method);

        HttpHeaders headers = new HttpHeaders();
        Map<String, List<String>> header = (Map<String, List<String>>) this.requestContext
            .get(MessageContext.HTTP_REQUEST_HEADERS);
        if (header != null) {
          for (Map.Entry<String, List<String>> entry : header.entrySet()) {
            if (entry.getKey().equals("T-Spring-Bean")) {
              headers = (HttpHeaders)beanFactory.getBean(entry.getValue().get(0));
            } else {
              headers.add(entry.getKey(), entry.getValue().get(0));
            }
          }
        }

        // payload
        InputStream payload = null;
        if (msg != null) {
          payload = msg.getInputStream();
          String bean = ObjectConverterUtil.convertToString(payload);
          InputStreamFactory isf = (InputStreamFactory)this.beanFactory.getBean(bean);
          payload = isf.getInputStream();
        }

                HttpEntity<InputStream> entity = new HttpEntity<InputStream>(payload, headers);
                ResponseEntity<byte[]> response = template.exchange(url.toURI(), httpMethod, entity,
                        byte[].class);

        String contentType = response.getHeaders().getContentType().toString();
        getResponseContext().put(WSConnection.STATUS_CODE, response.getStatusCode().value());
        return new HttpDataSource(url, new ByteArrayInputStream(response.getBody()), contentType);
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
    Dispatch<T> dispatch = (Dispatch<T>) new HttpDispatch(endpoint, this.template, this.beanFactory, binding);
    return dispatch;
  }

  private RestTemplate template;
  private BeanFactory beanFactory;

  public RestConnection(RestTemplate template, BeanFactory beanFactory) {
      this.template = template;
      this.beanFactory = beanFactory;
  }

    @Override
  public void close() throws Exception {
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
