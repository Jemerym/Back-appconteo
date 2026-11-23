package cl.duocuc.usuarios_ms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlmacenamientoServiceTest {

    @TempDir
    Path carpeta;

    private AlmacenamientoService service;

    @BeforeEach
    void setUp() {
        service = new AlmacenamientoService(carpeta.resolve("avatares").toString());
    }

    private static void assertStatus(Throwable e, HttpStatus estado) {
        assertThat(e).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(estado);
    }

    @Test
    void creaLaCarpeta() {
        assertThat(carpeta.resolve("avatares")).isDirectory();
    }

    @Test
    void noPuedeCrearLaCarpetaSiEsUnArchivo() throws IOException {
        Path archivo = Files.writeString(carpeta.resolve("soy-archivo"), "x");
        assertThatThrownBy(() -> new AlmacenamientoService(archivo.toString()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void guardarLeerYEliminar() {
        byte[] bytes = {10, 20, 30};
        String nombre = service.guardar(new MockMultipartFile("archivo", "f.png", "image/png", bytes));

        assertThat(nombre).endsWith(".jpg");
        assertThat(service.leer(nombre)).containsExactly(bytes);

        service.eliminar(nombre);
        assertThat(carpeta.resolve("avatares").resolve(nombre)).doesNotExist();
    }

    @Test
    void guardarArchivoVacioLanza400() {
        assertThatThrownBy(() -> service.guardar(new MockMultipartFile("archivo", "f.jpg", "image/jpeg", new byte[0])))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
    }

    @Test
    void guardarArchivoQueNoEsImagenLanza400() {
        assertThatThrownBy(() -> service.guardar(new MockMultipartFile("archivo", "f.txt", "text/plain", new byte[]{1})))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.guardar(new MockMultipartFile("archivo", "f", null, new byte[]{1})))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
    }

    @Test
    void guardarConErrorDeLecturaLanza500() {
        MockMultipartFile roto = new MockMultipartFile("archivo", "f.jpg", "image/jpeg", new byte[]{1}) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("roto");
            }
        };
        assertThatThrownBy(() -> service.guardar(roto)).satisfies(e -> assertStatus(e, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void leerInexistenteLanza404() {
        assertThatThrownBy(() -> service.leer("no-existe.jpg")).satisfies(e -> assertStatus(e, HttpStatus.NOT_FOUND));
    }

    @Test
    void eliminarNullOInexistenteNoFalla() throws IOException {
        assertThatCode(() -> service.eliminar(null)).doesNotThrowAnyException();
        assertThatCode(() -> service.eliminar("no-existe.jpg")).doesNotThrowAnyException();

        // una carpeta con contenido no se puede borrar: la IOException se ignora
        Path sub = Files.createDirectories(carpeta.resolve("avatares").resolve("sub"));
        Files.writeString(sub.resolve("x.txt"), "x");
        assertThatCode(() -> service.eliminar("sub")).doesNotThrowAnyException();
    }
}
