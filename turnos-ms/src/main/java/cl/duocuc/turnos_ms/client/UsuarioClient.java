package cl.duocuc.turnos_ms.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Component
public class UsuarioClient {

    private final WebClient web;

    // Quitas WebClient.Builder de aquí y lo creas directamente adentro:
    public UsuarioClient(@Value("${usuarios.service.url}") String url) {
        this.web = WebClient.builder().baseUrl(url).build();
    }

    /** GET /api/usuarios/{id}. Falla con 400 si no existe y con 503 si usuarios-service no responde. */
    public UsuarioRemoto obtener(Long id) {
        try {
            return web.get()
                    .uri("/api/usuarios/{id}", id)
                    .retrieve()
                    .bodyToMono(UsuarioRemoto.class)
                    .block(Duration.ofSeconds(5));
        } catch (WebClientResponseException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El observador no existe");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo verificar al observador (usuarios-service no responde)");
        }
    }
}
