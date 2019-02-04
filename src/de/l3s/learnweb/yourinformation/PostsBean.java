package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.forum.ForumPost;
import org.jsoup.Jsoup;

import javax.inject.Named;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
* PostsBean is responsible for displaying user courses.
* */
@Named
public class PostsBean extends GeneralinfoBean {
    private List<ForumPost> userPosts;

    public PostsBean(){
        try{
            userPosts = user.getForumPosts();

            for(ForumPost post:userPosts) {
                post.setText(Jsoup.parse(post.getText()).text());
            }
        } catch(SQLException sqlException){
            this.userPosts = new ArrayList<>();
            logger.error("Could not properly retrieve user posts." + sqlException);
        }
    }

    public List<ForumPost> getUserPosts()
    {
        return userPosts;
    }

    public void setUserPosts(final List<ForumPost> userPosts)
    {
        this.userPosts = userPosts;
    }
}

