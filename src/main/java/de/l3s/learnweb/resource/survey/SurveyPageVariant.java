package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;

import de.l3s.util.HasId;

public class SurveyPageVariant implements HasId, Serializable {
    @Serial
    private static final long serialVersionUID = -1852496221401358199L;

    private int id;
    private String description;

    public SurveyPageVariant() {
    }

    public SurveyPageVariant(SurveyPageVariant page) {
        setId(0);
        setDescription(page.description);
    }

    @Override
    public int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
