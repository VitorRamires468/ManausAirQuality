package com.projeto.poluicao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PoluicaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PoluicaoApplication.class, args);
    }

}
