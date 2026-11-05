package com.mycompany.sistemainventariov3.resources;

import com.google.gson.Gson;
import com.mycompany.sistemainventariov3.dto.ActaMantenimientoPcRequest;
import com.mycompany.sistemainventariov3.dto.ActaRedesComunicacionesRequest;
import com.mycompany.sistemainventariov3.dto.ActaSoftwareRequest;
import com.mycompany.sistemainventariov3.dto.ApiResponse;
import com.mycompany.sistemainventariov3.model.Usuario;
import com.mycompany.sistemainventariov3.model.ActaMantenimiento;
import com.mycompany.sistemainventariov3.service.ActaMantenimientoPcDocumentService;
import com.mycompany.sistemainventariov3.service.ActaMantenimientoImpresoraDocumentService;
import com.mycompany.sistemainventariov3.service.ActaMantenimientoProyectorDocumentService;
import com.mycompany.sistemainventariov3.service.ActaMantenimientoEscanerDocumentService;
import com.mycompany.sistemainventariov3.service.ActaMantenimientoTelefonoDocumentService;
import com.mycompany.sistemainventariov3.service.ActaSoftwareDocumentService;
import com.mycompany.sistemainventariov3.service.ActaRedesComunicacionesDocumentService;
import com.mycompany.sistemainventariov3.service.ActaLicenciasDocumentService;
import com.mycompany.sistemainventariov3.service.ActaMantenimientoService;
import com.mycompany.sistemainventariov3.service.DatabaseService;
import com.mycompany.sistemainventariov3.util.SesionUsuario;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

@Path("actas")
public class ActasResource {

    @Context
    private HttpServletRequest servletRequest;

    private final Gson gson = new Gson();
    private final ActaMantenimientoPcDocumentService documentService = new ActaMantenimientoPcDocumentService();
    private final ActaMantenimientoImpresoraDocumentService impresoraDocumentService = new ActaMantenimientoImpresoraDocumentService();
    private final ActaMantenimientoProyectorDocumentService proyectorDocumentService = new ActaMantenimientoProyectorDocumentService();
    private final ActaMantenimientoEscanerDocumentService escanerDocumentService = new ActaMantenimientoEscanerDocumentService();
    private final ActaMantenimientoTelefonoDocumentService telefonoDocumentService = new ActaMantenimientoTelefonoDocumentService();
    private final ActaSoftwareDocumentService softwareDocumentService = new ActaSoftwareDocumentService();
    private final ActaRedesComunicacionesDocumentService redesComunicacionesDocumentService = new ActaRedesComunicacionesDocumentService();
    private final ActaMantenimientoService actaMantenimientoService = new ActaMantenimientoService();
    private final ActaLicenciasDocumentService licenciasDocumentService = new ActaLicenciasDocumentService();

