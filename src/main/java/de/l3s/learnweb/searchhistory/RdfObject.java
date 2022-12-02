package de.l3s.learnweb.searchhistory;

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
}
