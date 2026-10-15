package cl.duoc.apigateway.security;

import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Filtro global: valida el token JWT y el rol antes de enrutar la petición.
 * Si es válido, agrega los headers X-Usuario y X-Rol para los microservicios.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String PREFIJO = "Bearer ";

    private final JwtUtil jwtUtil;
    private final ReglasAcceso reglasAcceso;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ReglasAcceso reglasAcceso) {
        this.jwtUtil = jwtUtil;
        this.reglasAcceso = reglasAcceso;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod metodo = request.getMethod();
        String path = request.getURI().getPath();

        // Nunca se aceptan estos headers desde el cliente
        ServerHttpRequest.Builder builder = request.mutate()
                .headers(h -> {
                    h.remove("X-Usuario");
                    h.remove("X-Rol");
                });

        if (reglasAcceso.esPublica(metodo, path)) {
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIJO)) {
            return responderError(exchange, HttpStatus.UNAUTHORIZED, "Falta el token de autenticación");
        }

        Optional<Claims> claims = jwtUtil.validar(header.substring(PREFIJO.length()));
        if (claims.isEmpty()) {
            return responderError(exchange, HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
        }

        String usuario = claims.get().getSubject();
        String rol = claims.get().get("rol", String.class);
        if (!reglasAcceso.tieneAcceso(rol, metodo, path)) {
            return responderError(exchange, HttpStatus.FORBIDDEN, "No tiene permisos para esta operación");
        }

        ServerHttpRequest requestConUsuario = builder
                .header("X-Usuario", usuario)
                .header("X-Rol", rol)
                .build();
        return chain.filter(exchange.mutate().request(requestConUsuario).build());
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private Mono<Void> responderError(ServerWebExchange exchange, HttpStatus status, String mensaje) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"mensaje\":\"%s\",\"path\":\"%s\"}",
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje,
                exchange.getRequest().getURI().getPath());
        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
