package de.l3s.learnweb.hserver.resources;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.User;

@Path("api/groups")
public class GroupsController {

    @Context
    private SecurityContext context;

    /**
     * Retrieve a list of applicable Groups, filtered by authority and target document (document_uri). Also retrieve user's private Groups.
     *  @param authority Filter returned groups to this authority. For authenticated requests, the user's associated authority will supersede any provided value.
     * @param documentUri Only retrieve public (i.e. non-private) groups that apply to a given document URI (i.e. the target document being annotated).
     * @param expand One or more relations to expand for a group resource. Items Enum: "organization", "scopes"
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonArray getGroups(@DefaultValue("learnweb.l3s.uni-hannover.de") @QueryParam("authority") final String authority,
                            @QueryParam("document_uri") final String documentUri,
                            @QueryParam("expand") final List<String> expand) {

        User user = (User) context.getUserPrincipal();

        JsonArrayBuilder groups = Json.createArrayBuilder();

        if (StringUtils.isNotEmpty(documentUri)) {
            JsonObjectBuilder groupObj = Json.createObjectBuilder();
            groupObj.add("id", "0");
            groupObj.add("groupid", 0);
            groupObj.add("name", "Public");
            JsonObjectBuilder links = Json.createObjectBuilder();
            links.add("html", Learnweb.getInstance().getServerUrl() + "/lw/myhome/welcome.jsf");
            groupObj.add("links", links);
            groupObj.add("organization", JsonValue.NULL);
            groupObj.add("public", true);
            groupObj.add("scoped", false);
            groupObj.add("type", "open"); // TODO:  Enum: "private" "open" "restricted"
            JsonObjectBuilder scopes = Json.createObjectBuilder();
            scopes.add("enforced", false);
            scopes.add("uri_patterns", JsonValue.EMPTY_JSON_ARRAY);
            groupObj.add("scopes", scopes);
            groups.add(groupObj);
        }

        return groups.build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String createGroup() {
        throw new NotImplementedException();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getGroup(@PathParam("id") Integer id, @QueryParam("expand") final List<String> expand) {
        throw new NotImplementedException();
    }

    @PATCH
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String updateGroup(@PathParam("id") Integer id) {
        throw new NotImplementedException();
    }

    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String createOrUpdateGroup(@PathParam("id") Integer id) {
        throw new NotImplementedException();
    }

    @GET
    @Path("{id}/members")
    @Produces(MediaType.APPLICATION_JSON)
    public String getGroupMembers(@PathParam("id") Integer id) {
        throw new NotImplementedException();
    }

    @POST
    @Path("{id}/members/{user}")
    @Produces(MediaType.APPLICATION_JSON)
    public String addMember(@PathParam("id") Integer id, @PathParam("user") String user) {
        throw new NotImplementedException();
    }

    @DELETE
    @Path("{id}/members/{user}")
    @Produces(MediaType.APPLICATION_JSON)
    public String removeMember(@PathParam("id") Integer id, @PathParam("user") String user) {
        throw new NotImplementedException();
    }
}
