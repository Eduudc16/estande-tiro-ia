package com.faculdade.estandetiro.model.repository;

import com.faculdade.estandetiro.model.entity.Arma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArmaRepository extends JpaRepository<Arma, Long> {

    List<Arma> findAllByOrderByOrdemAscIdAsc();
}
