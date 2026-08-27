package com.rifas.publicas.sorteo.service;

import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
public class CryptoService {

    // Llave secreta para la firma (en un entorno de producción ideal, esto va en application.properties)
    private static final String SECRET_KEY = "FortunaRifasSecureSecretKeyDigitalTicket2026";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Genera un estado aleatorio único (Random State / Salt) para evitar duplicados y predecibilidad.
     */
    public String generarRandomState() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    /**
     * Genera el sello digital (firma criptográfica HMAC-SHA256) combinando los datos clave del boleto.
     */
    public String generarSelloDigital(Long boletoId, String numeroBoleto, String usuarioEmail, String randomState) {
        try {
            // Cadena de datos que conforma la identidad inalterable del boleto
            String datosAFirmar = boletoId + "|" + numeroBoleto + "|" + usuarioEmail + "|" + randomState;

            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKeySpec);

            byte[] hashBytes = mac.doFinal(datosAFirmar.getBytes(StandardCharsets.UTF_8));
            
            // Convertimos la firma binaria a formato Hexadecimal o Base64 legible
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el sello digital criptográfico", e);
        }
    }

    /**
     * Valida si un sello digital presentado es auténtico recalculando la firma.
     */
    public boolean verificarSello(Long boletoId, String numeroBoleto, String usuarioEmail, String randomState, String selloFirmadoRecibido) {
        String selloCalculado = generarSelloDigital(boletoId, numeroBoleto, usuarioEmail, randomState);
        return selloCalculado.equals(selloFirmadoRecibido);
    }
}