    @GET
    @Path("listar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listarActas() {
        try {
            validarAutenticacion();
            Usuario usuario = SesionUsuario.getUsuarioActual(servletRequest);
            List<ActaMantenimiento> lista = actaMantenimientoService.listarActas(usuario);
            ApiResponse<List<ActaMantenimiento>> response = ApiResponse.success("Lista de actas obtenida", lista);
            return Response.ok(gson.toJson(response)).build();
        } catch (Exception e) {
            ApiResponse<?> response = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(response)).build();
        }
    }

    @GET
    @Path("descargar/{id}/{format}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response descargarActa(@PathParam("id") int id, @PathParam("format") String format) {
        try {
            validarAutenticacion();
            ActaMantenimiento acta = actaMantenimientoService.obtenerPorId(id);
            if (acta == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Validar que si el usuario es CUSTODIO, solo pueda descargar sus propias actas
            Usuario usuarioActual = SesionUsuario.getUsuarioActual(servletRequest);
            if (usuarioActual != null && "CUSTODIO".equals(usuarioActual.getRol())) {
                boolean esSuya = false;
                if (usuarioActual.getIdCustodio() != null && usuarioActual.getIdCustodio().equals(acta.getIdCustodio())) {
                    esSuya = true;
                } else if (usuarioActual.getNombreCompleto() != null && 
                           (usuarioActual.getNombreCompleto().equalsIgnoreCase(acta.getNombreCustodio()) ||
                            acta.getNombreCustodio().toUpperCase().contains(usuarioActual.getNombreCompleto().toUpperCase()))) {
                    esSuya = true;
                }
                if (!esSuya) {
                    return Response.status(Response.Status.FORBIDDEN).entity("No tiene permisos para descargar esta acta.").build();
                }
            }

            String json = acta.getJsonPayload();
            String tipo = acta.getTipoActa();
            byte[] content = null;
            String mediaType = "";
            String extension = format.trim().toLowerCase();
            String fileName = "acta_" + tipo + "_" + id + "." + extension;

            if ("rc".equals(tipo)) {
                ActaRedesComunicacionesRequest req = gson.fromJson(json, ActaRedesComunicacionesRequest.class);
                if ("docx".equals(extension)) {
                    content = redesComunicacionesDocumentService.generarDocx(req);
                    mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                } else {
                    content = redesComunicacionesDocumentService.generarPdf(req);
                    mediaType = "application/pdf";
                }
            } else if ("software".equals(tipo)) {
                ActaSoftwareRequest req = gson.fromJson(json, ActaSoftwareRequest.class);
                if ("docx".equals(extension)) {
                    content = softwareDocumentService.generarDocx(req);
                    mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                } else {
                    content = softwareDocumentService.generarPdf(req);
                    mediaType = "application/pdf";
                }
            } else {
                ActaMantenimientoPcRequest req = gson.fromJson(json, ActaMantenimientoPcRequest.class);
                if ("pc".equals(tipo)) {
                    if ("docx".equals(extension)) {
                        content = documentService.generarDocx(req);
                        mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    } else {
                        content = documentService.generarPdf(req);
                        mediaType = "application/pdf";
                    }
                } else if ("impresora".equals(tipo)) {
                    if ("docx".equals(extension)) {
                        content = impresoraDocumentService.generarDocx(req);
                        mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    } else {
                        content = impresoraDocumentService.generarPdf(req);
                        mediaType = "application/pdf";
                    }
                } else if ("telefono".equals(tipo)) {
                    if ("docx".equals(extension)) {
                        content = telefonoDocumentService.generarDocx(req);
                        mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    } else {
                        content = telefonoDocumentService.generarPdf(req);
                        mediaType = "application/pdf";
                    }
                } else if ("proyector".equals(tipo)) {
                    if ("docx".equals(extension)) {
                        content = proyectorDocumentService.generarDocx(req);
                        mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    } else {
                        content = proyectorDocumentService.generarPdf(req);
                        mediaType = "application/pdf";
                    }
                } else if ("escaner".equals(tipo)) {
                    if ("docx".equals(extension)) {
                        content = escanerDocumentService.generarDocx(req);
                        mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    } else {
                        content = escanerDocumentService.generarPdf(req);
                        mediaType = "application/pdf";
                    }
                }
            }

            if (content == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Tipo o formato no compatible.").build();
            }

            return Response.ok(content, mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("rc/export/{format}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportarActaRc(@PathParam("format") String format, String json) {
        try {
            validarAutenticacion();
            ActaRedesComunicacionesRequest request = gson.fromJson(json, ActaRedesComunicacionesRequest.class);
            if (request == null || request.getFuncionario() == null || isBlank(request.getFuncionario().getNombre())) {
                ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", "El nombre del funcionario es obligatorio.");
                return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
            }

            String normalizedFormat = String.valueOf(format).trim().toLowerCase();
            byte[] content;
            String mediaType;
            String extension;
            if ("docx".equals(normalizedFormat)) {
                content = redesComunicacionesDocumentService.generarDocx(request);
                mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                extension = "docx";
            } else if ("pdf".equals(normalizedFormat)) {
                content = redesComunicacionesDocumentService.generarPdf(request);
                mediaType = "application/pdf";
                extension = "pdf";
            } else {
                ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", "Formato no soportado.");
                return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
            }

            String codigo = sanitizeSegment(request.getEquiposCisco() != null ? request.getEquiposCisco().getCodigo() : "redes");
            registrarActa("rc", codigo, request.getFuncionario().getNombre(), json);

            String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String fileName = "acta_redes_comunicaciones_" + codigo + "_" + fecha + "." + extension;
            return Response.ok(content, mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .build();
        } catch (IllegalArgumentException e) {
            ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<?> response = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(response)).build();
        }
    }

    @POST
    @Path("software/export/{format}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportarActaSoftware(@PathParam("format") String format, String json) {
        try {
            validarAutenticacion();
            ActaSoftwareRequest request = gson.fromJson(json, ActaSoftwareRequest.class);
            if (request == null || request.getFuncionario() == null || isBlank(request.getFuncionario().getNombre())) {
                ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", "El nombre del funcionario es obligatorio.");
                return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
            }
            String normalizedFormat = String.valueOf(format).trim().toLowerCase();
            byte[] content;
            String mediaType;
            String extension;
            if ("docx".equals(normalizedFormat)) {
                content = softwareDocumentService.generarDocx(request);
                mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                extension = "docx";
            } else if ("pdf".equals(normalizedFormat)) {
                content = softwareDocumentService.generarPdf(request);
                mediaType = "application/pdf";
                extension = "pdf";
            } else {
                ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", "Formato no soportado.");
                return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
            }

            String codigo = sanitizeSegment(request.getEquipo() != null ? request.getEquipo().getCodigo() : "equipo");
            registrarActa("software", codigo, request.getFuncionario().getNombre(), json);

            String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String fileName = "acta_software_" + codigo + "_" + fecha + "." + extension;
            return Response.ok(content, mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .build();
        } catch (IllegalArgumentException e) {
            ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<?> response = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(response)).build();
        }
    }

    @POST
    @Path("equipos/pc/export/{format}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportarActaPc(@PathParam("format") String format, String json) {
        try {
            validarAutenticacion();
            ActaMantenimientoPcRequest request = gson.fromJson(json, ActaMantenimientoPcRequest.class);
            validarRequest(request);

            String normalizedFormat = String.valueOf(format).trim().toLowerCase();
            byte[] content;
            String mediaType;
            String extension;
            if ("docx".equals(normalizedFormat)) {
                content = documentService.generarDocx(request);
                mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                extension = "docx";
            } else if ("pdf".equals(normalizedFormat)) {
                content = documentService.generarPdf(request);
                mediaType = "application/pdf";
                extension = "pdf";
            } else {
                ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", "Formato no soportado.");
                return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
            }

            String codigo = sanitizeSegment(request.getDesktop() != null ? request.getDesktop().getCodigo() : "equipo");
            registrarActa("pc", codigo, request.getFuncionario().getNombre(), json);

            String fileName = buildFileName(request, extension);
            return Response.ok(content, mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .build();
        } catch (IllegalArgumentException e) {
            ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<?> response = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(response)).build();
        }
    }

    @POST
    @Path("equipos/impresora/export/{format}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportarActaImpresora(@PathParam("format") String format, String json) {
        try {
            validarAutenticacion();
            ActaMantenimientoPcRequest request = gson.fromJson(json, ActaMantenimientoPcRequest.class);
            validarRequestImpresora(request);

            String normalizedFormat = String.valueOf(format).trim().toLowerCase();
            byte[] content;
            String mediaType;
            String extension;
            if ("docx".equals(normalizedFormat)) {
                content = impresoraDocumentService.generarDocx(request);
                mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                extension = "docx";
            } else if ("pdf".equals(normalizedFormat)) {
                content = impresoraDocumentService.generarPdf(request);
                mediaType = "application/pdf";
                extension = "pdf";
            } else {
                ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", "Formato no soportado.");
                return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
            }

            String codigo = sanitizeSegment(request.getDesktop() != null ? request.getDesktop().getCodigo() : "equipo");
            registrarActa("impresora", codigo, request.getFuncionario().getNombre(), json);

            String fileName = buildFileName(request, extension);
            return Response.ok(content, mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .build();
        } catch (IllegalArgumentException e) {
            ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<?> response = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(response)).build();
        }
    }

    @POST
    @Path("equipos/telefono/export/{format}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportarActaTelefono(@PathParam("format") String format, String json) {
        try {
            validarAutenticacion();
            ActaMantenimientoPcRequest request = gson.fromJson(json, ActaMantenimientoPcRequest.class);
            validarRequestTelefono(request);

            String normalizedFormat = String.valueOf(format).trim().toLowerCase();
            byte[] content;
            String mediaType;
            String extension;
            if ("docx".equals(normalizedFormat)) {
                content = telefonoDocumentService.generarDocx(request);
                mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                extension = "docx";
            } else if ("pdf".equals(normalizedFormat)) {
                content = telefonoDocumentService.generarPdf(request);
                mediaType = "application/pdf";
                extension = "pdf";
            } else {
                ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", "Formato no soportado.");
                return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
            }

            String codigo = sanitizeSegment(request.getDesktop() != null ? request.getDesktop().getCodigo() : "equipo");
            registrarActa("telefono", codigo, request.getFuncionario().getNombre(), json);

            String fileName = buildFileName(request, extension);
            return Response.ok(content, mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .build();
        } catch (IllegalArgumentException e) {
            ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<?> response = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(response)).build();
        }
    }

    private void validarRequestTelefono(ActaMantenimientoPcRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("No se recibieron datos del acta.");
        }
        if (request.getFuncionario() == null || isBlank(request.getFuncionario().getNombre())) {
            throw new IllegalArgumentException("El nombre del funcionario es obligatorio.");
        }
        if (request.getDesktop() == null || isBlank(request.getDesktop().getCodigo())) {
            throw new IllegalArgumentException("Debe seleccionar un teléfono IP válido.");
        }
        if (request.getActividades() == null || request.getActividades().isEmpty()) {
            throw new IllegalArgumentException("Las actividades del acta son obligatorias.");
        }
    }

    private void validarAutenticacion() throws Exception {
        Usuario usuario = SesionUsuario.getUsuarioActual(servletRequest);
        if (usuario == null) {
            throw new Exception("Usuario no autenticado");
        }
    }

    private void validarRequest(ActaMantenimientoPcRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("No se recibieron datos del acta.");
        }
        if (request.getFuncionario() == null || isBlank(request.getFuncionario().getNombre())) {
            throw new IllegalArgumentException("El nombre del funcionario es obligatorio.");
        }
        if (request.getDesktop() == null || isBlank(request.getDesktop().getCodigo())) {
            throw new IllegalArgumentException("Debe seleccionar un equipo valido.");
        }
        if (request.getActividades() == null || request.getActividades().isEmpty()) {
            throw new IllegalArgumentException("Las actividades del acta son obligatorias.");
        }
    }

    @POST
    @Path("equipos/proyector/export/{format}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportarActaProyector(@PathParam("format") String format, String json) {
        try {
            validarAutenticacion();
            ActaMantenimientoPcRequest request = gson.fromJson(json, ActaMantenimientoPcRequest.class);
            validarRequestProyector(request);

            String normalizedFormat = String.valueOf(format).trim().toLowerCase();
            byte[] content;
            String mediaType;
            String extension;
            if ("docx".equals(normalizedFormat)) {
                content = proyectorDocumentService.generarDocx(request);
                mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                extension = "docx";
            } else if ("pdf".equals(normalizedFormat)) {
                content = proyectorDocumentService.generarPdf(request);
                mediaType = "application/pdf";
                extension = "pdf";
            } else {
                ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", "Formato no soportado.");
                return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
            }

            String codigo = sanitizeSegment(request.getDesktop() != null ? request.getDesktop().getCodigo() : "equipo");
            registrarActa("proyector", codigo, request.getFuncionario().getNombre(), json);

            String fileName = buildFileName(request, extension);
            return Response.ok(content, mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .build();
        } catch (IllegalArgumentException e) {
            ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<?> response = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(response)).build();
        }
    }

    @POST
    @Path("equipos/escaner/export/{format}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportarActaEscaner(@PathParam("format") String format, String json) {
        try {
            validarAutenticacion();
            ActaMantenimientoPcRequest request = gson.fromJson(json, ActaMantenimientoPcRequest.class);
            validarRequestEscaner(request);

            String normalizedFormat = String.valueOf(format).trim().toLowerCase();
            byte[] content;
            String mediaType;
            String extension;
            if ("docx".equals(normalizedFormat)) {
                content = escanerDocumentService.generarDocx(request);
                mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                extension = "docx";
            } else if ("pdf".equals(normalizedFormat)) {
                content = escanerDocumentService.generarPdf(request);
                mediaType = "application/pdf";
                extension = "pdf";
            } else {
                ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", "Formato no soportado.");
                return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
            }

            String codigo = sanitizeSegment(request.getDesktop() != null ? request.getDesktop().getCodigo() : "equipo");
            registrarActa("escaner", codigo, request.getFuncionario().getNombre(), json);

            String fileName = buildFileName(request, extension);
            return Response.ok(content, mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .build();
        } catch (IllegalArgumentException e) {
            ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<?> response = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(response)).build();
        }
    }

    private void registrarActa(String tipoActa, String codigoEquipo, String nombreCustodio, String jsonPayload) {
        try {
            Usuario usuario = SesionUsuario.getUsuarioActual(servletRequest);
            String creadoPor = usuario != null ? usuario.getUsuario() : "SISTEMA";
            Integer idCustodio = lookupIdCustodio(nombreCustodio);

            ActaMantenimiento acta = new ActaMantenimiento();
            acta.setTipoActa(tipoActa);
            acta.setCodigoEquipo(codigoEquipo);
            acta.setNombreCustodio(nombreCustodio);
            acta.setIdCustodio(idCustodio);
            acta.setCreadoPor(creadoPor);
            acta.setJsonPayload(jsonPayload);

            boolean guardado = actaMantenimientoService.guardarActa(acta);
            System.out.println("[ActasResource] Acta auto-guardada: " + guardado + " | Tipo: " + tipoActa + " | Custodio: " + nombreCustodio);
        } catch (Exception e) {
            System.err.println("[ActasResource] Error al registrar acta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Integer lookupIdCustodio(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT id_custodio FROM custodio WHERE UPPER(nombre) = ? LIMIT 1";
        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_custodio");
                }
            }
        } catch (Exception e) {
            System.err.println("[ActasResource] Error looking up id_custodio: " + e.getMessage());
        }
        return null;
    }

    private void validarRequestEscaner(ActaMantenimientoPcRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("No se recibieron datos del acta.");
        }
        if (request.getFuncionario() == null || isBlank(request.getFuncionario().getNombre())) {
            throw new IllegalArgumentException("El nombre del funcionario es obligatorio.");
        }
        if (request.getDesktop() == null || isBlank(request.getDesktop().getCodigo())) {
            throw new IllegalArgumentException("Debe seleccionar un escáner válido.");
        }
        if (request.getActividades() == null || request.getActividades().isEmpty()) {
            throw new IllegalArgumentException("Las actividades del acta son obligatorias.");
        }
    }

    private void validarRequestProyector(ActaMantenimientoPcRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("No se recibieron datos del acta.");
        }
        if (request.getFuncionario() == null || isBlank(request.getFuncionario().getNombre())) {
            throw new IllegalArgumentException("El nombre del funcionario es obligatorio.");
        }
        if (request.getDesktop() == null || isBlank(request.getDesktop().getCodigo())) {
            throw new IllegalArgumentException("Debe seleccionar un proyector válido.");
        }
        if (request.getActividades() == null || request.getActividades().isEmpty()) {
            throw new IllegalArgumentException("Las actividades del acta son obligatorias.");
        }
    }

    private void validarRequestImpresora(ActaMantenimientoPcRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("No se recibieron datos del acta.");
        }
        if (request.getFuncionario() == null || isBlank(request.getFuncionario().getNombre())) {
            throw new IllegalArgumentException("El nombre del funcionario es obligatorio.");
        }
        if (request.getDesktop() == null || isBlank(request.getDesktop().getCodigo())) {
            throw new IllegalArgumentException("Debe seleccionar una impresora válida.");
        }
        if (request.getActividades() == null || request.getActividades().isEmpty()) {
            throw new IllegalArgumentException("Las actividades del acta son obligatorias.");
        }
    }

    private String buildFileName(ActaMantenimientoPcRequest request, String extension) {
        String codigo = sanitizeSegment(request.getDesktop() != null ? request.getDesktop().getCodigo() : "equipo");
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        String tipo = sanitizeSegment(request.getSubapartado() != null ? request.getSubapartado() : "equipo");
        return "acta_mantenimiento_" + tipo + "_" + codigo + "_" + fecha + "." + extension;
    }

    private String sanitizeSegment(String value) {
        String normalized = isBlank(value) ? "equipo" : value.trim();
        return normalized.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // ══════════════════════════ ACTA DE LICENCIAS ═══════════════════════════════
    // PDF del Acta de Licencias generado en el servidor (texto real), con la misma
    // calidad que las demás actas. La licencia llega en "desktop". No registra el
    // acta en la base de datos (igual que la exportación anterior de licencias).
    @POST
    @Path("licencias/export/pdf")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response exportarActaLicenciasPdf(String json) {
        try {
            validarAutenticacion();
            ActaMantenimientoPcRequest request = gson.fromJson(json, ActaMantenimientoPcRequest.class);
            if (request == null) {
                throw new IllegalArgumentException("No se recibieron datos del acta.");
            }
            if (request.getFuncionario() == null || isBlank(request.getFuncionario().getNombre())) {
                throw new IllegalArgumentException("El nombre del funcionario es obligatorio.");
            }
            if (request.getDesktop() == null || isBlank(request.getDesktop().getCodigo())) {
                throw new IllegalArgumentException("Debe seleccionar una licencia válida.");
            }

            byte[] content = licenciasDocumentService.generarPdf(request);
            String fileName = "acta_licencia_" + sanitizeSegment(request.getDesktop().getCodigo()) + ".pdf";
            return Response.ok(content, "application/pdf")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .build();
        } catch (IllegalArgumentException e) {
            ApiResponse<?> response = ApiResponse.error("VALIDATION_ERROR", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(response)).build();
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse<?> response = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(response)).build();
        }
    }
}
