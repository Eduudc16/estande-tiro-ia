package com.faculdade.estandetiro.config;

import com.faculdade.estandetiro.model.entity.AlvoInimigo;
import com.faculdade.estandetiro.model.entity.Arma;
import com.faculdade.estandetiro.model.entity.ClasseAlvo;
import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.entity.EstiloVisual;
import com.faculdade.estandetiro.model.entity.TipoArma;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import com.faculdade.estandetiro.model.service.AlvoInimigoService;
import com.faculdade.estandetiro.model.service.ArmaService;
import com.faculdade.estandetiro.model.service.EstandeTiroService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Cadastra dados de exemplo quando as tabelas estão vazias: 4 estandes (sem textos nem imagens), os inimigos e a
 * refém de cada tipo de estande (sem imagens) e 6 armas.
 */
@Component
@Order(1)   // depois do AjusteColunasEnum
public class DadosIniciais implements CommandLineRunner {

    private final EstandeTiroService estandeService;
    private final ArmaService armaService;
    private final AlvoInimigoService alvoService;

    public DadosIniciais(EstandeTiroService estandeService, ArmaService armaService,
                         AlvoInimigoService alvoService) {
        this.estandeService = estandeService;
        this.armaService = armaService;
        this.alvoService = alvoService;
    }

    /**
     * Inimigos de cada tipo, na mesma ordem dos desenhos de static/js/alvos.js (usados enquanto não houver imagem).
     * As descrições são genéricas e originais: nenhuma cita jogo ou personagem.
     */
    private void criarAlvos() {
        alvo(TipoEstande.VILA_RURAL, 0, "Aldeão com forcado", ClasseAlvo.COMUM,
                "camponês hostil de uma vila europeia antiga, de boina escura, camisa de linho surrada e calça de "
                        + "lona, segurando um forcado na diagonal à frente do corpo");
        alvo(TipoEstande.VILA_RURAL, 1, "Aldeão com tocha", ClasseAlvo.COMUM,
                "camponês hostil de uma vila europeia antiga, de colete marrom e boina, segurando uma tocha acesa "
                        + "ao lado do corpo");
        alvo(TipoEstande.VILA_RURAL, 2, "Aldeã com foice", ClasseAlvo.AGIL,
                "camponesa hostil de uma vila europeia antiga, com lenço na cabeça, blusa surrada e saia longa "
                        + "escura, segurando uma foice ao lado do corpo");
        alvo(TipoEstande.VILA_RURAL, 3, "Lenhador com machado", ClasseAlvo.FORTE,
                "lenhador hostil, alto, robusto e barbudo, de colete de couro e camisa encardida, segurando um "
                        + "machado grande com as duas mãos à frente do corpo");
        alvo(TipoEstande.CASTELO, 0, "Cultista com cajado", ClasseAlvo.COMUM,
                "cultista encapuzado de manto roxo escuro, com o rosto na sombra do capuz, segurando um cajado de "
                        + "madeira com uma pedra roxa na ponta");
        alvo(TipoEstande.CASTELO, 1, "Cultista com foice", ClasseAlvo.AGIL,
                "cultista encapuzado de manto vermelho escuro, com o rosto na sombra do capuz, segurando uma foice");
        alvo(TipoEstande.CASTELO, 2, "Guardião mascarado", ClasseAlvo.FORTE,
                "guardião de manto preto com máscara branca lisa, segurando um escudo de metal e um bastão");
        alvo(TipoEstande.BASE_MILITAR, 0, "Soldado com cassetete", ClasseAlvo.COMUM,
                "soldado de uniforme verde-oliva, capacete e máscara de gás, segurando um cassetete");
        alvo(TipoEstande.BASE_MILITAR, 1, "Soldado com faca", ClasseAlvo.AGIL,
                "soldado ágil de uniforme verde-oliva, capacete e máscara de gás, segurando uma faca de combate");
        alvo(TipoEstande.BASE_MILITAR, 2, "Soldado pesado com escudo", ClasseAlvo.FORTE,
                "soldado pesado de armadura cinza, capacete e máscara de gás, segurando um escudo antimotim e uma "
                        + "marreta");
        alvo(TipoEstande.MINA, 0, "Mineiro com picareta", ClasseAlvo.COMUM,
                "mineiro possuído de capacete amarelo com lanterna e roupas sujas de carvão, segurando uma picareta");
        alvo(TipoEstande.MINA, 1, "Mineiro com dinamite", ClasseAlvo.AGIL,
                "mineiro possuído de capacete amarelo com lanterna, segurando uma banana de dinamite de pavio aceso");
        alvo(TipoEstande.MINA, 2, "Brutamontes com marreta", ClasseAlvo.FORTE,
                "minerador brutamontes, enorme e barbudo, de avental de couro, segurando uma marreta pesada");
        alvo(TipoEstande.PORTO, 0, "Pirata com sabre", ClasseAlvo.COMUM,
                "pirata sombrio de bandana vermelha e camisa listrada, segurando um sabre curvo");
        alvo(TipoEstande.PORTO, 1, "Marujo com gancho", ClasseAlvo.AGIL,
                "marujo sombrio de bandana azul e camisa listrada, com um gancho de metal no lugar de uma das mãos");
        alvo(TipoEstande.PORTO, 2, "Capitão pirata", ClasseAlvo.FORTE,
                "capitão pirata sombrio, alto e barbudo, de casaco longo escuro e chapéu tricórnio, segurando um sabre");
    }

