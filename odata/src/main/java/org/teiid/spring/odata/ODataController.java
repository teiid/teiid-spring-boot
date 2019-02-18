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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.teiid.olingo.web.ODataServlet;

@Controller
@RequestMapping(value = "/")
public class ODataController {
    private ODataServlet servlet = new ODataServlet();

    @RequestMapping(value = "**")
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException{
        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
            @Override
            public String getServletPath() {
                return "";
            }
            @Override
            public String getContextPath() {
                return (String)request.getAttribute(SpringODataFilter.CONTEXT_PATH);
            }
        };
        servlet.service(wrapper, response);
    }
}
