package com.ecommerce.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import java.net.URI;

/** Fail closed on incomplete production setup without logging secret values. */
@Configuration
@Profile("prod")
public class ProductionConfiguration {
    public ProductionConfiguration(Environment environment) {
        for(String name:new String[]{"DB_URL","DB_USERNAME","DB_PASSWORD","SUPABASE_JWT_ISSUER","SUPABASE_JWKS_URI"}) {
            String value=environment.getProperty(name);
            if(value==null||value.isBlank())throw new IllegalStateException("Falta configurar la variable de producción: "+name);
        }
        // An empty list intentionally denies cross-origin access until Vercel has a URL.
        String origins=environment.getProperty("CORS_ALLOWED_ORIGINS","");
        if(!origins.isBlank())for(String origin:origins.split(",",-1)) {
            try {
                URI uri=URI.create(origin.trim());
                if(!"https".equals(uri.getScheme())||uri.getHost()==null||origin.contains("*")||uri.getRawUserInfo()!=null
                    ||uri.getRawQuery()!=null||uri.getRawFragment()!=null||(uri.getRawPath()!=null&&!uri.getRawPath().isEmpty()))throw new IllegalArgumentException();
            }catch(IllegalArgumentException ex){throw new IllegalStateException("CORS_ALLOWED_ORIGINS debe contener orígenes HTTPS exactos, sin ruta ni comodines.");}
        }
    }
}
