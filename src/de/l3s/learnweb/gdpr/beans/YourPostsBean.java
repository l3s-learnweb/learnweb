package de.l3s.learnweb.gdpr;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.forum.ForumManager;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;

import de.l3s.learnweb.forum.ForumPost;

/**
* PostsBean is responsible for displaying user courses.
* */
@Named
@ViewScoped
public class YourPostsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 4437146430672930717L;
    private static final Logger log = Logger.getLogger(YourPostsBean.class);

    private List<ForumPost> userPosts;
    private Map<Integer, String> postThreadTopics;

    public YourPostsBean() throws SQLException
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        final ForumManager forumManager = this.getLearnweb().getForumManager();
        postThreadTopics = new HashMap<>();

        this.userPosts = user.getForumPosts();
        for(ForumPost post : userPosts)
        {
            post.setText(Jsoup.parse(post.getText()).text());
            postThreadTopics.put(post.getTopicId(), forumManager.getTopicById(post.getTopicId()).getTitle());
        }
    }

    public List<ForumPost> getUserPosts()
    {
        return this.userPosts;
    }

    public Map<Integer, String> getPostThreadTopics()
    {
        return postThreadTopics;
    }
}
