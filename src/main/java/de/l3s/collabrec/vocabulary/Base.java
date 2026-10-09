package de.l3s.collabrec.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public final class Base {
    private static final Model M_MODEL = ModelFactory.createDefaultModel();

    public static final String NS = "https://github.com/l3s-learnweb/collabrec/";
    public static final Resource NAMESPACE;

    public static final Resource SearchQuery;
    public static final Resource SearchResult;
    public static final Resource Snippet;
    public static final Resource SearchSession;
    public static final Resource UserApplicationData;

    public static final Property shares;
    public static final Property generatesResult;
    public static final Property hasWebpage;
    public static final Property hasSearches;

    public static final Property confidenceScore;
    public static final Property surfaceForm;
    public static final Property groupType;
    public static final Property listingOrder;
    public static final Property opened;
    public static final Property interest;
    public static final Property profession;
    public static final Property username;

    static {
        NAMESPACE = M_MODEL.createResource(NS);

        SearchQuery = M_MODEL.createResource(NS + "SearchQuery");
        SearchResult = M_MODEL.createResource(NS + "SearchResult");
        Snippet = M_MODEL.createResource(NS + "Snippet");
        SearchSession = M_MODEL.createResource(NS + "SearchSession");
        UserApplicationData = M_MODEL.createResource(NS + "UserApplicationData");

        shares = M_MODEL.createProperty(NS + "shares");
        generatesResult = M_MODEL.createProperty(NS + "generatesResult");
        hasWebpage = M_MODEL.createProperty(NS + "hasWebpage");
        hasSearches = M_MODEL.createProperty(NS + "hasSearches");

        confidenceScore = M_MODEL.createProperty(NS + "confidenceScore");
        surfaceForm = M_MODEL.createProperty(NS + "surfaceForm");
        groupType = M_MODEL.createProperty(NS + "groupType");
        listingOrder = M_MODEL.createProperty(NS + "listingOrder");
        opened = M_MODEL.createProperty(NS + "opened");
        interest = M_MODEL.createProperty(NS + "interest");
        profession = M_MODEL.createProperty(NS + "profession");
        username = M_MODEL.createProperty(NS + "username");
    }
}
