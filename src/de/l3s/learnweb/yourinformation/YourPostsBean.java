package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.forum.ForumPost;
import org.jsoup.Jsoup;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*
* PostsBean is responsible for displaying user courses.
* */
@ManagedBean(name = "yourPostsBean", eager = true)
@SessionScoped
public class YourPostsBean extends YourGeneralInfoBean {
    private List<ForumPost> userPosts;

    public YourPostsBean(){
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

