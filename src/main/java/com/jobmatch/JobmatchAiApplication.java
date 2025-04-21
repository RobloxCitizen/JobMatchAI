package com.jobmatch;

import com.jobmatch.ui.MainUI;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:secrets.properties")
public class JobmatchAiApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(JobmatchAiApplication.class, args);
        //Application.launch(MainUI.class, args);
    }
}