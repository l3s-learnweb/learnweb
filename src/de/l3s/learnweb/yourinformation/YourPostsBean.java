package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.forum.ForumPost;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
* PostsBean is responsible for displaying user courses.
* */
@Named
@ViewScoped
public class YourPostsBean extends YourGeneralInfoBean implements Serializable {
    private static final Logger logger = Logger.getLogger(YourPostsBean.class);

    public YourPostsBean(){ }

    public List<ForumPost> getUserPosts()
    {
        try{
            List<ForumPost> userPosts = this.getUser().getForumPosts();

            for(ForumPost post:userPosts) {
                post.setText(Jsoup.parse(post.getText()).text());
            }

            return userPosts;
        } catch(SQLException sqlException){
            logger.error("Could not properly retrieve user posts." + sqlException);
            return new ArrayList<>();
        }
    }
}

