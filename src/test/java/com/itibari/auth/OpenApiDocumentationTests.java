package com.itibari.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OpenApiDocumentationTests {
    @Autowired
    MockMvc mvc;

    @Test
    void documentationIsPublicAndUsesTheRequestServer() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths").isNotEmpty())
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost"));
        mvc.perform(get("/v3/api-docs")
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "api-pos-dev.itibari.io")
                        .header("X-Forwarded-Port", "443")
                        .header("X-Forwarded-Prefix", "/api/v1/pos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("https://api-pos-dev.itibari.io/api/v1/pos"));
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost"));
    }

    @Test
    void swaggerUiAndItsConfigurationArePublic() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/swagger-ui/index.html")));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk());
    }
}
