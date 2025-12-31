package com.hb.cda.elec_business.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // L'annotation @EnableAsync suffit pour activer le mode asynchrone
    // Les méthodes annotées avec @Async s'exécuteront dans un thread séparé
}
