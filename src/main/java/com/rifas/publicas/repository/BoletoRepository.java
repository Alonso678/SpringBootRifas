package com.rifas.publicas.repository;

import com.rifas.publicas.model.Boleto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoletoRepository extends JpaRepository<Boleto, Long> {

    // Agregas este método para obtener los boletos de una rifa específica ordenados por número de boleto
    List<Boleto> findByRifaIdOrderByNumeroBoletoAsc(Long rifaId);

    // Agregas este método para obtener los boletos de una rifa específica junto con la información del usuario que los compró, ordenados por número de boleto
    @Query("SELECT b FROM Boleto b LEFT JOIN FETCH b.usuario WHERE b.rifa.id = :rifaId ORDER BY b.numeroBoleto ASC")
    List<Boleto> findByRifaIdConUsuarioOrdenados(@Param("rifaId") Long rifaId);

    // Agregas este método para obtener los boletos de una rifa específica filtrados por estado
    List<Boleto> findByRifaIdAndEstado(Long rifaId, String estado);

    // Agregas este método para obtener los boletos de una rifa específica filtrados por estado y ordenados por número de boleto
    List<Boleto> findByRifaIdAndEstadoOrderByNumeroBoletoAsc(Long rifaId, String estado);

    // Método para eliminar de forma masiva los boletos de una rifa
    @Modifying
    @Query("DELETE FROM Boleto b WHERE b.rifa.id = :rifaId")
    void eliminarBoletosPorRifa(@Param("rifaId") Long rifaId);

    // Cuenta cuántos boletos están pagados para una rifa específica
    @Query("SELECT COUNT(b) FROM Boleto b WHERE b.rifa.id = :rifaId AND b.estado = 'PAGADO'")
    long countByRifaIdAndPagado(@Param("rifaId") Long rifaId);

    // Cuenta cuántos boletos están pendientes de pago para una rifa específica
    @Query("SELECT COUNT(b) FROM Boleto b WHERE b.rifa.id = :rifaId AND b.estado = 'APARTADO'")
    long countByRifaIdAndPendiente(@Param("rifaId") Long rifaId);

    // Obtiene los boletos pagados de una rifa específica
    @Query("SELECT b FROM Boleto b WHERE b.rifa.id = :rifaId AND b.estado = 'PAGADO'")
    List<Boleto> findByRifaIdAndPagado(@Param("rifaId") Long rifaId);

    // Cuenta cuántos boletos tiene una rifa específica
    int countByRifaId(Long id);

    // Obtiene los boletos de una rifa específica ordenados por número de boleto en orden descendente
    List<Boleto> findByRifaIdAndEstadoOrderByNumeroBoletoDesc(Long id, String string);

    // Genera boletos en lote para una rifa específica
    @Modifying
    @Query(value = "INSERT INTO boletos (numero_boleto, estado, rifa_id) " +
            "SELECT s, 'DISPONIBLE', :rifaId " +
            "FROM generate_series(:inicio, :fin) s", nativeQuery = true)
    void generarBoletosEnLote(@Param("rifaId") Long rifaId,
            @Param("inicio") int inicio,
            @Param("fin") int fin);

    // Elimina boletos disponibles en lote para una rifa específica
    @Modifying
    @Query(value = "DELETE FROM boletos WHERE id IN (" +
            "  SELECT id FROM boletos " +
            "  WHERE rifa_id = :rifaId AND estado = 'DISPONIBLE' " +
            "  ORDER BY numero_boleto DESC " +
            "  LIMIT :limite" +
            ")", nativeQuery = true)
    void eliminarBoletosDisponiblesEnLote(@Param("rifaId") Long rifaId,
            @Param("limite") int limite);

}
