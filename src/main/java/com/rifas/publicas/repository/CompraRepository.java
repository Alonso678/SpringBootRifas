package com.rifas.publicas.repository;
import com.rifas.publicas.model.Compra;
import com.rifas.publicas.model.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByUsuarioId(Long usuarioId);

    @Modifying
    @Query("DELETE FROM Compra c WHERE c.rifa.id = :rifaId")
    void eliminarPorRifaId(@Param("rifaId") Long rifaId);

    // NUEVO: Método para borrar únicamente compras rechazadas por rifa
    @Modifying
    @Query("DELETE FROM Compra c WHERE c.rifa.id = :rifaId AND c.estadoPago = 'RECHAZADO'")
    void eliminarComprasRechazadasPorRifaId(@Param("rifaId") Long rifaId);

    @Query("SELECT c FROM Compra c JOIN FETCH c.rifa r JOIN FETCH r.administrador WHERE c.usuario = :usuario AND c.estadoPago = :estadoPago")
    List<Compra> findByUsuarioAndEstadoPago(@Param("usuario") Usuario usuario, @Param("estadoPago") String estadoPago);

    // Nuevo método para filtrar compras por el correo del administrador de la rifa asociada
    List<Compra> findByRifaAdministradorEmail(String email);
}