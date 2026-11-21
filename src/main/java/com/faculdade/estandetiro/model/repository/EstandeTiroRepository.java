package com.faculdade.estandetiro.model.repository;

import com.faculdade.estandetiro.model.entity.EstandeTiro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstandeTiroRepository extends JpaRepository<EstandeTiro, Long> {

    List<EstandeTiro> findAllByOrderByIdAsc();
}
