package com.faculdade.estandetiro.config;

import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.entity.EstiloVisual;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import com.faculdade.estandetiro.model.service.EstandeTiroService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Na primeira execução (banco vazio), cadastra quatro estandes de exemplo, ainda sem textos nem imagens. */
@Component
public class DadosIniciais implements CommandLineRunner {

    private final EstandeTiroService estandeService;

    public DadosIniciais(EstandeTiroService estandeService) {
        this.estandeService = estandeService;
    }

    @Override
    public void run(String... args) {
        if (estandeService.contar() > 0) {
            return;
        }
        estandeService.criar(new EstandeTiro("Galpão da Vila", TipoEstande.VILA_RURAL, EstiloVisual.REALISTA));
        estandeService.criar(new EstandeTiro("Salão do Castelo", TipoEstande.CASTELO, EstiloVisual.REALISTA));
        estandeService.criar(new EstandeTiro("Hangar da Ilha", TipoEstande.BASE_MILITAR, EstiloVisual.CARTOON));
        estandeService.criar(new EstandeTiro("Poço da Mina", TipoEstande.MINA, EstiloVisual.PIXEL_ART));
    }
}
