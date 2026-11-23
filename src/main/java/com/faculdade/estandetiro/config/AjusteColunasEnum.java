package com.faculdade.estandetiro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Migração simples para bancos criados por versões anteriores: o H2 tinha guardado as colunas de enum como tipo
 * ENUM nativo, que recusa valores novos (ex.: a classe REFEM). Converte essas colunas para VARCHAR, sem perder dados.
 * Roda antes dos dados iniciais e não faz nada se a coluna já for texto.
 */
@Component
@Order(0)
public class AjusteColunasEnum implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AjusteColunasEnum.class);

    /** tabela, coluna e tamanho do texto. */
    private static final List<String[]> COLUNAS = List.of(
            new String[]{"ESTANDE_TIRO", "TIPO", "30"},
            new String[]{"ESTANDE_TIRO", "ESTILO_VISUAL", "20"},
            new String[]{"ARMA", "TIPO", "20"},
            new String[]{"ALVO_INIMIGO", "TIPO_ESTANDE", "30"},
            new String[]{"ALVO_INIMIGO", "CLASSE", "10"});

    private final JdbcTemplate jdbc;

    public AjusteColunasEnum(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        for (String[] c : COLUNAS) {
            try {
                List<String> tipo = jdbc.queryForList("SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?", String.class, c[0], c[1]);
                if (!tipo.isEmpty() && "ENUM".equalsIgnoreCase(tipo.get(0))) {
                    jdbc.execute("ALTER TABLE " + c[0] + " ALTER COLUMN " + c[1] + " SET DATA TYPE VARCHAR(" + c[2] + ")");
                    log.info("Coluna {}.{} convertida de ENUM para VARCHAR({}).", c[0], c[1], c[2]);
                }
            } catch (RuntimeException e) {
                log.warn("Não foi possível conferir a coluna {}.{}: {}", c[0], c[1], e.getMessage());
            }
        }
    }
}
