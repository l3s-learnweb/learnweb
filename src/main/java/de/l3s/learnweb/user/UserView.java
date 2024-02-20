package de.l3s.learnweb.user;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.l3s.learnweb.group.Group;
import de.l3s.util.Misc;

/**
 * This class shall be used to cache computationally intensive attributes of a user.<br/>
 * It is intended to be used in viewscoped beans.
 *
 * @author Kemkes
 *
 */
public final class UserView implements Serializable {

    @Serial
    private static final long serialVersionUID = 5664039620488069850L;

    private final User user;

    // caches
    private transient String groupsTitles;
    private String coursesTitles;

    private UserView(User user) {
        Objects.requireNonNull(user);
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    // (additional) computationally "intensive" attributes

    /**
     * @return Concatenated titles of all groups the user belongs to
     */
    public String getGroupsTitles() {
        if (null == groupsTitles) {
            groupsTitles = user.getGroups().stream().map(Group::getTitle).sorted().collect(Collectors.joining(", "));
        }

        return groupsTitles;
    }

    public String getCoursesTitles() {
        if (null == coursesTitles) {
            coursesTitles = user.getCourses().stream().map(Course::getTitle).sorted().collect(Collectors.joining(", "));
        }

        return coursesTitles;
    }

    // Convenience methods that redirect to user

    public int getId() {
        return user.getId();
    }

    public String getUsername() {
        return user.getUsername();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public LocalDateTime getLastLoginDate() {
        return user.getLastLoginDate();
    }

    public ZoneId getTimeZone() {
        return user.getTimeZone();
    }

    public String getStudentId() {
        return user.getStudentId();
    }

    public Organisation getOrganisation() {
        return user.getOrganisation();
    }

    public boolean isAdmin() {
        return user.isAdmin();
    }

    public boolean isModerator() {
        return user.isModerator();
    }

    // factory methods

    public static UserView of(User user) {
        return new UserView(user);
    }

    /**
     * Converts the given user list to a list of user views.
     *
     * @param preloadFields These methods will be called once asynchronously. This can be used to preload values if the methods cache them internally
     */
    @SafeVarargs
    public static List<UserView> of(List<User> users, Function<UserView, ?>... preloadFields) {
        Objects.requireNonNull(users);

        List<UserView> userViews = users.stream().map(UserView::of).toList();

        if (preloadFields.length > 0 && !users.isEmpty()) { // preload specified fields
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                Misc.sleep(2000); // sleep so that the page gets loaded asap

                for (UserView uv : userViews) {
                    for (Function<UserView, ?> field : preloadFields) {
                        field.apply(uv);
                    }
                }
            });
        }

        return userViews;
    }
}
