package com.demo.butaca.controller;

import com.demo.butaca.dto.VerificacionIdentidadDTO;
import com.demo.butaca.service.VerificacionIdentidadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/verificaciones")
public class VerificacionIdentidadController {

    private final VerificacionIdentidadService verificacionService;

    public VerificacionIdentidadController(VerificacionIdentidadService verificacionService) {
        this.verificacionService = verificacionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public VerificacionIdentidadDTO verificarIdentidad(
            @RequestParam("usuario_id") Long usuarioId,
            @RequestParam(value = "email_usuario", required = false) String emailUsuario,
            @RequestParam(value = "nombre_usuario", required = false) String nombreUsuario,
            @RequestParam(value = "apellido_usuario", required = false) String apellidoUsuario,
            @RequestParam("dni_usuario") String dniUsuario,
            @RequestParam(value = "numero_tramite", required = false) String numeroTramite,
            @RequestParam(value = "metodo_verificacion", defaultValue = "BIOMETRIA_RENAPER") String metodoVerificacion,
            @RequestPart(value = "foto_dni", required = false) MultipartFile fotoDni,
            @RequestPart(value = "selfie", required = false) MultipartFile selfie) {
        VerificacionIdentidadDTO request = VerificacionIdentidadDTO.builder()
                .usuarioId(usuarioId)
                .emailUsuario(emailUsuario)
                .nombreUsuario(nombreUsuario)
                .apellidoUsuario(apellidoUsuario)
                .dniUsuario(dniUsuario)
                .numeroTramite(numeroTramite)
                .metodoVerificacion(metodoVerificacion)
                .build();

        return verificacionService.verificarIdentidad(
                request,
                leerBytes(fotoDni),
                fotoDni == null ? null : fotoDni.getContentType(),
                leerBytes(selfie),
                selfie == null ? null : selfie.getContentType()
        );
    }

    @GetMapping("/usuario/{usuarioId}")
    public VerificacionIdentidadDTO obtenerPorUsuario(@PathVariable Long usuarioId) {
        return verificacionService.obtenerPorUsuario(usuarioId);
    }

    private byte[] leerBytes(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        try {
            return archivo.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer la imagen enviada");
        }
    }
}
