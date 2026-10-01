package com.rifas.publicas.repository;

import com.rifas.publicas.model.DatosBancarios;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DatoBancarioRepository extends JpaRepository<DatosBancarios, Long> {
    
    // Buscar si el administrador ya tiene datos bancarios registrados
    Optional<DatosBancarios> findByUsuarioId(Long usuarioId);
}