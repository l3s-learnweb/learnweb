package de.l3s.learnweb.hserver.resources;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.l3s.learnweb.Learnweb;

@Path("api")
public class ApiController {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getIt() {
        String serverUrl = Learnweb.getInstance().getServerUrl() + "/annotations";

        JsonObjectBuilder response = Json.createObjectBuilder();
        JsonObjectBuilder links = Json.createObjectBuilder();

        JsonObjectBuilder linksAnnotation = Json.createObjectBuilder();
        JsonObjectBuilder linksAnnotationCreate = Json.createObjectBuilder();
        linksAnnotationCreate.add("method", "POST");
        linksAnnotationCreate.add("url", serverUrl + "/api/annotations");
        linksAnnotationCreate.add("desc", "Create an annotation");
        linksAnnotation.add("create", linksAnnotationCreate);
        JsonObjectBuilder linksAnnotationDelete = Json.createObjectBuilder();
        linksAnnotationDelete.add("method", "DELETE");
        linksAnnotationDelete.add("url", serverUrl + "/api/annotations/:id");
        linksAnnotationDelete.add("desc", "Delete an annotation");
        linksAnnotation.add("delete", linksAnnotationDelete);
        JsonObjectBuilder linksAnnotationRead = Json.createObjectBuilder();
        linksAnnotationRead.add("method", "GET");
        linksAnnotationRead.add("url", serverUrl + "/api/annotations/:id");
        linksAnnotationRead.add("desc", "Fetch an annotation");
        linksAnnotation.add("read", linksAnnotationRead);
        JsonObjectBuilder linksAnnotationUpdate = Json.createObjectBuilder();
        linksAnnotationUpdate.add("method", "PATCH");
        linksAnnotationUpdate.add("url", serverUrl + "/api/annotations/:id");
        linksAnnotationUpdate.add("desc", "Update an annotation");
        linksAnnotation.add("update", linksAnnotationUpdate);
        JsonObjectBuilder linksAnnotationFlag = Json.createObjectBuilder();
        linksAnnotationFlag.add("method", "PUT");
        linksAnnotationFlag.add("url", serverUrl + "/api/annotations/:id/flag");
        linksAnnotationFlag.add("desc", "Flag an annotation for review");
        linksAnnotation.add("flag", linksAnnotationFlag);
        JsonObjectBuilder linksAnnotationHide = Json.createObjectBuilder();
        linksAnnotationHide.add("method", "PUT");
        linksAnnotationHide.add("url", serverUrl + "/api/annotations/:id/hide");
        linksAnnotationHide.add("desc", "Hide an annotation as a group moderator");
        linksAnnotation.add("hide", linksAnnotationHide);
        JsonObjectBuilder linksAnnotationShow = Json.createObjectBuilder();
        linksAnnotationShow.add("method", "DELETE");
        linksAnnotationShow.add("url", serverUrl + "/api/annotations/:id/hide");
        linksAnnotationShow.add("desc", "Unhide an annotation as a group moderator");
        linksAnnotation.add("unhide", linksAnnotationShow);
        links.add("annotation", linksAnnotation);

        JsonObjectBuilder linksSearch = Json.createObjectBuilder();
        linksSearch.add("method", "GET");
        linksSearch.add("url", serverUrl + "/api/search");
        linksSearch.add("desc", "Search for annotations");
        links.add("search", linksSearch);

        JsonObjectBuilder linksBulk = Json.createObjectBuilder();
        linksBulk.add("method", "POST");
        linksBulk.add("url", serverUrl + "/api/bulk");
        linksBulk.add("desc", "Perform multiple operations in one call");
        links.add("bulk", linksBulk);

        JsonObjectBuilder linksGroup = Json.createObjectBuilder();
        JsonObjectBuilder linksGroupMember = Json.createObjectBuilder();
        JsonObjectBuilder linksGroupMemberAdd = Json.createObjectBuilder();
        linksGroupMemberAdd.add("method", "POST");
        linksGroupMemberAdd.add("url", serverUrl + "/api/groups/:pubid/members/:userid");
        linksGroupMemberAdd.add("desc", "Add the user in the request params to a group.");
        linksGroupMember.add("add", linksGroupMemberAdd);
        JsonObjectBuilder linksGroupMemberDelete = Json.createObjectBuilder();
        linksGroupMemberDelete.add("method", "DELETE");
        linksGroupMemberDelete.add("url", serverUrl + "/api/groups/:pubid/members/:userid");
        linksGroupMemberDelete.add("desc", "Remove the current user from a group");
        linksGroupMember.add("delete", linksGroupMemberDelete);
        linksGroup.add("member", linksGroupMember);
        JsonObjectBuilder linksGroupCreate = Json.createObjectBuilder();
        linksGroupCreate.add("method", "POST");
        linksGroupCreate.add("url", serverUrl + "/api/groups");
        linksGroupCreate.add("desc", "Create a new group");
        linksGroup.add("create", linksGroupCreate);
        JsonObjectBuilder linksGroupRead = Json.createObjectBuilder();
        linksGroupRead.add("method", "GET");
        linksGroupRead.add("url", serverUrl + "/api/groups/:id");
        linksGroupRead.add("desc", "Fetch a group");
        linksGroup.add("read", linksGroupRead);
        JsonObjectBuilder linksGroupMembers = Json.createObjectBuilder();
        JsonObjectBuilder linksGroupMembersRead = Json.createObjectBuilder();
        linksGroupMembersRead.add("method", "GET");
        linksGroupMembersRead.add("url", serverUrl + "/api/groups/:pubid/members");
        linksGroupMembersRead.add("desc", "Fetch all members of a group");
        linksGroupMembers.add("read", linksGroupMembersRead);
        linksGroup.add("members", linksGroupMembers);
        links.add("group", linksGroup);
        JsonObjectBuilder linksGroupUpdate = Json.createObjectBuilder();
        linksGroupUpdate.add("method", "PATCH");
        linksGroupUpdate.add("url", serverUrl + "/api/groups/:id");
        linksGroupUpdate.add("desc", "Update a group");
        linksGroup.add("update", linksGroupUpdate);
        JsonObjectBuilder linksGroupCreateOrUpdate = Json.createObjectBuilder();
        linksGroupCreateOrUpdate.add("method", "PUT");
        linksGroupCreateOrUpdate.add("url", serverUrl + "/api/groups/:id");
        linksGroupCreateOrUpdate.add("desc", "Create or update a group");
        linksGroup.add("create_or_update", linksGroupCreateOrUpdate);

