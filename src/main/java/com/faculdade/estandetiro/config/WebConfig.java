package com.faculdade.estandetiro.config;

import com.faculdade.estandetiro.model.service.ArmazenamentoAlvos;
import com.faculdade.estandetiro.model.service.ArmazenamentoCenarios;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Serve as imagens geradas: cenários em /assets/cenarios/&lt;id&gt;.png e alvos em /assets/alvos/&lt;id&gt;.png.
 * "no-cache": o navegador sempre confere se a imagem mudou (importante depois de Regenerar).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ArmazenamentoCenarios cenarios;
    private final ArmazenamentoAlvos alvos;

    public WebConfig(ArmazenamentoCenarios cenarios, ArmazenamentoAlvos alvos) {
        this.cenarios = cenarios;
        this.alvos = alvos;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(ArmazenamentoCenarios.URL_BASE + "**")
                .addResourceLocations(local(cenarios.getPasta()))
                .setCacheControl(CacheControl.noCache());
        registry.addResourceHandler(ArmazenamentoAlvos.URL_BASE + "**")
                .addResourceLocations(local(alvos.getPasta()))
                .setCacheControl(CacheControl.noCache());
    }

    private static String local(Path pasta) {
        String uri = pasta.toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }
}
