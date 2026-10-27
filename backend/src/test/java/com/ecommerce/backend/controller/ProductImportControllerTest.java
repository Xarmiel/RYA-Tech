package com.ecommerce.backend.controller;

import com.ecommerce.backend.service.ProductImportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductImportControllerTest {
    @Test void exigeSkuRevisionDecisionYDatosValidos() throws Exception {
        var service=mock(ProductImportService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new ProductImportController(service)).build();
        var path="/api/v1/admin/productos/"+UUID.randomUUID()+"/importacion";
        for(var body:List.of("{}","{\"sku\":\"SKU\",\"modo\":\"completar\",\"campos\":[],\"datos\":{}}",
            "{\"sku\":\"SKU\",\"revision\":\"r\",\"modo\":\"actualizar\",\"campos\":[\"precioBase\"],\"datos\":{\"precioBase\":-1}}"))
            mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
