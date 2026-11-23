package cl.duocuc.usuarios_ms.client;

import cl.duocuc.usuarios_ms.dto.RolDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RolClientTest {

    private final AtomicInteger llamadas = new AtomicInteger();

    private RolClient cliente(ExchangeFunction respuesta) {
        ExchangeFunction contando = request -> {
            llamadas.incrementAndGet();
            return respuesta.exchange(request);
        };
        return new RolClient(WebClient.builder().exchangeFunction(contando), "http://roles-ms");
    }

    private static Mono<ClientResponse> json(HttpStatus estado, String body) {
        return Mono.just(ClientResponse.create(estado)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }

    @Test
    void listarConsultaUnaVezYLuegoUsaCache() {
        RolClient client = cliente(request -> {
            assertThat(request.url().toString()).isEqualTo("http://roles-ms/api/roles");
            return json(HttpStatus.OK, "[{\"id\":1,\"nombre\":\"Administrador\"},{\"id\":2,\"nombre\":\"Observador\"}]");
        });

        assertThat(client.listar()).hasSize(2);
        assertThat(client.listar()).hasSize(2);
        assertThat(client.obtener(1L)).isEqualTo(new RolDto(1L, "Administrador"));
        assertThat(llamadas.get()).isEqualTo(1);
    }

    @Test
    void obtenerConsultaYGuardaEnCache() {
        RolClient client = cliente(request -> {
            assertThat(request.url().toString()).isEqualTo("http://roles-ms/api/roles/2");
            return json(HttpStatus.OK, "{\"id\":2,\"nombre\":\"Observador\"}");
        });

        assertThat(client.obtener(2L)).isEqualTo(new RolDto(2L, "Observador"));
        assertThat(client.obtener(2L)).isEqualTo(new RolDto(2L, "Observador"));
        assertThat(llamadas.get()).isEqualTo(1);
    }

    @Test
    void obtenerRolSinCuerpoDevuelveNull() {
        RolClient client = cliente(request -> Mono.just(ClientResponse.create(HttpStatus.OK).build()));
        assertThat(client.obtener(2L)).isNull();
    }

    @Test
    void obtenerRolInexistenteDevuelveNull() {
        RolClient client = cliente(request -> json(HttpStatus.NOT_FOUND, "{}"));
        assertThat(client.obtener(9L)).isNull();
    }

    @Test
    void servicioCaidoLanza503() {
        RolClient client = cliente(request -> Mono.error(new IllegalStateException("caído")));

        assertThatThrownBy(client::listar)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> client.obtener(1L))
                .isInstanceOf(ResponseStatusException.class);
    }
}
