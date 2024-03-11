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
    private int resourceId;
    private int messageId;
    private int userId;
    private boolean submitted; // has the user submitted the survey finally
    private LocalDateTime createdAt;
    private final HashMap<Integer, String> answers = new LinkedHashMap<>(); // simple answers
    private final HashMap<Integer, String[]> multipleAnswers = new LinkedHashMap<>(); // used for questions that allow multiple answers

    // cache
    private transient User user;
    private transient SurveyResource resource;

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

    public void setResourceId(final int resourceId) {
        this.resourceId = resourceId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(final int messageId) {
        this.messageId = messageId;
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

    public boolean isEmpty() {
        return answers.isEmpty() && multipleAnswers.isEmpty();
    }

    public void clear() {
        answers.clear();
        multipleAnswers.clear();
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

    /**
     * @return caution the value will not include multivalued answers
     */
    public HashMap<Integer, String> getAnswers() {
        return answers;
    }

    /**
     * @return caution the value will not include single valued answers
     */
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
