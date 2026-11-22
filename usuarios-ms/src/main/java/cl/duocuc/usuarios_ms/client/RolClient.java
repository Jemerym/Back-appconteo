package cl.duocuc.usuarios_ms.client;

import cl.duocuc.usuarios_ms.dto.RolDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RolClient {

    private static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(5);

    private final WebClient webClient;

    // CACHÉ: los roles casi nunca cambian (son 2). Una vez leídos se guardan en memoria,
    // así el login y los listados no consultan a roles-ms en cada petición
    // y siguen funcionando aunque roles-ms se caiga después.
    private final Map<Long, RolDto> cache = new ConcurrentHashMap<>();
    private volatile boolean listaCompleta = false;

    // El builder viene de WebClientConfig (@LoadBalanced): así "http://roles-ms" se resuelve con Eureka.
    public RolClient(WebClient.Builder builder, @Value("${roles.service.url}") String rolesUrl) {
        this.webClient = builder.baseUrl(rolesUrl).build();
    }

    public List<RolDto> listar() {
        if (!listaCompleta) {
            List<RolDto> roles = pedir(() -> webClient.get()
                    .uri("/api/roles")
                    .retrieve()
                    .bodyToFlux(RolDto.class)
                    .collectList()
                    .block(TIEMPO_MAXIMO));
            if (roles != null) {
                roles.forEach(r -> cache.put(r.id(), r));
                listaCompleta = true;
            }
        }
        return List.copyOf(cache.values());
    }

    /** Devuelve null si el rol no existe. */
    public RolDto obtener(Long id) {
        RolDto enCache = cache.get(id);
        if (enCache != null) {
            return enCache;
        }
        try {
            RolDto rol = pedir(() -> webClient.get()
                    .uri("/api/roles/{id}", id)
                    .retrieve()
                    .bodyToMono(RolDto.class)
                    .block(TIEMPO_MAXIMO));
            if (rol != null) {
                cache.put(rol.id(), rol);
            }
            return rol;
        } catch (RolNoEncontrado e) {
            return null;
        }
    }

    // Traduce cualquier falla de roles-ms (apagado, lento, error 5xx) a un 503 con mensaje claro.
    private <T> T pedir(java.util.function.Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (WebClientResponseException.NotFound e) {
            throw new RolNoEncontrado();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de roles no está disponible");
        }
    }

    private static class RolNoEncontrado extends RuntimeException {
    }
}
