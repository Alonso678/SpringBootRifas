package com.rifas.publicas.repository;
import com.rifas.publicas.model.Boleto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BoletoRepository extends JpaRepository<Boleto, Long> {
    
    // Sustituimos este método para que devuelva los boletos ordenados ascendentemente por su número
    // List<Boleto> findByRifaId(Long rifaId);
    List<Boleto> findByRifaIdOrderByNumeroBoletoAsc(Long rifaId);
    List<Boleto> findByRifaIdAndEstado(Long rifaId, String estado);
}
