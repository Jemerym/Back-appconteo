package cl.duocuc.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Puerta de entrada única: la app llama solo a http://IP:8080/ y el gateway reenvía
// cada ruta al microservicio que corresponde (ver application.properties).
@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}
