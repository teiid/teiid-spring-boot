package org.teiid.spring.example;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import javax.sql.DataSource;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {

    @Autowired
    ApplicationContext context;
    
    Random r = new Random(System.currentTimeMillis());
    
    @RequestMapping("/test")
    @Transactional
    public String index() {
        
        JdbcTemplate template = new JdbcTemplate((DataSource)context.getBean("dataSource"));
        int id = r.nextInt();
        
        template.update("INSERT INTO customer(id, ssn, name) VALUES (?, ?,?)", new Object[] {id, "1234", "ramesh"});
        
        template.query("select id, name from customer where id = " + id, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                //System.out.println(rs.getInt(1) + ":" + rs.getString(2));
            }
        });

        template.update("UPDATE CUSTOMER SET name = ? WHERE id = ?", new Object[] {"foo", id});
        
        return "Greetings from Spring Boot!";
    }
}