        JsonObjectBuilder linksGroups = Json.createObjectBuilder();
        JsonObjectBuilder linksGroupsRead = Json.createObjectBuilder();
        linksGroupsRead.add("method", "GET");
        linksGroupsRead.add("url", serverUrl + "/api/groups");
        linksGroupsRead.add("desc", "Fetch the user's groups");
        linksGroups.add("read", linksGroupsRead);
        links.add("groups", linksGroups);

        JsonObjectBuilder linksIndex = Json.createObjectBuilder();
        linksIndex.add("method", "GET");
        linksIndex.add("url", serverUrl + "/api/");
        linksIndex.add("desc", JsonValue.NULL);
        links.add("index", linksIndex);

        JsonObjectBuilder linksLinks = Json.createObjectBuilder();
        linksLinks.add("method", "GET");
        linksLinks.add("url", serverUrl + "/api/links");
        linksLinks.add("desc", "URL templates for generating URLs for HTML pages");
        links.add("links", linksLinks);

        JsonObjectBuilder linksProfile = Json.createObjectBuilder();
        JsonObjectBuilder linksProfileRead = Json.createObjectBuilder();
        linksProfileRead.add("method", "GET");
        linksProfileRead.add("url", serverUrl + "/api/profile");
        linksProfileRead.add("desc", "Fetch the user's profile");
        linksProfile.add("read", linksProfileRead);
        JsonObjectBuilder linksProfileGroups = Json.createObjectBuilder();
        JsonObjectBuilder linksProfileGroupsRead = Json.createObjectBuilder();
        linksProfileGroupsRead.add("method", "GET");
        linksProfileGroupsRead.add("url", serverUrl + "/api/profile/groups");
        linksProfileGroupsRead.add("desc", "Fetch the current user's groups");
        linksProfileGroups.add("read", linksProfileGroupsRead);
        linksProfile.add("groups", linksProfileGroups);
        JsonObjectBuilder linksProfileUpdate = Json.createObjectBuilder();
        linksProfileUpdate.add("method", "PATCH");
        linksProfileUpdate.add("url", serverUrl + "/api/profile");
        linksProfileUpdate.add("desc", "Update a user's preferences");
        linksProfile.add("update", linksProfileUpdate);
        links.add("profile", linksProfile);

        JsonObjectBuilder linksUser = Json.createObjectBuilder();
        JsonObjectBuilder linksUserCreate = Json.createObjectBuilder();
        linksUserCreate.add("method", "POST");
        linksUserCreate.add("url", serverUrl + "/api/users");
        linksUserCreate.add("desc", "Create a new user");
        linksUser.add("create", linksUserCreate);
        JsonObjectBuilder linksUserRead = Json.createObjectBuilder();
        linksUserRead.add("method", "GET");
        linksUserRead.add("url", serverUrl + "/api/users/:userid");
        linksUserRead.add("desc", "Fetch a user");
        linksUser.add("read", linksUserRead);
        JsonObjectBuilder linksUserUpdate = Json.createObjectBuilder();
        linksUserUpdate.add("method", "PATCH");
        linksUserUpdate.add("url", serverUrl + "/api/users/:username");
        linksUserUpdate.add("desc", "Update a user");
        linksUser.add("update", linksUserUpdate);
        links.add("user", linksUser);

        response.add("links", links);

        return response.build();
    }

    @GET
    @Path("links")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getLinks() {
        String serverUrl = Learnweb.getInstance().getServerUrl();

        JsonObjectBuilder links = Json.createObjectBuilder();
        links.add("account.settings", serverUrl + "/lw/myhome/profile.jsf");
        links.add("forgot-password", serverUrl + "/lw/user/password.jsf");
        links.add("groups.new", serverUrl + "/lw/myhome/groups.jsf");
        links.add("help", serverUrl + "/lw/contact.jsf");
        links.add("oauth.authorize", serverUrl + "/oauth/authorize");
        links.add("oauth.revoke", serverUrl + "/oauth/revoke");
        links.add("search.tag", serverUrl + "/search?q=tag%3A%22:tag%22");
        links.add("signup", serverUrl + "/lw/user/register.jsf");
        links.add("user", serverUrl + "/lw/user/detail.jsf?user_id=:user");

        return links.build();
    }

    @POST
    @Path("token")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject requestToken(@FormParam("assertion") String assertion, @FormParam("grant_type") String grantType) {
        JsonObjectBuilder response = Json.createObjectBuilder();
        response.add("access_token", assertion);
        response.add("expires_in", 3600.0);
        response.add("token_type", "Bearer");
        response.add("scope", "annotation:read annotation:write");
        // response.add("refresh_token", "null");
        return response.build();
    }
}
