package com.faculdade.estandetiro.config;

import com.faculdade.estandetiro.model.entity.Arma;
import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.entity.EstiloVisual;
import com.faculdade.estandetiro.model.entity.TipoArma;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import com.faculdade.estandetiro.model.service.ArmaService;
import com.faculdade.estandetiro.model.service.EstandeTiroService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Cadastra dados de exemplo quando as tabelas estão vazias: 4 estandes (sem textos nem imagens) e 6 armas. */
@Component
public class DadosIniciais implements CommandLineRunner {

    private final EstandeTiroService estandeService;
    private final ArmaService armaService;

    public DadosIniciais(EstandeTiroService estandeService, ArmaService armaService) {
        this.estandeService = estandeService;
        this.armaService = armaService;
    }

    @Override
    public void run(String... args) {
        if (estandeService.contar() == 0) {
            estandeService.criar(new EstandeTiro("Galpão da Vila", TipoEstande.VILA_RURAL, EstiloVisual.REALISTA));
            estandeService.criar(new EstandeTiro("Salão do Castelo", TipoEstande.CASTELO, EstiloVisual.REALISTA));
            estandeService.criar(new EstandeTiro("Hangar da Ilha", TipoEstande.BASE_MILITAR, EstiloVisual.CARTOON));
            estandeService.criar(new EstandeTiro("Poço da Mina", TipoEstande.MINA, EstiloVisual.PIXEL_ART));
        }
        if (armaService.contar() == 0) {
            armaService.criar(new Arma("Pistola 9mm", TipoArma.PISTOLA, 1, 12, 220, 1100,
                    "Equilibrada e precisa, com recarga rápida.", 1));
            armaService.criar(new Arma("Revólver Magnum", TipoArma.REVOLVER, 4, 6, 650, 2000,
                    "Derruba até os inimigos fortes com um tiro e atravessa escudos e alvos enfileirados.", 2));
            armaService.criar(new Arma("Escopeta", TipoArma.ESCOPETA, 1, 8, 900, 2400,
                    "Seis chumbos espalhados: limpa grupos, mas cuidado com a refém por perto.", 3));
            armaService.criar(new Arma("Rifle de Precisão", TipoArma.RIFLE, 3, 5, 1100, 2200,
                    "Zoom com o botão direito ou Shift; na cabeça causa o triplo de dano.", 4));
            armaService.criar(new Arma("Submetralhadora", TipoArma.SUBMETRALHADORA, 1, 40, 90, 1700,
                    "Rajada automática enquanto o clique estiver pressionado; espalha um pouco.", 5));
            armaService.criar(new Arma("Lança-granadas", TipoArma.LANCA_GRANADAS, 5, 3, 1300, 2800,
                    "Explosão em área que derruba grupos inteiros e ignora escudos.", 6));
        }
    }
}
