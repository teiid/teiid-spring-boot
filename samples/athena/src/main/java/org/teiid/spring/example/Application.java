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

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Autowired
    JdbcTemplate template;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args).close();
    }

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        template.query("SELECT request_timestamp, elb_name FROM elb_logs limit 10",
                new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                System.out.print(rs.getObject(1));
                System.out.print("'");
                System.out.println(rs.getObject(2));
            }
        });

    }
}
