package com.mycompany.sistemainventariov3.resources;

import com.google.gson.Gson;
import com.mycompany.sistemainventariov3.dto.ApiResponse;
import com.mycompany.sistemainventariov3.model.Laptop;
import com.mycompany.sistemainventariov3.model.PC;
import com.mycompany.sistemainventariov3.service.BienService;
import com.mycompany.sistemainventariov3.util.SesionUsuario;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Controlador REST para búsquedas avanzadas de bienes
 */
@Path("busqueda")
public class BusquedaResource {
    
    private BienService bienService = new BienService();
    private Gson gson = new Gson();
    
    /**
     * Búsqueda general de bienes
     * GET /resources/busqueda?tipo=pcs&criterio=marca&valor=Dell
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response buscar(
            @QueryParam("tipo") String tipo,
            @QueryParam("criterio") String criterio,
            @QueryParam("valor") String valor) {
        try {
            validarAutenticacion();
            
            if (tipo == null || criterio == null || valor == null) {
                ApiResponse<?> resp = ApiResponse.error("INVALID_PARAMS", 
                    "Se requieren parámetros: tipo, criterio, valor");
                return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(resp)).build();
            }
            
            List<?> resultados = new ArrayList<>();
            
            if ("pcs".equals(tipo) || "pc".equals(tipo)) {
                if ("marca".equals(criterio)) {
                    resultados = bienService.buscarPCsPorMarca(valor);
                } else if ("estado".equals(criterio)) {
                    resultados = bienService.buscarPCsPorEstado(valor);
                }
            } else if ("laptops".equals(tipo) || "laptop".equals(tipo)) {
                if ("marca".equals(criterio)) {
                    resultados = bienService.buscarLaptopsPorMarca(valor);
                } else if ("estado".equals(criterio)) {
                    resultados = bienService.buscarLaptopsPorEstado(valor);
                }
            } else {
                ApiResponse<?> resp = ApiResponse.error("INVALID_TYPE", "Tipo de bien no válido");
                return Response.status(Response.Status.BAD_REQUEST).entity(gson.toJson(resp)).build();
            }
            
            ApiResponse<?> response = ApiResponse.success("Búsqueda completada", resultados);
            return Response.ok(gson.toJson(response)).build();
        } catch (Exception e) {
            ApiResponse<?> resp = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(resp)).build();
        }
    }
    
    /**
     * Búsqueda por Código MEGAN
     * GET /resources/busqueda/megan/{codigoMegan}
     */
    @GET
    @Path("megan/{codigoMegan}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buscarPorCodigoMegan(@PathParam("codigoMegan") String codigoMegan) {
        try {
            validarAutenticacion();
            
            List<Object> resultados = new ArrayList<>();
            
            PC pc = new com.mycompany.sistemainventariov3.dao.PCDAO().buscarPorCodigoMegan(codigoMegan);
            if (pc != null) resultados.add(pc);
            
            Laptop laptop = new com.mycompany.sistemainventariov3.dao.LaptopDAO().buscarPorCodigoMegan(codigoMegan);
            if (laptop != null) resultados.add(laptop);
            
            ApiResponse<?> response = ApiResponse.success("Búsqueda por Código MEGAN completada", resultados);
            return Response.ok(gson.toJson(response)).build();
        } catch (Exception e) {
            ApiResponse<?> resp = ApiResponse.error("ERROR", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(gson.toJson(resp)).build();
        }
    }
    
    /**
     * Búsqueda por Código SBAI
     * GET /resources/busqueda/sbai/{codigoSbai}
     */
    @GET
    @Path("sbai/{codigoSbai}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buscarPorCodigoSbai(@PathParam("codigoSbai") String codigoSbai) {
        try {
            validarAutenticacion();
            
            List<Object> resultados = new ArrayList<>();
            
            PC pc = new com.mycompany.sistemainventariov3.dao.PCDAO().buscarPorCodigoSbai(codigoSbai);
            if (pc != null) resultados.add(pc);
            
            Laptop laptop = new com.mycompany.sistemainventariov3.dao.LaptopDAO().buscarPorCodigoSbai(codigoSbai);
            if (laptop != null) resultados.add(laptop);
            
            if (resultados.isEmpty()) {
                ApiResponse<?> resp = ApiResponse.error("NOT_FOUND", "No se encontraron resultados");
                return Response.status(Response.Status.NOT_FOUND).entity(gson.toJson(resp)).build();
            }
            
            ApiResponse<?> response = ApiResponse.success("Búsqueda por Código SBAI completada", resultados);
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
}
