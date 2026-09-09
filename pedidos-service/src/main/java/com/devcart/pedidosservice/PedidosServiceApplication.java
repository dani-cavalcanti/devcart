package com.devcart.pedidosservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class PedidosServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PedidosServiceApplication.class, args);
    }

}
