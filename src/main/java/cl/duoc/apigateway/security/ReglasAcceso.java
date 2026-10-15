package cl.duoc.apigateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * Define qué rutas son públicas y qué rol se necesita para cada operación.
 * Roles: ADMIN (acceso total) y VENDEDOR (consultar y registrar ventas).
 */
@Component
public class ReglasAcceso {

    public static final String ADMIN = "ADMIN";

    public boolean esPublica(HttpMethod metodo, String path) {
        return HttpMethod.POST.equals(metodo)
                && (path.equals("/api/auth/login") || path.equals("/api/auth/registro"));
    }

    public boolean tieneAcceso(String rol, HttpMethod metodo, String path) {
        if (ADMIN.equals(rol)) {
            return true;
        }
        // Gestión de usuarios y roles: solo ADMIN
        if (path.startsWith("/api/usuarios") || path.startsWith("/api/roles")) {
            return false;
        }
        // Ventas: cualquier usuario autenticado puede consultar y registrar
        if (path.startsWith("/api/ventas")) {
            return HttpMethod.GET.equals(metodo) || HttpMethod.POST.equals(metodo);
        }
        // Productos, categorías e inventario: los demás roles solo consultan
        return HttpMethod.GET.equals(metodo);
    }
}
