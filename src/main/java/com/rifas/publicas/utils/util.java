package com.rifas.publicas.utils;

import java.util.UUID;

public class util {

    // Método auxiliar para generar un código único usando Java nativo
    public String generarCodigoReferido(String nombreUsuario) {
        String base = nombreUsuario != null && nombreUsuario.length() >= 3
                ? nombreUsuario.substring(0, 3).toUpperCase()
                : "RIFA";

        // Genera una cadena alfanumérica aleatoria corta usando UUID
        String aleatorio = UUID.randomUUID().toString().substring(0, 5).toUpperCase();

        return base + "-" + aleatorio;
    }

}
