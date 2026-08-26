package com.rifas.publicas.repository;
import com.rifas.publicas.model.Boleto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface BoletoRepository extends JpaRepository<Boleto, Long> {
    
    // Sustituimos este método para que devuelva los boletos ordenados ascendentemente por su número
    // List<Boleto> findByRifaId(Long rifaId);
    List<Boleto> findByRifaIdOrderByNumeroBoletoAsc(Long rifaId);
    List<Boleto> findByRifaIdAndEstado(Long rifaId, String estado);
    List<Boleto> findByRifaIdAndEstadoOrderByNumeroBoletoAsc(Long rifaId, String estado);

    // Método para eliminar de forma masiva los boletos de una rifa
    @Modifying
    @Query("DELETE FROM Boleto b WHERE b.rifa.id = :rifaId")
    void eliminarBoletosPorRifa(@Param("rifaId") Long rifaId);

}
