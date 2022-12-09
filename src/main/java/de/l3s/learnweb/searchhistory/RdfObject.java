package de.l3s.learnweb.searchhistory;

import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

public class RdfObject {

    private int user_id;
    private String rdf_value;

    public RdfObject(final int user_id, final String rdf_value) {
        this.user_id = user_id;
        this.rdf_value = rdf_value;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(final int user_id) {
        this.user_id = user_id;
    }

    public String getRdf_value() {
        return rdf_value;
    }

    public void setRdf_value(final String rdf_value) {
        this.rdf_value = rdf_value;
    }

    public List<String> findResourceWithTopWeight(int n) {
        Model model = ModelFactory.createDefaultModel().read(IOUtils.toInputStream(rdf_value, "UTF-8"), null,"TTL");
        Map<String, Integer> weightedResource;
        //Get the statements from the rdf model first
        StmtIterator iter = model.listStatements();
        while (iter.hasNext()) {
            Statement stmt = iter.nextStatement();  // get next statement
            Resource subject = stmt.getSubject();     // get the subject
            Property predicate = stmt.getPredicate();   // get the predicate
            RDFNode object = stmt.getObject();      // get the object
            if (predicate.equals(model.getProperty("schema:weight"))) {
                //TODO
                //Build a Map with K = resource name, V = weight of the resource
                //Sort after the value
                //Get the first n
            }
        }
        return null;
    }
}
