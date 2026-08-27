package com.rifas.publicas.sorteo.repository;

import com.rifas.publicas.sorteo.model.ConfigSorteo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigSorteoRepository extends JpaRepository<ConfigSorteo, Long> {
    
    // Permite buscar la configuración del sorteo asociada a una rifa específica
    Optional<ConfigSorteo> findByRifaId(Long rifaId);
}