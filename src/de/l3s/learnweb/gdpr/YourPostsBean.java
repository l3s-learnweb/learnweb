package de.l3s.learnweb.gdpr;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.forum.ForumPost;
import de.l3s.learnweb.forum.ForumTopic;
import de.l3s.learnweb.forum.ForumTopicDao;
import de.l3s.learnweb.user.User;

/**
 * PostsBean is responsible for displaying user courses.
 */
@Named
@ViewScoped
public class YourPostsBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 4437146430672930717L;
    private static final Logger log = LogManager.getLogger(YourPostsBean.class);

    private List<ForumPost> userPosts;
    private Map<Integer, String> postThreadTopics;

    public YourPostsBean() throws SQLException {
        User user = getUser();
        BeanAssert.authorized(user);

        postThreadTopics = new HashMap<>();
        userPosts = user.getForumPosts();

        for (ForumPost post : userPosts) {
            try {
                StringBuilder allText = new StringBuilder();
                String[] tds = StringUtils.substringsBetween(Jsoup.parse(post.getText()).outerHtml(), "<p>", "</p>");
                if (tds != null) { // for example, when message contains only quotes
                    for (String td : tds) {
                        allText.append(td).append(" ");
                    }
                }
                post.setText(Jsoup.parse(allText.toString()).text());

                Optional<ForumTopic> topic = Learnweb.getInstance().getJdbi().withExtension(ForumTopicDao.class, dao -> dao.getTopicById(post.getTopicId()));
                postThreadTopics.put(post.getTopicId(), topic.get().getTitle());
            } catch (Exception e) {
                log.error("An error occurred during processing post " + post.getId(), e);
            }
        }
    }

    public List<ForumPost> getUserPosts() {
        return this.userPosts;
    }

    public Map<Integer, String> getPostThreadTopics() {
        return postThreadTopics;
    }
}
