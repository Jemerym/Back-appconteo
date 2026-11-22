package cl.duocuc.turnos_ms.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // @LoadBalanced: permite usar el NOMBRE del microservicio en la URL (http://usuarios-ms)
    // y Eureka responde en qué IP y puerto está corriendo.
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
