package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.l3s.util.Deletable;
import de.l3s.util.HasId;

public class SurveyQuestion implements HasId, Deletable, Serializable {
    @Serial
    private static final long serialVersionUID = -7698089608547415349L;

    public enum QuestionType { // represents primefaces input types
        INPUT_TEXT(false, false), // options define the valid length (first entry = min length, second entry = max length)
        INPUT_TEXTAREA(false, false), // options define the valid length (first entry = min length, second entry = max length)

        ONE_RADIO(false, true), // Multiple choice
        ONE_MENU(false, true), // Dropdown
        ONE_MENU_EDITABLE(false, true), // Dropdown with free text input
        MANY_CHECKBOX(false, true, true), // Checkboxes
        MULTIPLE_MENU(false, true, true), // Tags

        FULLWIDTH_HEADER();

        private final boolean readOnly;
        private final boolean options;
        private final boolean multiple;

        QuestionType() {
            this.readOnly = true;
            this.options = false;
            this.multiple = false;
        }

        QuestionType(boolean readOnly, boolean options) {
            this.readOnly = readOnly;
            this.options = options;
            this.multiple = false;
        }

        QuestionType(boolean readOnly, boolean options, boolean multiple) {
            this.readOnly = readOnly;
            this.multiple = multiple;
            this.options = options;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public boolean isOptions() {
            return options;
        }

        public boolean isMultiple() {
            return multiple;
        }
    }

    private int id;
    private int resourceId;
    private int pageId;
    private boolean deleted = false;
    private int order;
    private QuestionType type;
    private String question; // question on the website, is replaced by a translated term if available
    private String description; // an explanation, displayed as tooltip
    private boolean required = false;
    private Integer minLength;
    private Integer maxLength;

    private List<SurveyQuestionOption> options = new ArrayList<>(); // predefined options for types like ONE_MENU, ONE_RADIO, MANY_CHECKBOX ...

    public SurveyQuestion(QuestionType type) {
        this.type = type;
        // set default length limits for text input fields
        if (type == QuestionType.INPUT_TEXT || type == QuestionType.INPUT_TEXTAREA) {
            minLength = 0;
            maxLength = 6000;
        }
    }

    public SurveyQuestion(QuestionType type, int resourceId) {
        this(type);
        setResourceId(resourceId);
    }

    public SurveyQuestion(SurveyQuestion question) {
        setId(0);
        setResourceId(0);
        setPageId(question.pageId);
        setQuestion(question.question);
        setDescription(question.description);
        setMinLength(question.minLength);
        setMaxLength(question.maxLength);
        setRequired(question.required);
        setDeleted(question.deleted);
        setOrder(question.order);
        for (SurveyQuestionOption answer : question.getOptions()) {
            answer.setId(0);
            options.add(answer);
        }
        setType(question.type);
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;

        if (type.options && this.getOptions().isEmpty()) {
            this.getOptions().add(new SurveyQuestionOption(id));
        }
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<SurveyQuestionOption> getOptions() {
        return options;
    }

    public void setOptions(List<SurveyQuestionOption> options) {
        this.options = options;
    }

    public List<SurveyQuestionOption> getActualAnswers() {
        return options.stream().filter(answer -> !answer.isDeleted()).toList();
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

    public int getPageId() {
        return pageId;
    }

    public void setPageId(final int pageId) {
        this.pageId = pageId;
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

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(final Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(final Integer maxLength) {
        this.maxLength = maxLength;
    }
}
