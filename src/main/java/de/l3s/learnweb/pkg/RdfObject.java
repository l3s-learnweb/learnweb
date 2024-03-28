package de.l3s.learnweb.pkg;


public class RdfObject {

    private int userId;
    private String rdfValue;

    public RdfObject(final int userId, final String rdfValue) {
        this.userId = userId;
        this.rdfValue = rdfValue;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(final int userId) {
        this.userId = userId;
    }

    public String getRdfValue() {
        return rdfValue;
    }

    public void setRdfValue(final String rdfValue) {
        this.rdfValue = rdfValue;
    }
}