    /**
     * A refém de cada tipo (índice -1: fica antes dos inimigos e, sem imagem, usa o desenho da refém).
     * Visual original e discreto: mãos amarradas e expressão de medo.
     */
    private void criarRefens() {
        String maos = ", com as mãos amarradas à frente por uma corda grossa";
        alvo(TipoEstande.VILA_RURAL, -1, "Refém da vila", ClasseAlvo.REFEM,
                "jovem refém assustada de uma vila europeia antiga, de cabelo castanho-escuro preso em rabo de cavalo, "
                        + "blusa de linho branca, colete azul-marinho e saia longa azul" + maos);
        alvo(TipoEstande.CASTELO, -1, "Refém do castelo", ClasseAlvo.REFEM,
                "jovem refém assustada, de cabelo preto preso em trança, vestido longo e simples cinza-claro de "
                        + "criada de castelo" + maos);
        alvo(TipoEstande.BASE_MILITAR, -1, "Refém da base", ClasseAlvo.REFEM,
                "jovem refém assustada, de cabelo ruivo curto, macacão laranja de prisioneira, com as mãos presas à "
                        + "frente por uma braçadeira de plástico");
        alvo(TipoEstande.MINA, -1, "Refém da mina", ClasseAlvo.REFEM,
                "jovem refém assustada, de cabelo castanho preso em coque, camisa xadrez, calça de trabalho e rosto "
                        + "sujo de carvão" + maos);
        alvo(TipoEstande.PORTO, -1, "Refém do porto", ClasseAlvo.REFEM,
                "jovem refém assustada, de cabelo preto ondulado, blusa listrada de marinheiro e saia longa escura"
                        + maos);
    }

    private void alvo(TipoEstande tipo, int indice, String nome, ClasseAlvo classe, String descricao) {
        alvoService.criar(new AlvoInimigo(tipo, indice, nome, classe, descricao));
    }

    @Override
    public void run(String... args) {
        if (estandeService.contar() == 0) {
            estandeService.criar(new EstandeTiro("Galpão da Vila", TipoEstande.VILA_RURAL, EstiloVisual.REALISTA));
            estandeService.criar(new EstandeTiro("Salão do Castelo", TipoEstande.CASTELO, EstiloVisual.REALISTA));
            estandeService.criar(new EstandeTiro("Hangar da Ilha", TipoEstande.BASE_MILITAR, EstiloVisual.CARTOON));
            estandeService.criar(new EstandeTiro("Poço da Mina", TipoEstande.MINA, EstiloVisual.PIXEL_ART));
        }
        if (alvoService.contar() == 0) {
            criarAlvos();
        }
        if (alvoService.contarRefens() == 0) {
            criarRefens();
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
