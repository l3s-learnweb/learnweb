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
    private Integer requiredQuestionId;
    private String requiredAnswer;
    private boolean deleted = false;
    private int order;
    private String title;
    private String description;
    private boolean sampling;
    private ArrayList<SurveyPageVariant> variants = new ArrayList<>();

    private transient List<SurveyQuestion> questions;

    public SurveyPage() {
    }

    public SurveyPage(SurveyPage other) {
        this.id = 0;
        this.resourceId = other.resourceId;
        this.requiredQuestionId = other.requiredQuestionId;
        this.requiredAnswer = other.requiredAnswer;
        this.title = other.title;
        this.description = other.description;
        this.sampling = other.sampling;
        this.deleted = other.deleted;
        this.order = other.order;

        this.variants = new ArrayList<>();
        for (SurveyPageVariant variant : other.getVariants()) {
            this.variants.add(new SurveyPageVariant(variant));
        }

        this.questions = new ArrayList<>();
        for (SurveyQuestion question : other.getQuestions()) {
            this.questions.add(new SurveyQuestion(question));
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

    public ArrayList<SurveyPageVariant> getVariants() {
        return variants;
    }

    public void setVariants(final ArrayList<SurveyPageVariant> variants) {
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

    public boolean hasCondition() {
        return requiredQuestionId != null && requiredAnswer != null && !requiredAnswer.isBlank();
    }

    public Integer getRequiredQuestionId() {
        return requiredQuestionId;
    }

    public void setRequiredQuestionId(final Integer requiredQuestionId) {
        this.requiredQuestionId = requiredQuestionId;
    }

    public String getRequiredAnswer() {
        return requiredAnswer;
    }

    public void setRequiredAnswer(final String requiredAnswer) {
        this.requiredAnswer = requiredAnswer;
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
                questions = new ArrayList<>(Learnweb.dao().getSurveyDao().findQuestionsAndOptionsByPageId(getId()));
            }
        }
        return questions;
    }
}
