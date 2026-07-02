package com.tao.card_nav;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication()
@MapperScan("com.tao.card_nav.mapper")
public class CardNavApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardNavApplication.class, args);
    }
}