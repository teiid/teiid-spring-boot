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

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Autowired
    private CustomerRepository customerRepository;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args).close();
    }

    @Override
    public void run(String... args) throws Exception {

        System.out.println("\n\nFrom JDBC Template");
        customerRepository.findAll().forEach(x -> System.out.println(x));

        Customer c = new Customer();
        c.setName("John Doe");
        c.setSsn("111-11-1111");

        Address a = new Address();
        a.setStreet("230 Market St.");
        a.setZip("12345");
        c.setAddress(Arrays.asList(a));

        customerRepository.save(c);

        System.out.println("\n\nFrom JDBC Template");
        customerRepository.findAll().forEach(x -> System.out.println(x));

    }
}
