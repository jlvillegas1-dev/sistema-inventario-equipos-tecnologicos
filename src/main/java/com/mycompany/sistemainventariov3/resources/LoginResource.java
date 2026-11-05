package com.mycompany.sistemainventariov3.resources;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mycompany.sistemainventariov3.dto.LoginRequest;
import com.mycompany.sistemainventariov3.model.Usuario;
import com.mycompany.sistemainventariov3.service.UsuarioService;
import com.mycompany.sistemainventariov3.service.LDAPAuthService;
import com.mycompany.sistemainventariov3.util.SesionUsuario;
import com.mycompany.sistemainventariov3.util.LDAPConfig;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Controlador REST para autenticacion de usuarios.
 * Integra autenticación contra LDAP (SC_Inventario) y base de datos local.
 */
@Path("login")
public class LoginResource {

    @Context
    private HttpServletRequest request;

    private final UsuarioService usuarioService;
    private final LDAPAuthService ldapAuthService;
    private final Gson gson = new Gson();
    
    // Flag para usar LDAP como método principal (true) o como fallback (false)
    private static final boolean USAR_LDAP_PRINCIPAL = true;

    public LoginResource() {
        this.usuarioService = new UsuarioService();
        this.ldapAuthService = new LDAPAuthService();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // Endpoint para autenticar usuarios. Retorna información del usuario y si es necesario seleccionar un perfil.
    // Integra autenticación LDAP (SC_Inventario) como método principal o fallback
    public Response autenticar(String json) {
        try {
            LoginRequest loginRequest = gson.fromJson(json, LoginRequest.class);
            Usuario usuario = null;
            String metodoAutenticacion = "BD LOCAL";

            System.out.println("\n═══════════════════════════════════════════════════════════");
            System.out.println("  INTENTO DE AUTENTICACIÓN - Usuario: " + loginRequest.getUsername());
            System.out.println("═══════════════════════════════════════════════════════════");
            
            // Imprimir configuración LDAP
            LDAPConfig.printConfig();

            // Intentar autenticación contra LDAP primero
            if (USAR_LDAP_PRINCIPAL) {
                System.out.println("→ Intentando autenticación LDAP...");
                try {
                    usuario = ldapAuthService.autenticarLDAP(loginRequest.getUsername(), loginRequest.getPassword());
                    metodoAutenticacion = "LDAP (SC_Inventario)";
                    System.out.println("✓ Autenticación exitosa via LDAP para: " + loginRequest.getUsername());
                } catch (Exception ldapEx) {
                    // Si LDAP falla, intentar BD local
                    System.out.println("⚠ Autenticación LDAP fallida: " + ldapEx.getMessage());
                    System.out.println("→ Intentando autenticación en BD local...");
                    usuario = usuarioService.autenticar(loginRequest.getUsername(), loginRequest.getPassword(), loginRequest.getRolElegido());
                    metodoAutenticacion = "BD LOCAL";
                }
            } else {
                // Usar BD local como principal
                System.out.println("→ Intentando autenticación en BD local (configuración)...");
                usuario = usuarioService.autenticar(loginRequest.getUsername(), loginRequest.getPassword(), loginRequest.getRolElegido());
            }

            boolean rolSeleccionado = loginRequest.getRolElegido() != null && !loginRequest.getRolElegido().trim().isEmpty();
            if (rolSeleccionado && usuario.getRolesDisponibles() != null) {
                String rolElegido = loginRequest.getRolElegido().trim().toUpperCase();
                if (usuario.getRolesDisponibles().contains(rolElegido)) {
                    usuario.setRol(rolElegido);
                }
            }
            boolean requiereSeleccionPerfil = !rolSeleccionado
                    && usuario.getRolesDisponibles() != null
                    && usuario.getRolesDisponibles().size() > 1;
            if (!requiereSeleccionPerfil && "CUSTODIO".equals(usuario.getRol()) && usuario.getIdCustodio() == null) {
                throw new Exception("Usuario LDAP autenticado, pero no esta enlazado a un custodio local. "
                        + "Revise la tabla usuario/custodio para asociar el usuario AD con id_custodio.");
            }
            if (!requiereSeleccionPerfil) {
                SesionUsuario.setUsuarioActual(request, usuario);
            }

            System.out.println("═══════════════════════════════════════════════════════════\n");

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Autenticacion exitosa (" + metodoAutenticacion + ")");
            response.addProperty("requiereSeleccionPerfil", requiereSeleccionPerfil);
            response.add("data", construirUsuarioResponse(usuario));
            return Response.ok(response.toString()).build();
        } catch (Exception e) {
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println(" ERROR DE AUTENTICACIÓN: " + (e.getMessage() != null ? e.getMessage() : "Error desconocido"));
            // e.printStackTrace();
            System.out.println("═══════════════════════════════════════════════════════════\n");
            
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("success", false);
            errorResponse.addProperty("error", "AUTH_ERROR");
            errorResponse.addProperty("message", e.getMessage() != null ? e.getMessage() : "Error desconocido");
            return Response.status(Response.Status.UNAUTHORIZED).entity(errorResponse.toString()).build();
        }
    }

    @GET
    @Path("actual")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerUsuarioActual() {
        try {
            Usuario usuario = SesionUsuario.getUsuarioActual(request);
            if (usuario == null) {
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("success", false);
                errorResponse.addProperty("error", "NOT_AUTHENTICATED");
                errorResponse.addProperty("message", "No hay usuario autenticado");
                return Response.status(Response.Status.UNAUTHORIZED).entity(errorResponse.toString()).build();
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Usuario actual obtenido");
            response.add("data", construirUsuarioResponse(usuario));
            return Response.ok(response.toString()).build();
        } catch (Exception e) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("success", false);
            errorResponse.addProperty("error", "ERROR");
            errorResponse.addProperty("message", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse.toString()).build();
        }
    }

    @POST
    @Path("logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout() {
        try {
            SesionUsuario.limpiar(request);
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Sesion cerrada exitosamente");
            return Response.ok(response.toString()).build();
        } catch (Exception e) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("success", false);
            errorResponse.addProperty("error", "ERROR");
            errorResponse.addProperty("message", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse.toString()).build();
        }
    }

