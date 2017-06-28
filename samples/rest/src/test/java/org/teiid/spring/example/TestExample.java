package org.teiid.spring.example;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {Application.class, TestConfiguration.class, QuoteRepository.class})
public class TestExample {

    @Autowired
    QuoteRepository quoteRepository;
    
    @Test
    public void test() {
        assertNotNull(quoteRepository.findAll().iterator().next());
        assertTrue(quoteRepository.findAll().iterator().next().getId() > 0);
    }
}
