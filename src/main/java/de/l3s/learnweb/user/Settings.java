package de.l3s.learnweb.user;

import java.lang.reflect.Type;

public enum Settings {
    chat_feedback_prompt_survey_page_id(Integer.class, null),
    chat_feedback_response_survey_page_id(Integer.class, null);

    private final Type type;
    private final String defaultValue;

    Settings(final Type type, final String defaultValue) {
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public Type getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
