package de.l3s.learnweb.hserver.resources;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import javax.ws.rs.Consumes;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.hserver.HUtils;
import de.l3s.learnweb.hserver.entities.Annotation;
import de.l3s.learnweb.hserver.models.SearchResults;
import de.l3s.learnweb.user.User;

@Path("api")
public class AnnotationsController {

    @Context
    private SecurityContext context;

    @POST
    @Path("annotations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Annotation createAnnotation(Annotation annotation) {
        User user = (User) context.getUserPrincipal();

        annotation.setUserId(user.getId());

        if (annotation.getUnsavedDocument() != null) {
            // Retrive unsaved document from request and set document with ID
            annotation.setDocument(Learnweb.getInstance().getDocumentManager().getOrCreate(annotation.getUnsavedDocument()));
        } else if (annotation.getReferences() != null) {
            JsonParser parser = Json.createParser(new StringReader(annotation.getReferences()));
            parser.next();
            JsonArray array = parser.getArray();
            int reference = array.getInt(0);
            Annotation refAnnotation = Learnweb.getInstance().getAnnotationManager().get(reference);
            annotation.setDocumentId(refAnnotation.getDocumentId());
        } else {
            throw new IllegalArgumentException("Unexpected scenario!");
        }
        // Save and return annotation
        return Learnweb.getInstance().getAnnotationManager().save(annotation);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("search")
    public SearchResults searchAnnotations(@DefaultValue("20") @QueryParam("limit") final Integer limit,
        @DefaultValue("0") @QueryParam("offset") final Integer offset,
        @DefaultValue("updated") @QueryParam("sort") String sort,
        @DefaultValue("desc") @QueryParam("order") final String order,
        @QueryParam("search_after") final String searchAfter,
        @QueryParam("url") final String url, // alias of uri
        @QueryParam("uri") final String uri,
        @QueryParam("uri.parts") final String uriParts,
        @QueryParam("wildcard_uri") final String wildcardUri, // not implemented right now, in original client marked as experimental feature
        @QueryParam("user") final String user,
        @QueryParam("group") final String groupId,
        @QueryParam("tag") final String tag,
        @QueryParam("tags") final String tags, // similar ro tag, but csv
        @QueryParam("any") final String any,
        @QueryParam("quote") final String quote,
        @QueryParam("references") final String references,
        @QueryParam("text") final String text) {

        List<String> tagsList = new ArrayList<>();
        if (StringUtils.isNotBlank(tag)) {
            tagsList.addAll(Arrays.asList(tag.split("\\s+")));
        } else if (StringUtils.isNotBlank(tags)) {
            tagsList.addAll(Arrays.asList(tag.split("\\s*,\\s*")));
        }

        if (StringUtils.isNotBlank(sort)) {
            if (Arrays.asList("created", "updated", "group", "id", "user").contains(sort.toLowerCase())) {
                if ("group".equals(sort.toLowerCase())) {
                    sort = "group_id";
                } else if ("user".equals(sort.toLowerCase())) {
                    sort = "user_id";
                }
            } else {
                throw new IllegalArgumentException("`sort` value is invalid!");
            }
        }

        if (StringUtils.isNotBlank(sort)) {
            if (!Arrays.asList("asc", "desc").contains(order.toLowerCase())) {
                throw new IllegalArgumentException("`order` value is invalid!");
            }
        }

        List<Annotation> annotations = Learnweb.getInstance().getAnnotationManager()
            .search(limit, offset,
                sort, order, searchAfter,
                ObjectUtils.firstNonNull(uri, url), uriParts,
                HUtils.getUserIdFromUser(user), HUtils.parseInt(groupId),
                tagsList, any, quote, references, text);

        return new SearchResults(annotations);
    }

    @GET
    @Path("annotations/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Annotation getAnnotation(@PathParam("id") Integer id) {
        return Learnweb.getInstance().getAnnotationManager().get(id);
    }

    @PATCH
    @Path("annotations/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Annotation updateAnnotation(@PathParam("id") Integer id, Annotation mods) {
        Annotation annotation = Learnweb.getInstance().getAnnotationManager().get(id);
        // TODO: update (mods includes only updated fields)
        return Learnweb.getInstance().getAnnotationManager().update(annotation);
    }

    @DELETE
    @Path("annotations/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject deleteAnnotation(@PathParam("id") Integer id) {
        Learnweb.getInstance().getAnnotationManager().delete(id);

        return Json.createObjectBuilder()
            .add("deleted", true)
            .add("id", id)
            .build();
    }

    @PUT
    @Path("annotations/{id}/flag")
    @Produces(MediaType.APPLICATION_JSON)
    public Response flagAnnotation(@PathParam("id") Integer id) {
        User user = (User) context.getUserPrincipal();

        Learnweb.getInstance().getAnnotationManager().flag(id, user);
        return Response.noContent().build();
    }

    @PUT
    @Path("annotations/{id}/hide")
    @Produces(MediaType.APPLICATION_JSON)
    public Response hideAnnotation(@PathParam("id") Integer id) {
        Learnweb.getInstance().getAnnotationManager().hide(id);
        return Response.noContent().build();
    }

    @DELETE
    @Path("annotations/{id}/hide")
    @Produces(MediaType.APPLICATION_JSON)
    public Response showAnnotation(@PathParam("id") Integer id) {
        Learnweb.getInstance().getAnnotationManager().show(id);
        return Response.noContent().build();
    }
}
