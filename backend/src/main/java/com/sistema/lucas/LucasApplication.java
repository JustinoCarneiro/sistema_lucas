package com.sistema.lucas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport; // <-- 1. Importe a configuração

@SpringBootApplication
// 👇 2. ADICIONE ESTA LINHA PARA PADRONIZAR O JSON DE PAGINAÇÃO 👇
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class LucasApplication {

    public static void main(String[] args) {
        SpringApplication.run(LucasApplication.class, args);
    }

}