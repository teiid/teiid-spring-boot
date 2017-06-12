package org.teiid.spring.example;

import java.util.List;

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
}   