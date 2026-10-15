package cl.duoc.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enrutamiento: cada ruta de la API se redirige al microservicio correspondiente.
 */
@Configuration
public class RutasConfig {

    @Bean
    public RouteLocator rutas(RouteLocatorBuilder builder,
                              @Value("${servicios.auth.url}") String authUrl,
                              @Value("${servicios.productos.url}") String productosUrl,
                              @Value("${servicios.ventas.url}") String ventasUrl,
                              @Value("${servicios.inventario.url}") String inventarioUrl) {
        return builder.routes()
                .route("ms-auth", r -> r.path("/api/auth/**", "/api/usuarios/**", "/api/roles/**")
                        .uri(authUrl))
                .route("ms-productos", r -> r.path("/api/productos/**", "/api/categorias/**")
                        .uri(productosUrl))
                .route("ms-ventas", r -> r.path("/api/ventas/**")
                        .uri(ventasUrl))
                .route("ms-inventario", r -> r.path("/api/inventario/**")
                        .uri(inventarioUrl))
                .build();
    }
}
