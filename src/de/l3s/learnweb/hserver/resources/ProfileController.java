package de.l3s.learnweb.hserver.resources;

import java.sql.SQLException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.hserver.HUtils;
import de.l3s.learnweb.user.User;

@Path("api/profile")
public class ProfileController {

    @Context
    private SecurityContext context;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject get() throws SQLException {
        User user = (User) context.getUserPrincipal();

        JsonObjectBuilder profile = Json.createObjectBuilder();
        if (user == null) {
            profile.add("userid", JsonValue.NULL);
            profile.add("authority", "learnweb.l3s.uni-hannover.de");
            JsonArrayBuilder groups = Json.createArrayBuilder();
            JsonObjectBuilder groupObj = Json.createObjectBuilder();
            groupObj.add("name", "Public");
            groupObj.add("id", "0");
            groupObj.add("public", true);
            groups.add(groupObj);
            profile.add("groups", groups);
        } else {
            profile.add("userid", HUtils.getUserForUserId(user.getId()));
            profile.add("authority", "learnweb.l3s.uni-hannover.de");

            JsonArrayBuilder groups = Json.createArrayBuilder();
            for (Group group : user.getGroups()) {
                JsonObjectBuilder groupObj = Json.createObjectBuilder();
                groupObj.add("id", group.getId());
                groupObj.add("name", group.getTitle());
                groupObj.add("public", true);
                groupObj.add("url", Learnweb.getInstance().getServerUrl() + "/lw/group/overview.jsf?group_id=" + group.getId());
                groups.add(groupObj);
            }
            profile.add("groups", groups);

            JsonObjectBuilder userInfo = Json.createObjectBuilder();
            userInfo.add("user_id", user.getId());
            userInfo.add("display_name", user.getFullName());
            profile.add("user_info", userInfo);
        }

        JsonObjectBuilder features = Json.createObjectBuilder();
        features.add("client_filter_status", false);
        features.add("embed_cachebuster", false);
        features.add("client_display_names", false);
        profile.add("features", features);

        JsonObjectBuilder preferences = Json.createObjectBuilder();
        profile.add("preferences", preferences);

        return profile.build();
    }

    @GET
    @Path("groups")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonArray geGroups() throws SQLException {
        User user = (User) context.getUserPrincipal();

        JsonArrayBuilder groups = Json.createArrayBuilder();

        if (user != null) {
            JsonObjectBuilder publicGroup = Json.createObjectBuilder();
            publicGroup.add("id", "0");
            publicGroup.add("groupid", 0);
            publicGroup.add("name", "Public");
            JsonObjectBuilder publicGroupLinks = Json.createObjectBuilder();
            publicGroupLinks.add("html", Learnweb.getInstance().getServerUrl() + "/lw/group/overview.jsf?group_id=0");
            publicGroup.add("links", publicGroupLinks);
            publicGroup.add("organization", JsonValue.NULL);
            publicGroup.add("public", true);
            publicGroup.add("scoped", false);
            publicGroup.add("type", "open");
            groups.add(publicGroup);

            for (Group group : user.getGroups()) {
                JsonObjectBuilder groupObj = Json.createObjectBuilder();
                groupObj.add("id", group.getId());
                groupObj.add("groupid", group.getId());
                groupObj.add("name", group.getTitle());
                JsonObjectBuilder links = Json.createObjectBuilder();
                links.add("html", Learnweb.getInstance().getServerUrl() + "/lw/group/overview.jsf?group_id=" + group.getId());
                groupObj.add("links", links);
                groupObj.add("organization", JsonValue.NULL);
                groupObj.add("type", "restricted"); // TODO:  Enum: "private" "open" "restricted"
                groups.add(groupObj);
            }
        }

        return groups.build();
    }
}
