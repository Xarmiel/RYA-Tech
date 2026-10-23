package com.ecommerce.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationTest {
    private MockEnvironment configured(){return new MockEnvironment().withProperty("DB_URL","jdbc:postgresql://example.invalid/postgres?sslmode=require")
        .withProperty("DB_USERNAME","test").withProperty("DB_PASSWORD","test-only-value")
        .withProperty("SUPABASE_JWT_ISSUER","https://example.invalid/auth/v1").withProperty("SUPABASE_JWKS_URI","https://example.invalid/jwks");}
    @Test void acceptsExactOriginsAndAllowsNoCorsBeforeFrontendExists(){
        assertDoesNotThrow(()->new ProductionConfiguration(configured()));
        assertDoesNotThrow(()->new ProductionConfiguration(configured().withProperty("CORS_ALLOWED_ORIGINS","https://rya.example,https://shop.example")));
    }
    @Test void rejectsMissingSecretsWithoutIncludingTheirValues(){
        var env=configured();env.setProperty("DB_PASSWORD","");
        var error=assertThrows(IllegalStateException.class,()->new ProductionConfiguration(env));
        assertTrue(error.getMessage().contains("DB_PASSWORD"));assertFalse(error.getMessage().contains("test-only-value"));
    }
    @Test void rejectsBroadOriginsPathsAndNonHttps(){
        for(String origin:new String[]{"https://*.vercel.app","http://shop.example","https://shop.example/login","https://shop.example/","https://user:secret@shop.example","https://shop.example?token=secret","https://shop.example,"}){
            var error=assertThrows(IllegalStateException.class,()->new ProductionConfiguration(configured().withProperty("CORS_ALLOWED_ORIGINS",origin)));
            assertFalse(error.getMessage().contains("secret"));
        }
    }
}
