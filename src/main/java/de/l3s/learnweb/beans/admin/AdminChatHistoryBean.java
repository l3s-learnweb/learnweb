package de.l3s.learnweb.beans.admin;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import de.l3s.interweb.client.Interweb;
import de.l3s.interweb.client.InterwebException;
import de.l3s.interweb.core.chat.Conversation;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.survey.SurveyDao;
import de.l3s.learnweb.resource.survey.SurveyQuestion;
import de.l3s.learnweb.resource.survey.SurveyResponse;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.OrganisationDao;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;

@Named
@ViewScoped
public class AdminChatHistoryBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -4815509777068373043L;

    // params
    private int organisationId;

    private Organisation organisation;
    private Interweb interweb;
    private transient Conversation selectedConv;
    private transient List<Conversation> conversations;

    private static final Cache<Integer, Optional<SurveyQuestion>> questionCache = Caffeine.newBuilder().maximumSize(1000).build();
    private static final Cache<Integer, Optional<SurveyResponse>> responseCache = Caffeine.newBuilder().maximumSize(1000).build();

    @Inject
    private OrganisationDao organisationDao;

    @Inject
    private UserDao userDao;

    @Inject
    private SurveyDao surveyDao;

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());
        interweb = getLearnweb().getInterweb();

        if (organisationId != 0) {
            BeanAssert.hasPermission(getUser().isAdmin());
            organisation = organisationDao.findByIdOrElseThrow(organisationId);
        } else {
            BeanAssert.hasPermission(getUser().isModerator());
            organisation = getUser().getOrganisation(); // by default, edit the user's organisation
        }
    }

    public Conversation getSelectedConv() {
        return selectedConv;
    }

    public void setSelectedConv(Conversation selectedConv) throws InterwebException {
        this.selectedConv = interweb.chatById(selectedConv.getId().toString());
        this.selectedConv.setUser(selectedConv.getUser());
    }

    public List<Conversation> getConversations() throws InterwebException {
        if (conversations == null) {
            conversations = interweb.chatAll();
        }
        return conversations;
    }

    public User getUserById(String userId) {
        if (userId == null || userId.isEmpty() || !userId.matches("\\d+")) {
            return null;
        }
        return userDao.findById(Integer.parseInt(userId)).orElse(null);
    }

    public SurveyResponse getResponseByMessageId(int messageId) {
        return responseCache.get(messageId, k -> Optional.ofNullable(surveyDao.findResponseByMessageId(k))).orElse(null);
    }

    public SurveyQuestion getQuestionById(int questionId) {
        return questionCache.get(questionId, k -> surveyDao.findQuestionById(k)).orElse(null);
    }

    public Organisation getOrganisation() {
        return organisation;
    }

    public int getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(int organisationId) {
        this.organisationId = organisationId;
    }
}
