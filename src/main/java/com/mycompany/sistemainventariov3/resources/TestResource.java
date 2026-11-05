package com.mycompany.sistemainventariov3.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Endpoint de prueba para verificar que JAX-RS funciona
 */
@Path("test")
public class TestResource {
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response test() {
        return Response.ok("{\"status\":\"ok\",\"message\":\"API funcionando correctamente\"}").build();
    }
}
