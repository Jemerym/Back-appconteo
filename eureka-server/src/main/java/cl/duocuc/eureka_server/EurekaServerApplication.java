package cl.duocuc.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

// Registro de servicios: cada microservicio se anota aquí al arrancar con su nombre
// (roles-ms, usuarios-ms, turnos-ms, registros-ms, api-gateway).
// Panel web: http://localhost:8761
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EurekaServerApplication.class, args);
	}

}
