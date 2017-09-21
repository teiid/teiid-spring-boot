package org.teiid.spring.example;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {Application.class, TestConfiguration.class, StockRepository.class})
public class TestExample {

    @Autowired
    StockRepository stocksRepository;
    
    @Test
    public void test() {
        Stock s = new Stock();
        s.setId(1002);
        s.setSymbol("BA");
        s.setPrice(84.97);
        s.setCompanyName("The Boeing Company");
        assertEquals(s, stocksRepository.findAll().iterator().next());
    }

}
