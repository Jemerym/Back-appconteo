package cl.duocuc.turnos_ms.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioClientTest {

    private static UsuarioClient cliente(ExchangeFunction respuesta) {
        return new UsuarioClient(WebClient.builder().exchangeFunction(respuesta), "http://usuarios-ms");
    }

    private static ExchangeFunction responde(HttpStatus estado, String json) {
        return request -> Mono.just(ClientResponse.create(estado)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build());
    }

    @Test
    void obtenerDevuelveElUsuario() {
        AtomicReference<String> url = new AtomicReference<>();
        UsuarioClient client = cliente(request -> {
            url.set(request.url().toString());
            return responde(HttpStatus.OK, "{\"id\":2,\"nombre\":\"Camila\",\"rolId\":2,\"activo\":true}").exchange(request);
        });

        UsuarioRemoto u = client.obtener(2L);

        assertThat(url.get()).isEqualTo("http://usuarios-ms/api/usuarios/2");
        assertThat(u).isEqualTo(new UsuarioRemoto(2L, "Camila", 2L, true));
    }

    @Test
    void usuarioInexistenteLanza400() {
        UsuarioClient client = cliente(responde(HttpStatus.NOT_FOUND, "{}"));

        assertThatThrownBy(() -> client.obtener(9L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void servicioCaidoLanza503() {
        UsuarioClient client = cliente(request -> Mono.error(new IllegalStateException("conexión rechazada")));

        assertThatThrownBy(() -> client.obtener(2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void errorDelServidorLanza503() {
        UsuarioClient client = cliente(responde(HttpStatus.INTERNAL_SERVER_ERROR, "{}"));

        assertThatThrownBy(() -> client.obtener(2L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
