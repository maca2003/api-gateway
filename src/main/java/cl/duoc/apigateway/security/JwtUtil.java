package cl.duoc.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Optional;

/**
 * Valida la firma y expiración de los tokens emitidos por MS-AUTH.
 */
@Component
public class JwtUtil {

    private final SecretKey clave;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * @return los datos del token si es válido, o vacío si es inválido o expiró.
     */
    public Optional<Claims> validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
