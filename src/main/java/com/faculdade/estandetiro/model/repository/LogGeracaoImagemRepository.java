package com.faculdade.estandetiro.model.repository;

import com.faculdade.estandetiro.model.entity.LogGeracaoImagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LogGeracaoImagemRepository extends JpaRepository<LogGeracaoImagem, Long> {

    @Query("select l from LogGeracaoImagem l left join fetch l.estandeTiro left join fetch l.alvoInimigo "
            + "order by l.dataHora desc, l.id desc")
    List<LogGeracaoImagem> listarComEstande();

    long countByAlvoInimigoId(Long alvoInimigoId);

    List<LogGeracaoImagem> findTop5ByEstandeTiroIdOrderByDataHoraDescIdDesc(Long estandeId);

    long countBySucesso(boolean sucesso);

    /** Mantem o historico de gastos quando um estande e excluido. */
    @Modifying
    @Query("update LogGeracaoImagem l set l.estandeTiro = null where l.estandeTiro.id = :estandeId")
    int desvincularEstande(@Param("estandeId") Long estandeId);
}
