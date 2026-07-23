package com.rifas.publicas.repository;
import com.rifas.publicas.model.Boleto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BoletoRepository extends JpaRepository<Boleto, Long> {
    List<Boleto> findByRifaId(Long rifaId);
    List<Boleto> findByRifaIdAndEstado(Long rifaId, String estado);
}
