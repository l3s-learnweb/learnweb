package de.l3s.collabrec;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SchemaDO;
import org.apache.jena.vocabulary.XSD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.collabrec.vocabulary.Base;
import de.l3s.collabrec.vocabulary.Educor;
import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

/**
 * Stores the users, groups and their relations in the CollabRec knowledge graph (Fuseki).
 * Disabled when {@code fuseki_url} isn't configured.
 */
@ApplicationScoped
public class GraphClient {
    private static final Logger log = LogManager.getLogger(GraphClient.class);

    static final PrefixMapping prefixMapping = PrefixMapping.Factory.create()
        .setNsPrefix("rdf", RDF.uri)
        .setNsPrefix("xsd", XSD.NS)
        .setNsPrefix("foaf", FOAF.NS)
        .setNsPrefix("schema", SchemaDO.NS)
        .setNsPrefix("educor", Educor.NS)
        .setNsPrefix("base", Base.NS).lock();

    @Inject
    private ConfigProvider configProvider;

    public boolean isEnabled() {
        return StringUtils.isNotEmpty(configProvider.getProperty("fuseki_url"));
    }

    private RDFConnection createConnection() {
        return RDFConnection.connectPW(
            configProvider.getProperty("fuseki_url"),
            configProvider.getProperty("fuseki_user"),
            configProvider.getProperty("fuseki_password")
        );
    }

    public void update(UpdateBuilder updateBuilder) throws GraphException {
        try (RDFConnection conn = createConnection()) {
            UpdateRequest request = updateBuilder.buildRequest();
            log.debug("Executing SPARQL update: {}", request);
            conn.update(request);
        } catch (Exception e) {
            throw new GraphException("Failed to update data", e);
        }
    }

    public Model select(SelectBuilder selectBuilder) throws GraphException {
        try (RDFConnection conn = createConnection()) {
            return conn.queryConstruct(selectBuilder.build());
        } catch (Exception e) {
            throw new GraphException("Failed to query data", e);
        }
    }

    static Resource userResource(int userId) {
        return ResourceFactory.createResource(Base.NS + "user" + userId);
    }

    static Resource userProfileResource(int userId) {
        return ResourceFactory.createResource(Base.NS + "userProfile" + userId);
    }

    static Resource groupResource(int groupId) {
        return ResourceFactory.createResource(Base.NS + "group" + groupId);
    }

    /**
     * Creates the user and its profile, or replaces their properties if they already exist.
     */
    public static UpdateBuilder updateUserNode(User user) {
        Resource u = userResource(user.getId());
        Resource p = userProfileResource(user.getId());

        UpdateBuilder ub = new UpdateBuilder(prefixMapping);
        WhereBuilder where = new WhereBuilder();

        ub.addInsert(u, RDF.type, Educor.User)
            .addInsert(u, Educor.hasProfile, p)
            .addInsert(p, RDF.type, Educor.UserProfile);

        replaceValue(ub, where, u, SchemaDO.name, user.getFullName());
        replaceValue(ub, where, p, Base.interest, user.getInterest());
        replaceValue(ub, where, p, Base.profession, user.getProfession());
        replaceValue(ub, where, p, Base.username, user.getUsername());

        return ub.addWhere(where);
    }

    /**
     * Creates the group, or replaces its properties if it already exists.
     */
    public static UpdateBuilder updateGroupNode(Group group) {
        Resource g = groupResource(group.getId());

        UpdateBuilder ub = new UpdateBuilder(prefixMapping);
        WhereBuilder where = new WhereBuilder();

        ub.addInsert(g, RDF.type, FOAF.Group);

        replaceValue(ub, where, g, SchemaDO.name, group.getTitle());
        replaceValue(ub, where, g, SchemaDO.description, group.getDescription());
        replaceValue(ub, where, g, SchemaDO.dateCreated, toDateTimeLiteral(group.getCreatedAt()));
        // TODO: add Base.groupType, once it is clear what it represents

        return ub.addWhere(where);
    }

    public static UpdateBuilder associateUserGroup(int userId, int groupId) {
        return new UpdateBuilder(prefixMapping)
            .addInsert(userResource(userId), SchemaDO.memberOf, groupResource(groupId));
    }

    public static UpdateBuilder dissociateUserGroup(int userId, int groupId) {
        return new UpdateBuilder(prefixMapping)
            .addDelete(userResource(userId), SchemaDO.memberOf, groupResource(groupId));
    }

    /**
     * Deletes the current values of the property and inserts the new one, if it isn't null.
     */
    private static void replaceValue(UpdateBuilder ub, WhereBuilder where, Resource subject, Property property, Object value) {
        Var old = Var.alloc("old_" + property.getLocalName());
        ub.addDelete(subject, property, old);
        where.addOptional(subject, property, old);

        if (value != null) {
            ub.addInsert(subject, property, value);
        }
    }

    private static Object toDateTimeLiteral(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return NodeFactory.createLiteralDT(dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), XSDDatatype.XSDdateTime);
    }
}
