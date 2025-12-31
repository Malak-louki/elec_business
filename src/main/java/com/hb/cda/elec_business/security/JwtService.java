package com.hb.cda.elec_business.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.validation.ValidationException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    /**
     * Clé secrète (Base64) utilisée pour signer les JWT.
     * Configurée dans application.properties : app.jwt.secret.key
     */
    @Value("${app.jwt.secret.key}")
    private String secretKey;

    /**
     * Durée de vie de l'access token en millisecondes.
     * Configurée dans application.properties : app.jwt.access.expiration
     */
    @Getter
    @Value("${app.jwt.access.expiration}")
    private long jwtExpiration;

    // ======================================================================
    // 1. GÉNÉRATION DE TOKEN
    // ======================================================================

    /**
     * Génère un access token (JWT) pour un utilisateur.
     * Pour l’instant on ne met que le subject (email) + dates.
     */
    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(userDetails, jwtExpiration, new HashMap<>());
    }

    /**
     * Méthode générique qui génère un JWT.
     *
     * @param userDetails    infos de sécurité (username = email chez toi)
     * @param expirationInMs durée de vie en millisecondes
     * @param extraClaims    claims supplémentaires (ex: roles) – optionnel
     */
    public String generateToken(
            UserDetails userDetails,
            long expirationInMs,
            Map<String, Object> extraClaims
    ) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationInMs);

        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())      // subject = email
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ======================================================================
    // 2. VALIDATION DU TOKEN
    // ======================================================================

    /**
     * Vérifie que :
     *  - le token n’est pas vide
     *  - le subject (email) correspond au user
     *  - le token n’est pas expiré
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("Token is missing or empty");
        }
        final String username = extractUsernameFromToken(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ======================================================================
    // 3. EXTRACTION DE CLAIMS
    // ======================================================================

    public String extractUsernameFromToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("Token is missing or empty");
        }
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = this.extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException | SignatureException | MalformedJwtException e) {
            log.error("Failed to extract claims from JWT: {}", e.getMessage());
            throw e;
        }
    }

    // ======================================================================
    // 4. CLÉ DE SIGNATURE
    // ======================================================================

    private Key getSignInKey() {
        // secretKey est en Base64 → on la décode
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
