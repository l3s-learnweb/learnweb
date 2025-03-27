package de.l3s.collabrec;

import java.io.Serial;
import java.io.Serializable;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.update.Update;
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

@ApplicationScoped
public class GraphClient implements Serializable {
    @Serial
    private static final long serialVersionUID = 8443696658680840510L;
    private static final Logger log = LogManager.getLogger(GraphClient.class);

    static final PrefixMapping prefixMapping = PrefixMapping.Factory.create()
        // .setNsPrefix( "rdfs", RDFS.uri )
        .setNsPrefix("rdf", RDF.uri)
        .setNsPrefix("xsd", XSD.NS)
        // .setNsPrefix( "owl", OWL.NS )
        .setNsPrefix("foaf", FOAF.NS)
        .setNsPrefix("schema", SchemaDO.NS)
        .setNsPrefix("educor", Educor.NS)
        .setNsPrefix("base", Base.NS).lock();

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

    public static UpdateBuilder updateUserNode(User user) {
        Resource u = ResourceFactory.createResource(Base.NS + "user" + user.getId());
        Resource p = ResourceFactory.createResource(Base.NS + "userProfile" + user.getId());

        UpdateBuilder ub = new UpdateBuilder(prefixMapping);

        ub.addInsert(u, RDF.type, Educor.User)
            .addInsert(u, SchemaDO.name, user.getFullName())
            .addInsert(u, Educor.hasProfile, p);

        ub.addInsert(p, RDF.type, Educor.UserProfile)
            .addInsert(p, Base.interest, user.getInterest())
            .addInsert(p, Base.profession, user.getProfession())
            .addInsert(p, Base.username, user.getUsername());
        // The condition below can be used to restrict the update to only insert if the user does not already exist, but we can simply overwrite existing data
        // .addWhere(new WhereBuilder()
        //     .addMinus(new WhereBuilder()
        //         .addWhere(userNode, RDF.type, Educor.User)));

        return ub;
    }

    public static UpdateBuilder updateGroupNode(Group group) {
        Resource g = ResourceFactory.createResource(Base.NS + "group" + group.getId());

        UpdateBuilder ub = new UpdateBuilder(prefixMapping);

        ub.addInsert(g, RDF.type, FOAF.Group)
            .addInsert(g, SchemaDO.name, group.getTitle())
            .addInsert(g, SchemaDO.description, group.getDescription())
            .addInsert(g, SchemaDO.dateCreated, NodeFactory.createLiteralDT(group.getCreatedAt().toString(), XSDDatatype.XSDdateTime));
            // .addInsert(g, Base.groupType, group.getR()); FIXME: what is groupType?
            // .addWhere(new WhereBuilder()
            //     .addMinus(new WhereBuilder()
            //         .addWhere(g, RDF.type, NodeFactory.createURI(FOAF.NS + "Group"))));

        return ub;
    }

    public static UpdateBuilder associateUserGroup(User user, Group group) {
        Resource u = ResourceFactory.createResource(Base.NS + "user" + user.getId());
        Resource g = ResourceFactory.createResource(Base.NS + "group" + group.getId());

        UpdateBuilder ub = new UpdateBuilder(prefixMapping);
        ub.addInsert(u, SchemaDO.memberOf, g);
        return ub;
    }

    public static UpdateBuilder createUserGroupMembershipUpdate(User user, Group group) {
        Node userNode = NodeFactory.createURI(Base.NS + "user" + user.getId());
        Node groupNode = NodeFactory.createURI(Base.NS + "group" + group.getId());

        UpdateBuilder ub = new UpdateBuilder(prefixMapping);

        ub.addInsert(userNode, "schema:memberOf", groupNode)
            .addWhere(new WhereBuilder()
                .addMinus(new WhereBuilder()
                    .addWhere(userNode, "schema:memberOf", groupNode)));

        return ub;
    }

    public static UpdateBuilder createUserProfileUpdate(User user) {
        Node userNode = NodeFactory.createURI(Base.NS + "user" + user.getId());
        Node profileNode = NodeFactory.createURI(Base.NS + "UserProfile" + user.getId());

        UpdateBuilder ub = new UpdateBuilder(prefixMapping);

        ub.addInsert(userNode, Educor.hasProfile, profileNode);

        ub.addInsert(profileNode, RDF.type, Educor.UserProfile)
            .addInsert(profileNode, "base:interest", user.getInterest())
            .addInsert(profileNode, "base:profession", user.getProfession())
            .addInsert(profileNode, "base:name", user.getFullName())
            .addInsert(profileNode, "base:email", user.getEmail());

        return ub;
    }
}
