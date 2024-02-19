package de.l3s.learnweb.resource.survey;

@FunctionalInterface
public interface SurveyAnswerHandler {
    void onQuestionAnswer(SurveyQuestion question);
}
