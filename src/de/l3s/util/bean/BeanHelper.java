package de.l3s.util.bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeSet;

import jakarta.faces.model.SelectItem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;
import org.primefaces.model.CheckboxTreeNode;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.LanguageBundle;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.util.Misc;

public final class BeanHelper {
    private static final Logger log = LogManager.getLogger(BeanHelper.class);

    private static final List<Locale> supportedLocales = Collections.synchronizedList(new ArrayList<>());

    /**
     * @return Supported frontend locales as defined in faces-config.xml
     */
    public static List<Locale> getSupportedLocales() {
        if (supportedLocales.isEmpty()) {
            supportedLocales.addAll(Faces.getSupportedLocales());
        }
        return Collections.unmodifiableList(supportedLocales);
    }

    public static boolean isMessageExists(String msgKey) {
        return LanguageBundle.getBundle(Faces.getLocale()).containsKey(msgKey);
    }

    public static String getMessageOrDefault(String msgKey, String msgDefault) {
        LanguageBundle bundle = LanguageBundle.getBundle(Faces.getLocale());
        return bundle.containsKey(msgKey) ? bundle.getFormatted(msgKey) : msgDefault;
    }

    /**
     * Converts a list of Locales to a list of SelectItems. The Locales are translated to the current frontend language
     */
    public static List<SelectItem> getLocalesAsSelectItems(List<Locale> locales, LanguageBundle bundle) {
        ArrayList<SelectItem> selectItems = new ArrayList<>(locales.size());

        for (Locale locale : locales) {
            selectItems.add(new SelectItem(locale, bundle.getString("language_" + locale.getLanguage())));
        }
        selectItems.sort(Misc.SELECT_ITEM_LABEL_COMPARATOR);

        return selectItems;
    }

    public static List<SelectItem> getLanguagesAsSelectItems(String[] languages, LanguageBundle bundle) {
        ArrayList<SelectItem> selectItems = new ArrayList<>(languages.length);

        for (String language : languages) {
            selectItems.add(new SelectItem(language, bundle.getString("language_" + language)));
        }
        selectItems.sort(Misc.SELECT_ITEM_LABEL_COMPARATOR);

        return selectItems;
    }

    public static TreeNode<String> createGroupsUsersTree(final User user, final boolean includeUsers) {
        TreeNode<String> root = new CheckboxTreeNode<>();

        for (Course course : user.getCourses()) {
            TreeNode<Course> courseNode = new CheckboxTreeNode<>("course", course, root);

            for (Group group : course.getGroups()) {
                TreeNode<Group> groupNode = new CheckboxTreeNode<>("group", group, courseNode);

                if (includeUsers) {
                    for (User member : group.getMembers()) {
                        new CheckboxTreeNode<>("user", member, groupNode); // create userNode
                    }
                }
            }
        }

        return root;
    }

    public static Collection<Integer> getSelectedUsers(final TreeNode[] selectedNodes) {
        // Set is used to make sure that every user gets the message only once
        TreeSet<Integer> selectedUsers = new TreeSet<>();
        if (selectedNodes != null && selectedNodes.length > 0) {
            for (TreeNode node : selectedNodes) {
                if ("user".equals(node.getType()) && node.getData() instanceof User) {
                    selectedUsers.add(((User) node.getData()).getId());
                }
            }
        }
        return selectedUsers;
    }

    public static Collection<Integer> getSelectedGroups(final TreeNode[] selectedNodes) {
        // Set is used to make sure that every user gets the message only once
        TreeSet<Integer> selectedGroups = new TreeSet<>();
        if (selectedNodes != null && selectedNodes.length > 0) {
            for (TreeNode node : selectedNodes) {
                if ("group".equals(node.getType()) && node.getData() instanceof Group) {
                    selectedGroups.add(((Group) node.getData()).getId());
                }
            }
        }
        return selectedGroups;
    }

    /**
     * @return some attributes of the current http request like url, referrer, ip etc.
     */
    public static String getRequestSummary() {
        return getRequestSummary(Faces.getRequest());
    }

    /**
     * @return some attributes of a request like url, referrer, ip etc.
     */
    public static String getRequestSummary(HttpServletRequest request) {
        StringJoiner joiner = new StringJoiner("; ", "[", "]");

        try {
            // a space after urls is required to avoid adding semicolon to the url by IDEs and mail clients when they parse content
            joiner.add("page: " + Servlets.getRequestURLWithQueryString(request) + " ");
            joiner.add("referrer: " + request.getHeader("referer") + " ");

            joiner.add("ip: " + Servlets.getRemoteAddr(request));
            joiner.add("userAgent: " + request.getHeader("User-Agent"));

            joiner.add("parameters: " + printMap(request.getParameterMap()));

            HttpSession session = request.getSession(false);
            if (session != null) {
                Integer userId = (Integer) session.getAttribute("learnweb_user_id");

                if (userId != null && userId != 0) {
                    joiner.add("userId: " + userId);
                    try {
                        joiner.add("username: " + Beans.getInstance(UserDao.class).findById(userId).map(User::getRealUsername).orElse(null));
                    } catch (Exception e) {
                        log.error("Unable to retrieve username", e);
                    }
                }
            }
        } catch (Throwable e) {
            log.error("An error occurred during gathering request summary", e);
        }

        return joiner.toString();
    }

    private static String printMap(Map<String, String[]> map) {
        if (map == null) {
            return null;
        }

        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            String value = entry.getKey().contains("password") ? "XXX replaced XXX" : String.join("; ", entry.getValue());
            joiner.add(entry.getKey() + "=[" + value + "]");
        }
        return joiner.toString();
    }
}
