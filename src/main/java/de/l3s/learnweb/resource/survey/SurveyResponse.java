package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.StringJoiner;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.user.User;
import de.l3s.util.HasId;

/**
 * This class represents the answers of a single user for a survey resources.
 */
public class SurveyResponse implements Serializable, HasId {
    @Serial
    private static final long serialVersionUID = -1442011853436353323L;
    private static final String separator = "|||";
    private static final String escapedSeparator = "|I|";

    private int id;
    private final int resourceId;
    private int userId;
    private boolean submitted; // has the user submitted the survey finally
    private LocalDateTime createdAt;
    private final HashMap<Integer, String> answers = new LinkedHashMap<>(); // simple answers
    private final HashMap<Integer, String[]> multipleAnswers = new LinkedHashMap<>(); // used for questions that allow multiple answers

    // cache
    private transient User user;
    private transient SurveyResource resource;

    public SurveyResponse(int resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public int getResourceId() {
        return resourceId;
    }

    public SurveyResource getResource() {
        if (null == resource && resourceId != 0) {
            resource = Learnweb.dao().getSurveyDao().findResourceByIdOrElseThrow(resourceId);
        }
        return resource;
    }

    public void setUserId(final int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    public User getUser() {
        if (null == user && userId != 0) {
            user = Learnweb.dao().getUserDao().findByIdOrElseThrow(userId);
        }
        return user;
    }

    public String getAnswer(int id) {
        if (answers.containsKey(id)) {
            return answers.get(id);
        }

        if (multipleAnswers.containsKey(id)) {
            return String.join(", ", multipleAnswers.get(id));
        }

        return "Unanswered";
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public HashMap<Integer, String> getAnswers() {
        return answers;
    }

    public HashMap<Integer, String[]> getMultipleAnswers() {
        return multipleAnswers;
    }

    public static String joinAnswers(String[] answers) {
        if (ArrayUtils.isEmpty(answers)) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(separator);
        for (String s : answers) {
            joiner.add(s.replace(separator, escapedSeparator));
        }
        return joiner.toString();
    }

    public static String[] splitAnswers(String answers) {
        if (StringUtils.isBlank(answers)) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }

        return Arrays.stream(answers.split(separator)).map(s -> s.replace(escapedSeparator, separator)).toArray(String[]::new);
    }
}
