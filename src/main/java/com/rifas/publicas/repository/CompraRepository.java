package com.rifas.publicas.repository;
import com.rifas.publicas.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByUsuarioId(Long usuarioId);
}
