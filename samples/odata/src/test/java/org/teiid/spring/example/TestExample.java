package org.teiid.spring.example;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { Application.class, TestConfiguration.class, RestTemplate.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
public class TestExample {
	@Autowired
	TestRestTemplate web;

    @LocalServerPort
    private int port;

    @Test
    public void test() throws Exception{
        ResponseEntity<String> response
    	  = web.getForEntity("http://localhost:"+port+"/CUSTOMER", String.class);
    	assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

    }

}
