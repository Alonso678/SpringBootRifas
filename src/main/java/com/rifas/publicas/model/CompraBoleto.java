package com.rifas.publicas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Entity
@Table(name = "compra_boletos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompraBoleto {

    // Mapeamos la llave primaria compuesta de la tabla pivote
    @EmbeddedId
    private CompraBoletoId id = new CompraBoletoId();

    @ManyToOne
    @MapsId("compraId")
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @ManyToOne
    @MapsId("boletoId")
    @JoinColumn(name = "boleto_id")
    private Boleto boleto;

    // Clase interna que representa los campos que forman la PK compuesta
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompraBoletoId implements Serializable {
        @Column(name = "compra_id")
        private Long compraId;

        @Column(name = "boleto_id")
        private Long boletoId;
    }
}