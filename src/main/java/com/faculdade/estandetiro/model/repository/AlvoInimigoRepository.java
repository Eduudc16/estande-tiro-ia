package com.faculdade.estandetiro.model.repository;

import com.faculdade.estandetiro.model.entity.AlvoInimigo;
import com.faculdade.estandetiro.model.entity.ClasseAlvo;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlvoInimigoRepository extends JpaRepository<AlvoInimigo, Long> {

    List<AlvoInimigo> findAllByOrderByTipoEstandeAscIndiceDesenhoAsc();

    List<AlvoInimigo> findByTipoEstandeOrderByIndiceDesenhoAsc(TipoEstande tipoEstande);

    long countByClasse(ClasseAlvo classe);
}
