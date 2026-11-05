package com.mycompany.sistemainventariov3.resources;

import com.google.gson.Gson;
import com.mycompany.sistemainventariov3.dto.ApiResponse;
import com.mycompany.sistemainventariov3.model.HistoricoCambio;
import com.mycompany.sistemainventariov3.service.HistoricoService;
import com.mycompany.sistemainventariov3.util.SesionUsuario;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Controlador REST para consulta del histórico de cambios
 */
@Path("historico")
public class HistoricoResource {
    
    private HistoricoService historicoService = new HistoricoService();
    private Gson gson = new Gson();
    
    /**
     * Obtener histórico de cambios de un bien específico
     * GET /resources/historico/bien/pcs/{idBien}
     */
    @GET
    @Path("bien/{tabla}/{idBien}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerHistoricoBien(
            @PathParam("tabla") String tabla,
            @PathParam("idBien") String idBien) {
        try {
            validarAutenticacion();
            
            List<HistoricoCambio> historico = historicoService.obtenerHistoricoBien(tabla, idBien);
            
            ApiResponse<List<HistoricoCambio>> response = 
                ApiResponse.success("Histórico del bien obtenido", historico);
            return Response.ok(gson.toJson(response)).build();
        } catch (Exception e) {
            ApiResponse<?> resp = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(resp)).build();
        }
    }
    
    /**
     * Obtener histórico de cambios hechos por un usuario
     * GET /resources/historico/usuario/{username}
     */
    @GET
    @Path("usuario/{usuario}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerHistoricoUsuario(@PathParam("usuario") String usuario) {
        try {
            validarAutenticacion();
            validarAdministrador();
            
            List<HistoricoCambio> historico = historicoService.obtenerHistoricoUsuario(usuario);
            
            ApiResponse<List<HistoricoCambio>> response = 
                ApiResponse.success("Histórico del usuario obtenido", historico);
            return Response.ok(gson.toJson(response)).build();
        } catch (Exception e) {
            ApiResponse<?> resp = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(resp)).build();
        }
    }
    
    /**
     * Obtener histórico completo de una tabla
     * GET /resources/historico/tabla/{tabla}
     */
    @GET
    @Path("tabla/{tabla}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerHistoricoTabla(@PathParam("tabla") String tabla) {
        try {
            validarAutenticacion();
            validarAdministrador();
            
            List<HistoricoCambio> historico = historicoService.obtenerHistoricoTabla(tabla);
            
            ApiResponse<List<HistoricoCambio>> response = 
                ApiResponse.success("Histórico de la tabla obtenido", historico);
            return Response.ok(gson.toJson(response)).build();
        } catch (Exception e) {
            ApiResponse<?> resp = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(resp)).build();
        }
    }
    
    /**
     * Obtener cambios de un campo específico
     * GET /resources/historico/campo/pcs/{idBien}/{campo}
     */
    @GET
    @Path("campo/{tabla}/{idBien}/{campo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerCambiosCampo(
            @PathParam("tabla") String tabla,
            @PathParam("idBien") String idBien,
            @PathParam("campo") String campo) {
        try {
            validarAutenticacion();
            
            List<HistoricoCambio> historico = historicoService.obtenerCambiosCampo(tabla, idBien, campo);
            
            ApiResponse<List<HistoricoCambio>> response = 
                ApiResponse.success("Histórico de cambios del campo obtenido", historico);
            return Response.ok(gson.toJson(response)).build();
        } catch (Exception e) {
            ApiResponse<?> resp = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(resp)).build();
        }
    }
    
    // ==================== VALIDACIONES ====================
    
    private void validarAutenticacion() throws Exception {
        if (!SesionUsuario.estaAutenticado()) {
            throw new Exception("Usuario no autenticado");
        }
    }
    
    private void validarAdministrador() throws Exception {
        if (!SesionUsuario.esAdministrador()) {
            throw new Exception("Permisos insuficientes. Se requiere rol de Administrador");
        }
    }
}
