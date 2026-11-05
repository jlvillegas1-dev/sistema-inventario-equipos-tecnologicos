package com.mycompany.sistemainventariov3.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import com.mycompany.sistemainventariov3.service.DatabaseService;
import com.google.gson.Gson;

/**
 * Recurso REST para acceder a los datos de la base de datos
 */
@Path("api")
@Produces("application/json")
public class DatabaseResource {
    
    private Gson gson = new Gson();
    
    @GET
    @Path("status")
    public Response getStatus() {
        return Response
                .ok(gson.toJson(DatabaseService.getDatabaseStatus()))
                .build();
    }
    
    @GET
    @Path("custodios")
    public Response getCustodios() {
        return Response
                .ok(gson.toJson(DatabaseService.getCustodios()))
                .build();
    }
    
    @GET
    @Path("pcs")
    public Response getPCs() {
        return Response
                .ok(gson.toJson(DatabaseService.getPCs()))
                .build();
    }
    
    @GET
    @Path("laptops")
    public Response getLaptops() {
        return Response
                .ok(gson.toJson(DatabaseService.getLaptops()))
                .build();
    }
}
