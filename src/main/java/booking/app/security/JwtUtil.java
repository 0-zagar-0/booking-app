package booking.app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    private final Key secet;

    @Value("${jwt.expiration}")
    private Long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secretString) {
        secet = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String name) {
        return Jwts.builder()
                .setSubject(name)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secet)
                .compact();
    }

    public boolean isValidToken(final String bearerToken) {
        try {
            Claims body = Jwts.parserBuilder()
                    .setSigningKey(secet)
                    .build()
                    .parseClaimsJws(bearerToken)
                    .getBody();
            return !body.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Expired or invalid JWT token");
        }
    }

    public String getUserName(final String bearerToken) {
        return getClaimsFromToken(bearerToken, Claims::getSubject);
    }

    private <T> T getClaimsFromToken(
            final String bearerToken, Function<Claims, T> disassembleClaims
    ) {
        return disassembleClaims.apply(
                Jwts.parserBuilder()
                        .setSigningKey(secet)
                        .build()
                        .parseClaimsJws(bearerToken)
                        .getBody()
        );
    }
}
