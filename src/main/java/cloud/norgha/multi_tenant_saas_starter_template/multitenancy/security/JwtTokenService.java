package cloud.norgha.multi_tenant_saas_starter_template.multitenancy.security;



import io.jsonwebtoken.*;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public class JwtTokenService {

    private final SecretKey secretKey;

    private final long expirationSeconds;


    public JwtTokenService(String secretKey, long expirationSeconds) {
        this.secretKey = secretKey;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String userId, String tenantId, List<String> roles){

        Instant now = Instant.now();


        return Jwts.builder()
                .subject(userId)
                .claim("tenant", tenantId)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

    }

    public Jws<Claims> parseAndValidate(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);

    }
}
