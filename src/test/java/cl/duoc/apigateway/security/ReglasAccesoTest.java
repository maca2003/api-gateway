package cl.duoc.apigateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglasAccesoTest {

    private final ReglasAcceso reglas = new ReglasAcceso();

    @Test
    void loginYRegistroSonPublicos() {
        assertTrue(reglas.esPublica(HttpMethod.POST, "/api/auth/login"));
        assertTrue(reglas.esPublica(HttpMethod.POST, "/api/auth/registro"));
        assertFalse(reglas.esPublica(HttpMethod.GET, "/api/productos"));
    }

    @Test
    void adminTieneAccesoTotal() {
        assertTrue(reglas.tieneAcceso("ADMIN", HttpMethod.DELETE, "/api/productos/1"));
        assertTrue(reglas.tieneAcceso("ADMIN", HttpMethod.GET, "/api/usuarios"));
    }

    @Test
    void vendedorConsultaYRegistraVentasPeroNoAdministra() {
        assertTrue(reglas.tieneAcceso("VENDEDOR", HttpMethod.GET, "/api/productos"));
        assertTrue(reglas.tieneAcceso("VENDEDOR", HttpMethod.POST, "/api/ventas"));
        assertFalse(reglas.tieneAcceso("VENDEDOR", HttpMethod.POST, "/api/productos"));
        assertFalse(reglas.tieneAcceso("VENDEDOR", HttpMethod.GET, "/api/usuarios"));
        assertFalse(reglas.tieneAcceso("VENDEDOR", HttpMethod.DELETE, "/api/ventas/1"));
    }
}
