package com.ecommerce.backend.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSigningAlgorithmsTest {
    private HttpServer server;
    private ECKey ec;
    private RSAKey rsa;
    private JwtDecoder decoder;
    private static final String ISSUER = "https://auth.example.test/auth/v1";

    @BeforeEach
    void setup() throws Exception {
        ec = new ECKeyGenerator(Curve.P_256).keyID("test-ec").algorithm(JWSAlgorithm.ES256).generate();
        rsa = new RSAKeyGenerator(2048).keyID("test-rsa").algorithm(JWSAlgorithm.RS256).generate();
        byte[] jwks = new JWKSet(List.of(ec.toPublicJWK(), rsa.toPublicJWK())).toString().getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            try (var output = exchange.getResponseBody()) { output.write(jwks); }
        });
        server.start();
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "jwtIssuer", ISSUER);
        ReflectionTestUtils.setField(config, "jwkSetUri", "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks");
        decoder = config.jwtDecoder();
    }

    @AfterEach
    void stop() { if (server != null) server.stop(0); }

    private String signedEc(String issuer, Instant expiry, ECKey signingKey) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(issuer).subject("test-user").audience("authenticated")
                .issueTime(Date.from(Instant.now().minusSeconds(600))).expirationTime(Date.from(expiry)).build();
        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(ec.getKeyID()).build(), claims);
        token.sign(new ECDSASigner(signingKey));
        return token.serialize();
    }

    @Test
    void acceptsValidEs256SessionsFromTrustedIssuer() throws Exception {
        String token = signedEc(ISSUER, Instant.now().plusSeconds(300), ec);
        assertEquals("test-user", decoder.decode(token).getSubject());
    }

    @Test
    void retainsRsaSupportForTrustedKeys() throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(ISSUER).subject("rsa-user")
                .expirationTime(Date.from(Instant.now().plusSeconds(300))).build();
        var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsa.getKeyID()).build(), claims);
        token.sign(new RSASSASigner(rsa));
        assertEquals("rsa-user", decoder.decode(token.serialize()).getSubject());
    }

    @Test
    void stillRejectsWrongIssuerExpiredTokensAndInvalidSignatures() throws Exception {
        String wrongIssuer = signedEc("https://untrusted.example.test", Instant.now().plusSeconds(300), ec);
        String expired = signedEc(ISSUER, Instant.now().minusSeconds(300), ec);
        ECKey wrongKey = new ECKeyGenerator(Curve.P_256).generate();
        String invalidSignature = signedEc(ISSUER, Instant.now().plusSeconds(300), wrongKey);
        assertThrows(JwtException.class, () -> decoder.decode(wrongIssuer));
        assertThrows(JwtException.class, () -> decoder.decode(expired));
        assertThrows(JwtException.class, () -> decoder.decode(invalidSignature));
    }
}
