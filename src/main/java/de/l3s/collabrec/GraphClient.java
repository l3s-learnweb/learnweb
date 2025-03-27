package de.l3s.collabrec;

import java.io.Serial;
import java.io.Serializable;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.update.Update;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

@ApplicationScoped
public class GraphClient implements Serializable {
    @Serial
    private static final long serialVersionUID = 8443696658680840510L;
    private static final Logger log = LogManager.getLogger(GraphClient.class);

    static final String base = "https://github.com/l3s-learnweb/collabrec/";
    static final String educor = "https://github.com/tibonto/educor#";

    static final PrefixMapping prefixMapping = PrefixMapping.Factory.create()
        .setNsPrefixes(PrefixMapping.Standard)
        .setNsPrefix("schema", "https://schema.org/")
        .setNsPrefix("foaf", FOAF.getURI())
        .setNsPrefix("educor", educor)
        .setNsPrefix("base", base).lock();

    @Inject
    private ConfigProvider configProvider;

    private RDFConnection createConnection() {
        return RDFConnection.connectPW(
            configProvider.getProperty("fuseki_url"),
            configProvider.getProperty("fuseki_user"),
            configProvider.getProperty("fuseki_password")
        );
    }

    public void update(UpdateBuilder updateBuilder) throws GraphException {
        try (RDFConnection conn = createConnection()) {
            Update update = updateBuilder.build();
            log.debug("Executing SPARQL update: {}", update.toString());
            conn.update(update);
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

    public static UpdateBuilder createUserUpdate(User user) {
        Node userNode = NodeFactory.createURI(base + "user" + user.getId());

        UpdateBuilder ub = new UpdateBuilder(prefixMapping);

        ub.addInsert(userNode, RDF.type, NodeFactory.createURI(educor + "User"))
            .addInsert(userNode, "schema:username", user.getUsername())
            .addWhere(new WhereBuilder()
                .addMinus(new WhereBuilder()
                    .addWhere(userNode, RDF.type, NodeFactory.createURI(educor + "User"))));

        return ub;
    }

    public static UpdateBuilder createGroupUpdate(Group group) {
        Node groupNode = NodeFactory.createURI(base + "group" + group.getId());

        UpdateBuilder ub = new UpdateBuilder(prefixMapping);

        ub.addInsert(groupNode, RDF.type, NodeFactory.createURI(FOAF.getURI() + "Group"))
            .addInsert(groupNode, "schema:name", group.getTitle())
            .addInsert(groupNode, "schema:description", group.getDescription())
            .addWhere(new WhereBuilder()
                .addMinus(new WhereBuilder()
                    .addWhere(groupNode, RDF.type, NodeFactory.createURI(FOAF.getURI() + "Group"))));

        return ub;
    }

    public static UpdateBuilder updateGroupUpdate(Group group) {
        Node groupNode = NodeFactory.createURI(base + "group" + group.getId());

        // Create proper URI nodes for predicates
        Node nameNode = NodeFactory.createURI("https://schema.org/name");
        Node descNode = NodeFactory.createURI("https://schema.org/description");
        Node typeNode = NodeFactory.createURI(FOAF.getURI() + "Group");

        UpdateBuilder ub = new UpdateBuilder(prefixMapping);

        // Always ensure the group has the proper type
        ub.addInsert(groupNode, RDF.type, typeNode);

        // Delete any existing name and description properties
        ub.addDelete(groupNode, nameNode, "?oldName")
            .addDelete(groupNode, descNode, "?oldDesc");

        // Where clause to find any existing values
        ub.addWhere(new WhereBuilder()
            .addOptional(groupNode, nameNode, "?oldName")
            .addOptional(groupNode, descNode, "?oldDesc"));

        // Insert the current values
        ub.addInsert(groupNode, nameNode, group.getTitle())
            .addInsert(groupNode, descNode, group.getDescription());

        return ub;
    }

    public static UpdateBuilder createUserGroupMembershipUpdate(User user, Group group) {
        Node userNode = NodeFactory.createURI(base + "user" + user.getId());
        Node groupNode = NodeFactory.createURI(base + "group" + group.getId());

        UpdateBuilder ub = new UpdateBuilder(prefixMapping);

        ub.addInsert(userNode, "schema:memberOf", groupNode)
            .addWhere(new WhereBuilder()
                .addMinus(new WhereBuilder()
                    .addWhere(userNode, "schema:memberOf", groupNode)));

        return ub;
    }

    public static UpdateBuilder createUserProfileUpdate(User user) {
        Node userNode = NodeFactory.createURI(base + "user" + user.getId());
        Node profileNode = NodeFactory.createURI(base + "UserProfile" + user.getId());

        UpdateBuilder ub = new UpdateBuilder(prefixMapping);

        ub.addInsert(userNode, educor + "hasProfile", profileNode);

        ub.addInsert(profileNode, RDF.type, NodeFactory.createURI(educor + "UserProfile"))
            .addInsert(profileNode, "base:interest", user.getInterest())
            .addInsert(profileNode, "base:profession", user.getProfession())
            .addInsert(profileNode, "base:name", user.getFullName())
            .addInsert(profileNode, "base:email", user.getEmail());

        return ub;
    }
}
