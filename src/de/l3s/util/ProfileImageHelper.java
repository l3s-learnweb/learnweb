package de.l3s.util;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public final class ProfileImageHelper {
    // private static final Logger log = LogManager.getLogger(ImageHelper.class);

    private static final Pattern NAME_SEPARATOR = Pattern.compile("[\\s.]+");

    public static String getProfilePicture(String name) {
        return getProfilePicture(getInitialsForProfilePicture(name), getColorForProfilePicture(name));
    }

    public static String getProfilePicture(String initials, String color) {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"200px\" height=\"200px\" viewBox=\"0 0 200 200\""
            + " version=\"1.1\"><rect fill=\"" + color + "\" cx=\"100\" width=\"200\" height=\"200\" cy=\"100\" r=\"100\"></rect><text x=\"50%\" y=\"50%\""
            + " style=\"color: #fff; line-height: 1;font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Fira Sans',"
            + " 'Droid Sans', 'Helvetica Neue', sans-serif;\" alignment-baseline=\"middle\" text-anchor=\"middle\" font-size=\"88\" font-weight=\"400\""
            + " dy=\".1em\" dominant-baseline=\"middle\" fill=\"#fff\">" + initials + "</text>"
            + "</svg>";
    }

    /**
     * Returns a color associated to the user.
     * If the user is registered this method will return always the same color otherwise a "random" color is returned.
     *
     * @return RGB color code without hash prefix
     */
    public static String getColorForProfilePicture(String name) {
        return ColorHelper.getColor(name.hashCode());
    }

    /**
     * Returns initials of the user using full name or username.
     *
     * @return 1-2 letters (or numbers if no letters in username) string
     */
    public static String getInitialsForProfilePicture(String name) {
        // if less than 3 characters, use as is
        if (name.length() < 3) {
            return name;
        }

        // happens when users use their student id as name
        if (StringUtils.isNumeric(name)) {
            return name.substring(name.length() - 2);
        }

        StringBuilder initials = new StringBuilder();
        if (name.contains(" ") || name.contains(".")) { // name consists of multiple terms separated by whitespaces or dots
            for (String part : NAME_SEPARATOR.split(name)) {
                if (part.isEmpty()) {
                    continue;
                }
                int index = StringUtils.isNumeric(part) ? (part.length() - 1) : 0; // if is number use last digit as initial
                initials.append(part.charAt(index));
            }
        } else if (StringUtils.isMixedCase(name)) {
            initials.append(name.charAt(0)); // always add first char

            for (int i = 1, len = name.length() - 1; i < len; i++) {
                if (Character.isUpperCase(name.charAt(i))) {
                    initials.append(name.charAt(i));
                }
            }
        }

        if (initials.length() == 0) {
            initials.append(name, 0, 2);
        }

        return initials.toString();
    }
}
