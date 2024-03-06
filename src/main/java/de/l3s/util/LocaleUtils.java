package de.l3s.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.omnifaces.util.Faces;

public final class LocaleUtils {
    private static final char UNDERSCORE = '_';
    private static final char DASH = '-';
    private static final List<Locale> supportedLocales = Collections.synchronizedList(new ArrayList<>());

    public static Locale toLocale(final String str) {
        try {
            ParsedLocale parsed = parseLocale(str);
            if (parsed == null) {
                return Locale.ROOT;
            }
            if (StringUtils.isNumeric(parsed.variant)) {
                return new Locale(parsed.language, parsed.country, parsed.variant);
            }
            return new Locale(parsed.language);
        } catch (IllegalArgumentException e) {
            return Locale.ROOT;
        }
    }

    /**
     * @return Supported frontend locales as defined in faces-config.xml
     */
    public static List<Locale> getSupportedLocales() {
        if (supportedLocales.isEmpty()) {
            supportedLocales.addAll(Faces.getSupportedLocales());
        }
        return Collections.unmodifiableList(supportedLocales);
    }

    public static boolean isAvailableLocale(final Locale locale) {
        return supportedLocales.isEmpty() || supportedLocales.contains(locale); // fallback to `true` if not initialized
    }

    public static String toLanguage(Locale locale) {
        if (StringUtils.isEmpty(locale.getCountry())) {
            return locale.getLanguage();
        }
        return locale.getLanguage() + UNDERSCORE + locale.getCountry();
    }

    private static ParsedLocale parseLocale(final String str) {
        if (str == null) {
            return null;
        }

        int len = str.length();
        if (len != 2 && len != 5 && len < 7) {
            throw new IllegalArgumentException("Invalid locale format: " + str);
        }

        char ch0 = str.charAt(0);
        char ch1 = str.charAt(1);
        if (ch0 < 'a' || ch0 > 'z' || ch1 < 'a' || ch1 > 'z') {
            throw new IllegalArgumentException("Invalid locale format: " + str);
        }

        if (len == 2) {
            return new ParsedLocale(str, "", "");
        } else {
            char ch2 = str.charAt(2);
            if (ch2 != UNDERSCORE && ch2 != DASH) {
                throw new IllegalArgumentException("Invalid locale format: " + str);
            }

            char ch3 = str.charAt(3);
            if (ch3 == UNDERSCORE || ch3 == DASH) {
                return new ParsedLocale(str.substring(0, 2), "", str.substring(4));
            }

            char ch4 = str.charAt(4);
            if (ch3 < 'A' || ch3 > 'Z' || ch4 < 'A' || ch4 > 'Z') {
                throw new IllegalArgumentException("Invalid locale format: " + str);
            }

            if (len == 5) {
                return new ParsedLocale(str.substring(0, 2), str.substring(3, 5), "");
            } else {
                char ch5 = str.charAt(5);
                if (ch5 != UNDERSCORE && ch5 != DASH) {
                    throw new IllegalArgumentException("Invalid locale format: " + str);
                }

                return new ParsedLocale(str.substring(0, 2), str.substring(3, 5), str.substring(6));
            }
        }
    }

    public static Locale getCoreLocale(final Locale locale) {
        ParsedVariant parsed = parseVariant(locale.getVariant());
        if (parsed == null) {
            return new Locale(locale.getLanguage(), locale.getCountry());
        }
        return new Locale(locale.getLanguage(), locale.getCountry(), parsed.audience);
    }

    public static Long getCompanyId(final Locale locale) {
        ParsedVariant parsed = parseVariant(locale.getVariant());
        if (parsed == null) {
            return null;
        }
        return parsed.companyId;
    }

    /**
     * This method supposes that variant contains parts separated by underscore.
     * Parts could be:
     * - audience (e.g. "school")
     * - companyId (e.g. "123")
     */
    private static ParsedVariant parseVariant(final String str) {
        if (str == null) {
            return null;
        }

        String[] parts = str.split("_");
        if (parts.length == 2) {
            try {
                return new ParsedVariant(parts[0], Long.parseLong(parts[1]));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return new ParsedVariant(str, null);
    }

    private record ParsedLocale(String language, String country, String variant) {
    }

    private record ParsedVariant(String audience, Long companyId) {
    }
}
