package de.l3s.collabrec.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public final class Educor {
    private static final Model M_MODEL = ModelFactory.createDefaultModel();

    public static final String NS = "https://github.com/tibonto/educor/";
    public static final Resource NAMESPACE;

    public static final Resource EducationalResource;
    public static final Resource KnowledgeTopic;
    public static final Resource User;
    public static final Resource UserLogs;
    public static final Resource UserProfile;

    public static final Property hasProfile;
    public static final Property generatesLogs;

    static {
        NAMESPACE = M_MODEL.createResource(NS);

        EducationalResource = M_MODEL.createResource(NS + "EducationalResource");
        KnowledgeTopic = M_MODEL.createResource(NS + "KnowledgeTopic");
        User = M_MODEL.createResource(NS + "User");
        UserLogs = M_MODEL.createResource(NS + "UserLogs");
        UserProfile = M_MODEL.createResource(NS + "UserProfile");

        hasProfile = M_MODEL.createProperty(NS + "hasProfile");
        generatesLogs = M_MODEL.createProperty(NS + "generatesLogs");
    }
}
