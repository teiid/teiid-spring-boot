package org.teiid.spring.example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {Application.class, TestConfiguration.class, EmployeeRepository.class})
public class TestExample {

    @Autowired
    EmployeeRepository employeeRepository;
    
    @Test
    public void test() {
        Employee e = new Employee();
        e.setROW_ID(2);
        e.setFirstName("John");
        e.setLastName("Doe");
        e.setAge(23);
        assertEquals(e, employeeRepository.findAll().iterator().next());
    }
}
