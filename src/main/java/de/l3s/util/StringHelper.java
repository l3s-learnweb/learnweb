package de.l3s.util;

import java.math.BigInteger;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

public final class StringHelper {
    private static final Logger log = LogManager.getLogger(StringHelper.class);

    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\n");
    private static final Pattern NOT_ALPHABETICAL_PATTERN = Pattern.compile("[^<\"'a-zA-Z]+");

    public static String removeNewLines(String str) {
        return NEW_LINE_PATTERN.matcher(str).replaceAll(" ");
    }

    public static String replaceNewLineWithBreak(final String str) {
        if (str == null) {
            return null;
        }

        return str.replace("\n", "<br/>");
    }

    public static String trimNotAlphabetical(final String str) {
        Matcher matcher = NOT_ALPHABETICAL_PATTERN.matcher(str);
        if (matcher.lookingAt()) {
            final String result = str.substring(matcher.end());
            //log.info("trimNotAlphabetical: '" + str + "' - '" + result + "'"); // for test, I want to know why we need it
            return result;
        }

        return str;
    }

    /**
     * If the string is longer than maxLength it is split at the nearest blank space.
     */
    public static String shortnString(String str, int maxLength) {
        if (maxLength < 3) {
            throw new IllegalArgumentException("maxLength must be greater than 3");
        }
        if (null == str) {
            return "";
        }

        if (str.length() > maxLength) {
            int endIdx = maxLength - 3;
            while (endIdx > 0 && str.charAt(endIdx) != ' ' && str.charAt(endIdx) != '\n') {
                endIdx--;
            }

            str = str.substring(0, endIdx) + "...";
        }
        return str;
    }

    /**
     * The same as StringUtils.remove, but removes a char from an array of strings.
     */
    public static String[] remove(final String[] values, final char remove) {
        for (int i = 0, len = values.length; i < len; ++i) {
            values[i] = StringUtils.remove(values[i], remove);
        }
        return values;
    }

    public static List<String> remove(final List<String> values, final char remove) {
        for (int i = 0, len = values.size(); i < len; ++i) {
            values.set(i, StringUtils.remove(values.get(i), remove));
        }
        return values;
    }

    public static String getDomainName(String url) {
        int index = url.indexOf('?');
        if (index > 0) { // remove parameters they can contain illegal characters
            url = url.substring(0, index);
        }

        try {
            URI uri = URI.create(url);
            return uri.getHost();
        } catch (IllegalArgumentException e) {
            log.error("Can't get domain for url: {}", url, e);
            return null;
        }
    }

    public static String join(Collection<Locale> collection) {
        return collection.stream()
            .map(Locale::toLanguageTag)
            .collect(Collectors.joining(","));
    }

    public static ArrayList<Locale> splitLocales(String input) {
        ArrayList<Locale> locales = new ArrayList<>();

        if (StringUtils.isEmpty(input)) {
            return locales;
        }

        String[] entries = input.split(",");

        for (String entry : entries) {
            locales.add(Locale.forLanguageTag(entry));
        }

        return locales;
    }

    /**
     * Translates a string into application/x-www-form-urlencoded format using a specific encoding scheme.
     * This method uses UTF-8.
     * It's just a convenience method to get rid of the UnsupportedEncodingException.
     */
    public static String urlEncode(String str) {
        if (null == str) {
            return "";
        }
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    public static String urlDecode(String str) {
        if (null == str) {
            return "";
        }
        return URLDecoder.decode(str, StandardCharsets.UTF_8);
    }

    /**
     * Returns true if the given string contains only ASCII characters.
     */
    public static boolean isASCII(CharSequence sequence) {
        for (int i = sequence.length() - 1; i >= 0; i--) {
            if (sequence.charAt(i) > '\u007f') {
                return false;
            }
        }
        return true;
    }

    public static String filenameChangeExt(String originalFilename, String newExt) {
        return originalFilename.substring(0, originalFilename.lastIndexOf('.')) + "." + newExt;
    }

    public static String getNameFromPath(String originalFilepath) {
        if (originalFilepath == null) {
            return null;
        }

        int lastUnixPos = originalFilepath.lastIndexOf('/');
        int lastWindowsPos = originalFilepath.lastIndexOf('\\');
        int index = Math.max(lastUnixPos, lastWindowsPos);
        return originalFilepath.substring(index + 1);
    }

    public static String decodeBase64(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    public static String encodeBase64(String str) {
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts a string of hexadecimal characters into a byte array.
     *
     * @param hex the hex string
     * @return the hex string decoded into a byte array
     */
    public static byte[] fromHex(String hex) {
        byte[] binary = new byte[hex.length() / 2];
        for (int i = 0, len = binary.length; i < len; i++) {
            binary[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return binary;
    }

    /**
     * Converts a byte array into a hexadecimal string.
     *
     * @param array the byte array to convert
     * @return a length*2 character string encoding the byte array
     */
    public static String toHex(byte[] array) {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if (paddingLength > 0) {
            return String.format("%0" + paddingLength + "d", 0) + hex;
        } else {
            return hex;
        }
    }

    /**
     * Converts a duration given in seconds to human readable minutes e.g. 90 to 1:30
     *
     * @param duration a duration in seconds
     */
    public static String getDurationInMinutes(int duration) {
        int rest = duration % 60;
        StringBuilder sb = new StringBuilder();
        sb.append((duration - rest) / 60);
        sb.append(':');
        sb.append(rest);
        if (rest < 10) {
            sb.append('0');
        }
        return sb.toString();
    }

    public static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();

        StringBuilder output = new StringBuilder();
        if (hours > 0) {
            output.append(hours);
            output.append("h ");
        }

        output.append(minutes);
        if (minutes < 10) {
            output.append('0');
        }
        output.append("m");

        return output.toString();
    }

    /**
     * Like Jsoup.clean but it preserves line breaks and spacing.
     *
     * @param safelist for example: Safelist.none()
     */
    public static String clean(String html, Safelist safelist) {
        if (html == null) {
            return null;
        }
        Document document = Jsoup.parse(html);
        document.outputSettings(new Document.OutputSettings().prettyPrint(false)); //makes html() preserve line breaks and spacing
        document.select("br").append("\\n");
        document.select("p").prepend("\\n\\n");
        String s = document.html().replaceAll("\\\\n", "\n");
        return Jsoup.clean(s, "", safelist, new Document.OutputSettings().prettyPrint(false));
    }

    /**
     * Works as StringUtils.isBlank but in addition it ignores all HTML tags. E.g. "&lt;p&gt; &lt;/p&gt;" will return true
     */
    public static boolean isBlankDisregardingHTML(String text) {
        if (StringUtils.isBlank(text)) {
            return true;
        }

        text = clean(text, Safelist.none());

        return StringUtils.isBlank(text);
    }

    /**
     * Add HTML bold tags around the query if it is present in the given str.
     */
    public static String highlightQuery(String str, String query) {
        String strLowered = str.toLowerCase();

        int index = strLowered.indexOf(query.toLowerCase());

        StringBuilder result = new StringBuilder();
        result.append(str);
        if (index != -1) {
            result.insert(index + query.length(), "</b>");
            result.insert(index, "<b>");
        }
        return result.toString();
    }
}
