package de.l3s.learnweb.searchhistory;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Literal;
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
    private Model model;
    public static final String prefixBase = "https://github.com/tibonto/PKGonto/";
    private static final String prefixSchema = "https://schema.org/";
    private static final String prefixEducor = "https://github.com/tibonto/educor#";
    private static final String prefixFoaf = "http://xmlns.com/foaf/spec/";

    public RdfModel() {
        model = ModelFactory.createDefaultModel();
        model.setNsPrefix("schema", prefixSchema);
        model.setNsPrefix("educor", prefixEducor);
        model.setNsPrefix("foaf", prefixFoaf);

        model.createResource("SharedObject/Negativity_Exponential_Algorithm");
    }

    public void addStatement(String sbj, String pre, String obj, String type) {
        Resource subject = model.getResource((sbj.contains(":")) ? COMPILE.matcher(sbj).replaceAll("_") : prefixBase + COMPILE.matcher(sbj).replaceAll("_"));
        Property predicate = model.createProperty(pre);

        switch (type) {
            case "literal" -> {
                if (obj == null) {
                    obj = "";
                }
                Literal object = model.createLiteral(obj);
                List<Statement> statementsToRemove = new ArrayList<>();
                StmtIterator existingStatements = model.listStatements(subject, predicate, (RDFNode) null);
                while (existingStatements.hasNext()) {
                    Statement statement = existingStatements.nextStatement();
                    statementsToRemove.add(statement);
                }
                for (Statement statement : statementsToRemove) {
                    model.remove(statement);
                }
                if (subject == null) {
                    subject = model.createResource(COMPILE.matcher(sbj).replaceAll("_"));
                    model.createStatement(subject, predicate, object);
                }
                subject.addLiteral(predicate, object);
            }
            case "resource" -> {
                RDFNode object = model.getResource(COMPILE.matcher(obj).replaceAll("_"));
                if (object == null) {
                    object = model.createResource(COMPILE.matcher(obj).replaceAll("_"));
                }
                subject.addProperty(predicate, object);
            }
            default -> {
            }
        }
    }

    public void addGroup(final User user, final Group group) {
        addStatement("educor:User/" + user.getId(), "schema:email", user.getEmail(), "literal");
        addStatement("educor:User/" + user.getId(), "educor:hasProfile", "educor:UserProfile/" + user.getId(), "resource");
        addStatement("educor:UserProfile/" + user.getId(), prefixBase + "username", user.getUsername(), "literal");
        addStatement("educor:User/" + user.getId(), "schema:name", user.getUsername(), "literal");
        addStatement("foaf:Group/" + group.getId(), "schema:description", PATTERN.matcher(group.getDescription()).replaceAll(""), "literal");
        addStatement("foaf:Group/" + group.getId(), "schema:name", group.getTitle(), "literal");
        addStatement("foaf:Group/" + group.getId(), "schema:dateCreated", group.getCreatedAt().format(DateTimeFormatter.ISO_DATE), "literal");
        addStatement("educor:User/" + user.getId(), "schema:memberOf", "foaf:Group/" + group.getId(), "resource");
    }

    public void addEntity(String name, String uri, String surfaceForm, double weight, double score, LocalDateTime time) {
        if (Objects.equals(uri, "default")) {
            return;
        }
        addStatement("RecognizedEntities/" + name, "schema:identifier", uri, "resource");
        addStatement("RecognizedEntities/" + name, prefixBase + "surfaceForm", surfaceForm, "literal");
        addStatement("RecognizedEntities/" + name, prefixBase + "confidenceScore", String.valueOf(score), "literal");
        addStatement("RecognizedEntities/" + name, "schema:dateCreated", time.format(DateTimeFormatter.ISO_DATE), "literal");
        addStatement("RecognizedEntities/" + name, prefixBase + "weight", String.valueOf(weight), "literal");
    }

    public String printModel() {
        // list the statements in the Model
        //print out the predicate, subject and object of each statement
        StringWriter out = new StringWriter();
        model.write(out, "TTL", prefixBase);
        //model.write(writer, "TTL", prefixBase);
        return out.toString();
    }

    public Model getModel() {
        return model;
    }

    public void makeModelFromString(final String inputStream) {
        StringReader stringReader = new StringReader(inputStream);
        model.read(stringReader, prefixBase, "TTL");
    }
}
