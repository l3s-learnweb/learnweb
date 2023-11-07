package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.util.Deletable;
import de.l3s.util.HasId;

public class SurveyPage implements HasId, Deletable, Serializable {
    @Serial
    private static final long serialVersionUID = 896140871851014639L;

    private int id;
    private int resourceId;
    private boolean deleted = false;
    private int order;
    private String title; // question on the website, is replaced by a translated term if available
    private String description; // an explanation, displayed as tooltip
    private boolean sampling = false;
    private List<SurveyPageVariant> variants = new ArrayList<>();

    private transient List<SurveyQuestion> questions;

    public SurveyPage(int resourceId) {
        setResourceId(resourceId);
    }

    public SurveyPage(SurveyPage other) {
        setId(0);
        setResourceId(0);
        setTitle(other.title);
        setDescription(other.description);
        setSampling(other.sampling);
        setDeleted(other.deleted);
        setOrder(other.order);

        for (SurveyQuestion question : other.getQuestions()) {
            getQuestions().add(new SurveyQuestion(question));
        }
    }

    public SurveyPageVariant getVariant(int responseId) {
        if (!isSampling()) {
            return null;
        }

        int index = getRandomSample(responseId, id, variants.size());
        return variants.get(index);
    }

    protected int getRandomSample(int responseId, int pageId, int totalSamples) {
        return new Random(responseId + pageId * 1000000000L).nextInt(totalSamples);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSampling() {
        return sampling && !variants.isEmpty();
    }

    public void setSampling(final boolean sampling) {
        this.sampling = sampling;
    }

    public List<SurveyPageVariant> getVariants() {
        return variants;
    }

    public void setVariants(final List<SurveyPageVariant> variants) {
        this.variants = variants;
    }

    @Override
    public int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(final int resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<SurveyQuestion> getQuestions() {
        if (null == questions) {
            if (getId() == 0) {
                questions = new ArrayList<>();
            } else {
                questions = Learnweb.dao().getSurveyDao().findQuestionsAndOptionsByResourceId(getResourceId(), getId());
            }
        }
        return questions;
    }
}
