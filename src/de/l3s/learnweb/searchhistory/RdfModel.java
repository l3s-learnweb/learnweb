package de.l3s.learnweb.searchhistory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

public class RdfModel {

    private static final Pattern COMPILE = Pattern.compile(" ");
    private static final Pattern PATTERN = Pattern.compile("\\<[^>]*>");
    private final Model model;
    private Model ontologyModel;
    private static final String prefixBase = "https://github.com/tibonto/PKGonto/";
    private static final String prefixSchema = "https://schema.org/";
    private Group group;
    private void readOntologyModel() throws FileNotFoundException {
        ontologyModel = ModelFactory.createDefaultModel();
        ontologyModel.read("/ontology/pkgOnto.ttl","TTL");
    }

    public RdfModel(final User user, final Group group, final List<SearchSession> sessions) throws IOException{
        model = ModelFactory.createDefaultModel();
        model.setNsPrefix("schema", prefixSchema);
        this.group = group;
        readOntologyModel();
        addStatement("Group/" + group.getTitle(), "description", PATTERN.matcher(group.getDescription()).replaceAll(""), "literal");
        addStatement("Group/" + group.getTitle(), "name", group.getTitle(), "literal");
        addStatement("Group/" + group.getTitle(), "dateCreated", group.getCreatedAt().format(DateTimeFormatter.ISO_DATE), "literal");
        addStatement("Group/" + group.getTitle(), "createInputStream", "InputStream/" + group.getTitle(), "resource");

        addStatement("User/" + user.getUsername(), "email", user.getEmail(), "literal");
        addStatement("User/" + user.getUsername(), "hasProfile", "UserProfile/" + user.getUsername(), "resource");
        addStatement("UserProfile/" + user.getUsername(), "username", user.getUsername(), "literal");
        addStatement("User/" + user.getUsername(), "memberOf", "Group/" + group.getTitle(), "resource");
        addStatement("User/" + user.getUsername(), "name", user.getUsername(), "literal");

        for (SearchSession session : sessions) {
            if (session.getUser().equals(user)) {
                addStatement("User/" + user.getUsername(), "generatesLogs", "SearchSession/" + session.getSessionId(), "resource");
                addStatement("SearchSession/" + session.getSessionId(), "startTime", session.getStartTimestamp().format(DateTimeFormatter.ISO_DATE), "literal");
                addStatement("SearchSession/" + session.getSessionId(), "endTime", session.getEndTimestamp().format(DateTimeFormatter.ISO_DATE), "literal");
                for (SearchQuery searchQuery : session.getQueries()) {
                    addStatement("SearchSession/" + session.getSessionId(), "contains", "SearchQuery/" + searchQuery.query(), "resource");
                    addStatement("SearchQuery/" + searchQuery.query(), "dateCreated", searchQuery.timestamp().format(DateTimeFormatter.ISO_DATE), "literal");
                }
            }
        }
    }

    public void addStatement(String sbj, String pre, String obj, String type) {
        Resource subject = model.createResource(prefixBase + COMPILE.matcher(sbj).replaceAll("_"));

        switch (type) {
            case "literal" -> {
                if (obj == null) obj = "";
                subject.addProperty(model.createProperty("schema:" + pre), obj);
            }
            case "resource" -> {
                Property predicate = model.createProperty("schema:" + pre);
                RDFNode object = model.createResource(prefixBase + COMPILE.matcher(obj).replaceAll("_"));
                Statement stmt = model.createStatement(subject, predicate, object);
                model.add(stmt);
            }
            default -> {
            }
        }
    }

    public void addEntity(String uri, String surfaceForm, double weight, double score) {
        if (uri == "default") return;
        addStatement("RecognizedEntities/" + surfaceForm, "identifier", uri, "literal");
        addStatement("RecognizedEntities/" + surfaceForm, "surfaceForm", surfaceForm, "literal");
        addStatement("RecognizedEntities/" + surfaceForm, "weight", String.valueOf(weight), "literal");
        addStatement("RecognizedEntities/" + surfaceForm, "confidenceScore", String.valueOf(score), "literal");
        addStatement("RecognizedEntities/" + surfaceForm, "processes", "InputStream/" + group.getTitle(), "resource");
    }

    public void printModel(String groupName, int index) throws IOException {
        // list the statements in the Model
        StmtIterator iter = model.listStatements();

        String localPath = System.getProperty("user.dir") + "\\" + "group_summary_" + groupName + "_no" + index + ".ttl";

        //print out the predicate, subject and object of each statement
        while (iter.hasNext()) {
            Statement stmt = iter.nextStatement();  // get next statement
            Resource subject = stmt.getSubject();     // get the subject
            Property predicate = stmt.getPredicate();   // get the predicate
            RDFNode object = stmt.getObject();      // get the object
            PrintStream ps = new PrintStream(System.out, true, "UTF-8");
            ps.print(subject.toString());
            ps.print(" " + predicate.toString() + " ");
            if (object instanceof Resource) {
                ps.print(object.toString());
            } else {
                // object is a literal
                ps.print(" \"" + object.toString() + "\"");
            }
            ps.println(" .");
        }
        File file = new File(localPath);
        Writer writer = new FileWriter(localPath, StandardCharsets.UTF_8);
        model.write(writer, "TTL", prefixBase);
        writer.close();
    }

    public Model getModel() {
        return model;
    }
}