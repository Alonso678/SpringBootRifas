package com.rifas.publicas.sorteo.repository;

import com.rifas.publicas.sorteo.model.BoletoDigital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BoletoDigitalRepository extends JpaRepository<BoletoDigital, Long> {

    Optional<BoletoDigital> findByRandomState(String randomState);

    @Query("SELECT COUNT(bd) FROM BoletoDigital bd WHERE bd.boleto.rifa.id = :rifaId AND bd.boleto.estado = 'PAGADO'")
    long countByRifaIdAndVendidoTrue(@Param("rifaId") Long rifaId);

    boolean existsByBoletoId(Long boletoId);
    
    Optional<BoletoDigital> findByBoletoId(Long boletoId);
}
