package com.tao.card_nav;


import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication()
@MapperScan("com.tao.card_nav.mapper")
public class CardNavApplication {

    public static void main(String[] args) {
        Dotenv.configure().ignoreIfMissing().systemProperties().load();
        SpringApplication.run(CardNavApplication.class, args);
    }
}