package com.shangdao.phoenix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
	
	protected final static Logger logger = LoggerFactory.getLogger(Application.class); 
	

    public static void main(String[] args) {
    	
        SpringApplication.run(Application.class, args);
    }
    
  

}