package de.l3s.util.bean;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

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
    /**
     * Languages that can be used in glossaries in addition to English and the frontend locales.
     */
    private static final List<Locale> EXTRA_GLOSSARY_LOCALES = Stream.of(
        "ar", "el", "fr", "nl", "ru", "sv", "zh", "fa", "ur", "pa", "ps", "ha", "yo", "ig"
    ).map(Locale::of).toList();

    // initialized lazily because they need a FacesContext; threads racing on the first call compute the same immutable value
    private static volatile List<Locale> supportedLocales;
    private static volatile Set<Locale> supportedGlossaryLocales;

    /**
     * @return Supported frontend locales as defined in faces-config.xml
     */
    public static List<Locale> getSupportedLocales() {
        List<Locale> locales = supportedLocales;
        if (locales == null) {
            locales = Collections.unmodifiableList(new ArrayList<>(Faces.getSupportedLocales())); // not List.copyOf, callers may ask contains(null)
            supportedLocales = locales;
        }
        return locales;
    }

    public static Set<Locale> getSupportedGlossaryLocales() {
        Set<Locale> locales = supportedGlossaryLocales;
        if (locales == null) {
            LinkedHashSet<Locale> glossaryLocales = new LinkedHashSet<>();
            glossaryLocales.add(Locale.of("en"));
            glossaryLocales.addAll(getSupportedLocales());
            glossaryLocales.addAll(EXTRA_GLOSSARY_LOCALES);

            locales = Collections.unmodifiableSequencedSet(glossaryLocales);
            supportedGlossaryLocales = locales;
        }
        return locales;
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
        return locales.stream()
            .filter(locale -> !locale.getDisplayLanguage(inLocale).isEmpty())
            .map(locale -> new SelectItem(locale, StringUtils.capitalize(locale.getDisplayLanguage(inLocale))))
            .sorted(Misc.SELECT_ITEM_LABEL_COMPARATOR).toList();
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
                if ("user".equals(node.getType()) && node.getData() instanceof User user) {
                    selectedUsers.add(user.getId());
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
                if ("group".equals(node.getType()) && node.getData() instanceof Group group) {
                    selectedGroups.add(group.getId());
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
