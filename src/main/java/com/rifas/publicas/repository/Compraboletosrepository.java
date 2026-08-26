package com.rifas.publicas.repository;

import com.rifas.publicas.model.CompraBoleto; // Asegúrate de importar tu entidad
import org.springframework.data.jpa.repository.JpaRepository; // Importante
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Compraboletosrepository extends JpaRepository<CompraBoleto, CompraBoleto.CompraBoletoId> {

    @Modifying
    @Query("DELETE FROM CompraBoleto cb WHERE cb.compra.id IN (SELECT c.id FROM Compra c WHERE c.rifa.id = :rifaId AND c.estadoPago = 'RECHAZADO')")
    void eliminarRechazadosPorRifaId(@Param("rifaId") Long rifaId);

    @Modifying
    @Query("DELETE FROM CompraBoleto cb WHERE cb.compra.id IN (SELECT c.id FROM Compra c WHERE c.rifa.id = :rifaId)")
    void eliminarPorRifaId(@Param("rifaId") Long rifaId);

}