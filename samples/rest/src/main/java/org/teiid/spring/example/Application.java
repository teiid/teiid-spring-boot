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

package org.teiid.spring.example;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.teiid.translator.ExecutionContext;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Autowired
    private QuoteRepository quoteRepository;

    @Autowired
    RestTemplate restTemplate;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args).close();
    }

    @Override
    public void run(String... args) throws Exception {
        quoteRepository.findAll().forEach(c -> System.out.println("***" + c));
    }

    @Bean(name = "webCallBean")
    public Function<ExecutionContext, String> webCallBean() {
        return (c) -> {
            String url = "http://gturnquist-quoters.cfapps.io/api/random";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getBody();
        };
    }
}
