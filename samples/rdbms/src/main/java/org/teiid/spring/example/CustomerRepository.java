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

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Customer> findAll() {

        List<Customer> result = jdbcTemplate.query(
                "SELECT id, name, ssn FROM accountsDS.Customers UNION ALL SELECT id, name, ssn FROM customerDS.Customer",
                (rs, rowNum) -> new Customer(rs.getInt("id"),
                        rs.getString("name"), rs.getString("ssn")));
     
        return result;

    }
    
    public List<Customer> findViewBasedCustomers() {

        List<Customer> result = jdbcTemplate.query(
                "SELECT id, name, ssn FROM all_customers",
                (rs, rowNum) -> new Customer(rs.getInt("id"),
                        rs.getString("name"), rs.getString("ssn")));
     
        return result;

    }    
    
    
    @Transactional 
    public int insert(long id, String name, String ssn) {
       return jdbcTemplate.update("INSERT INTO all_customers(id, name, ssn) VALUES(?,?, ?)", id, name, ssn);
    }    
}   