package de.l3s.learnweb.hserver.resources;

import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.NotImplementedException;

@Path("api/users")
public class UsersController {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String create() {
        throw new NotImplementedException();
    }

    @GET
    @Path("{user}")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@PathParam("user") String user) {
        throw new NotImplementedException();
    }

    @PATCH
    @Path("{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public String update(@PathParam("username") String username) {
        throw new NotImplementedException();
    }
}
