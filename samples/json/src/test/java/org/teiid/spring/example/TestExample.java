package org.teiid.spring.example;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.*;

import java.math.BigDecimal;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ExampleApplication.class, TestConfiguration.class, EmployeeRepository.class})
public class TestExample {

    @Autowired
    EmployeeRepository employeeRepository;
    
    @Test
    public void test() {
        Employee e = new Employee();
        e.setId(123);
        e.setName("Henry Smith");
        e.setAge(28);
        e.setSalary(new BigDecimal("2000"));
        e.setDesignation("Programmer");
        e.setPhoneNumbers(new long[] {654321,222333});
        Address address = new Address();
        address.setStreet("Park Avn.");
        address.setCity("Westchester");
        address.setZipcode(10583);
        e.setAddress(address);
        assertEquals(e, employeeRepository.findAll().iterator().next());
    }
}
