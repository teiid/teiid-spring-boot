package org.teiid.spring.example;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessService {

    @Autowired
    CustomerRepository repository;
    
    @Transactional
    public void updateCustomer(Customer c) {
        repository.save(c);
    }
}
