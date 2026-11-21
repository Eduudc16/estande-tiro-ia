package com.faculdade.estandetiro.config;

import com.faculdade.estandetiro.model.service.ArmazenamentoCenarios;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serve as imagens geradas (pasta assets/cenarios) na URL /assets/cenarios/&lt;id&gt;.png. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ArmazenamentoCenarios armazenamento;

    public WebConfig(ArmazenamentoCenarios armazenamento) {
        this.armazenamento = armazenamento;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String local = armazenamento.getPasta().toUri().toString();
        registry.addResourceHandler(ArmazenamentoCenarios.URL_BASE + "**")
                .addResourceLocations(local.endsWith("/") ? local : local + "/")
                // "no-cache": o navegador sempre confere se a imagem mudou (importante depois de Regenerar)
                .setCacheControl(CacheControl.noCache());
    }
}