    private JsonObject construirUsuarioResponse(Usuario usuario) {
        String rol = usuario.getRol() == null ? "" : usuario.getRol().toUpperCase();

        JsonObject usuarioResponse = new JsonObject();
        usuarioResponse.addProperty("usuario", usuario.getUsuario());
        usuarioResponse.addProperty("rol", rol);
        usuarioResponse.addProperty("nombreCompleto",
                usuario.getNombreCompleto() != null ? usuario.getNombreCompleto() : usuario.getUsuario());
        if (usuario.getIdCustodio() != null) {
            usuarioResponse.addProperty("idCustodio", usuario.getIdCustodio());
        }
        // Retornar lista de roles disponibles para el selector en el frontend
        JsonArray rolesArray = new JsonArray();
        if (usuario.getRolesDisponibles() != null) {
            for (String r : usuario.getRolesDisponibles()) {
                rolesArray.add(r);
            }
        } else {
            rolesArray.add(rol);
        }
        usuarioResponse.add("rolesDisponibles", rolesArray);

        JsonObject permisos = new JsonObject();
        if ("ADMINISTRADOR".equals(rol)) {
            permisos.addProperty("puedeEditarTodos", true);
            permisos.addProperty("puedeActualizarEstado", true);
            permisos.addProperty("puedeVer", true);
            permisos.addProperty("puedeCrearEquipo", true);
            permisos.addProperty("puedeEditarCustodio", true);
            permisos.addProperty("puedeExportarInventario", true);
            permisos.addProperty("puedeVerHistorial", true);
        } else if ("TECNICO".equals(rol)) {
            permisos.addProperty("puedeEditarTodos", false);
            permisos.addProperty("puedeActualizarEstado", false);
            permisos.addProperty("puedeVer", true);
            permisos.addProperty("puedeCrearEquipo", true);
            permisos.addProperty("puedeEditarCustodio", true);
            permisos.addProperty("puedeExportarInventario", true);
            permisos.addProperty("puedeVerHistorial", true);
        } else if ("CUSTODIO".equals(rol)) {
            permisos.addProperty("puedeEditarTodos", false);
            permisos.addProperty("puedeActualizarEstado", false);
            permisos.addProperty("puedeVer", true);
            permisos.addProperty("puedeCrearEquipo", false);
            permisos.addProperty("puedeEditarCustodio", false);
            permisos.addProperty("puedeExportarInventario", false);
            permisos.addProperty("puedeVerHistorial", true);
        } else {
            permisos.addProperty("puedeVer", false);
        }
        usuarioResponse.add("permisos", permisos);
        return usuarioResponse;
    }
}
