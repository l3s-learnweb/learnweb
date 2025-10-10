package de.l3s.util.bean;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import jakarta.faces.model.SelectItem;
import jakarta.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.omnifaces.util.Exceptions;
import org.omnifaces.util.Faces;
import org.primefaces.model.CheckboxTreeNode;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.i18n.MessagesBundle;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.User;
import de.l3s.util.Misc;

public final class BeanHelper {
    private static final List<Locale> supportedLocales = Collections.synchronizedList(new ArrayList<>());
    private static final Set<Locale> supportedGlossaryLocales = Collections.synchronizedSet(new HashSet<>());

    /**
     * @return Supported frontend locales as defined in faces-config.xml
     */
    public static List<Locale> getSupportedLocales() {
        if (supportedLocales.isEmpty()) {
            supportedLocales.addAll(Faces.getSupportedLocales());
        }
        return Collections.unmodifiableList(supportedLocales);
    }

    public static Set<Locale> getSupportedGlossaryLocales() {
        if (supportedGlossaryLocales.isEmpty()) {
            supportedGlossaryLocales.add(Locale.of("en"));
            supportedGlossaryLocales.addAll(getSupportedLocales());
            supportedGlossaryLocales.add(Locale.of("ar"));
            supportedGlossaryLocales.add(Locale.of("el"));
            supportedGlossaryLocales.add(Locale.of("fr"));
            supportedGlossaryLocales.add(Locale.of("nl"));
            supportedGlossaryLocales.add(Locale.of("ru"));
            supportedGlossaryLocales.add(Locale.of("sv"));
            supportedGlossaryLocales.add(Locale.of("zh"));
        }
        return Collections.unmodifiableSet(supportedGlossaryLocales);
    }

    public static boolean isMessageExists(String msgKey) {
        return MessagesBundle.of(Faces.getLocale()).containsKey(msgKey);
    }

    public static String getMessageOrDefault(String msgKey, String msgDefault) {
        ResourceBundle bundle = MessagesBundle.of(Faces.getLocale());
        return bundle.containsKey(msgKey) ? bundle.getString(msgKey) : msgDefault;
    }

    /**
     * Converts a list of Locales to a list of SelectItems. The Locales are translated to the current frontend language
     */
    public static List<SelectItem> getLocalesAsSelectItems(Collection<Locale> locales, Locale inLocale) {
        ArrayList<SelectItem> selectItems = new ArrayList<>(locales.size());

        for (Locale locale : locales) {
            if (!locale.getDisplayLanguage(inLocale).isEmpty()) {
                selectItems.add(new SelectItem(locale, StringUtils.capitalize(locale.getDisplayLanguage(inLocale))));
            }
        }
        selectItems.sort(Misc.SELECT_ITEM_LABEL_COMPARATOR);

        return selectItems;
    }

    public static TreeNode<?> createHierarchicalGroupsTree(final User user, final boolean includeUsers) {
        TreeNode<Object> root = new CheckboxTreeNode<>();

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

    public static TreeSet<Integer> getSelectedUsers(final TreeNode<?>[] selectedNodes) {
        // Set is used to make sure that every user gets the message only once
        TreeSet<Integer> selectedUsers = new TreeSet<>();
        if (selectedNodes != null) {
            for (TreeNode<?> node : selectedNodes) {
                if ("user".equals(node.getType()) && node.getData() instanceof User) {
                    selectedUsers.add(((User) node.getData()).getId());
                }
            }
        }
        return selectedUsers;
    }

    public static Collection<Integer> getSelectedGroups(final TreeNode<?>[] selectedNodes) {
        // Set is used to make sure that every user gets the message only once
        TreeSet<Integer> selectedGroups = new TreeSet<>();
        if (selectedNodes != null) {
            for (TreeNode<?> node : selectedNodes) {
                if ("group".equals(node.getType()) && node.getData() instanceof Group) {
                    selectedGroups.add(((Group) node.getData()).getId());
                }
            }
        }
        return selectedGroups;
    }

    @SuppressWarnings("UseOfObsoleteDateTimeApi")
    public static Date date(final LocalDateTime localDateTime) {
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

    public static Throwable unwrap(Throwable exception) {
        if (exception == null) {
            return null;
        }

        return Exceptions.unwrap(exception, ServletException.class);
    }
}
