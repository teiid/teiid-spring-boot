package org.teiid.spring.example;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExampleApplication implements CommandLineRunner {

    @Autowired
    private CustomerRepository customerRepository;    
    
	public static void main(String[] args) {
		SpringApplication.run(ExampleApplication.class, args);
	}
	
    @Override
    public void run(String... args) throws Exception {

        List<Customer> list = customerRepository.findAll();
        list.forEach(x -> System.out.println(x));        
        
    }
}
