package de.l3s.util;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

public class StringHelper
{
    private static final Logger log = Logger.getLogger(StringHelper.class);

    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\n");
    private static final Pattern NOT_ALPHABETICAL_PATTERN = Pattern.compile("[^<\"\'a-zA-Z]+");

    public static String removeNewLines(String str)
    {
        return NEW_LINE_PATTERN.matcher(str).replaceAll(" ");
    }

    public static String trimNotAlphabetical(final String str)
    {
        Matcher matcher = NOT_ALPHABETICAL_PATTERN.matcher(str);
        if(matcher.lookingAt())
        {
            final String result = str.substring(matcher.end());
            log.info("trimNotAlphabetical: '" + str + "' - '" + result + "'"); // for test, I want to know why we need it
            return result;
        }

        return str;
    }

    /**
     * If the string is longer than maxLength it is split at the nearest blank space
     */
    public static String shortnString(String str, int maxLength)
    {
        if(maxLength < 3) throw new IllegalArgumentException("maxLength must be greater than 3");
        if(null == str) return "";

        if(str.length() > maxLength)
        {
            int endIdx = maxLength - 3;
            while(endIdx > 0 && str.charAt(endIdx) != ' ' && str.charAt(endIdx) != '\n')
                endIdx--;

            str = str.substring(0, endIdx) + "...";
        }
        return str;
    }

    public static String[] remove(final String[] values, final char remove)
    {
        for (int i = 0; i < values.length; ++i)
        {
            values[i] = StringUtils.remove(values[i], remove);
        }
        return values;
    }

    public static List<String> remove(final List<String> values, final char remove)
    {
        for (int i = 0, l = values.size(); i < l; ++i)
        {
            values.set(i, StringUtils.remove(values.get(i), remove));
        }
        return values;
    }

    /**
     * Uses Integer.parseInt but instead of an exception it returns -1 if the input can not be parsed
     *
     * @param number string input
     * @return int value or -1 if can't be parsed
     */
    public static int parseInt(String number)
    {
        return parseInt(number, -1);
    }

    public static int parseInt(String number, int defaultValue)
    {
        try
        {
            return Integer.parseInt(number);
        }
        catch(NumberFormatException e)
        {
            // ignore
        }

        return defaultValue;
    }

    public static String getDomainName(String url)
    {
        int index = url.indexOf('?');
        if(index > 0) // remove parameters they can contain illegal characters
            url = url.substring(0, index);

        try
        {
            URL uri = new URL(url);
            return uri.getHost();
        }
        catch(MalformedURLException e)
        {
            log.error("Can't get domain for url: " + url, e);
            return null;
        }
    }

    public static boolean empty(String str)
    {
        if(null == str)
            return true;
        if(str.length() == 0)
            return true;
        return false;
    }

    public static String implode(Iterable<String> list, String delimiter)
    {
        StringBuilder out = new StringBuilder();
        for(String item : list)
        {
            if(out.length() != 0)
                out.append(delimiter);
            out.append(item);
        }
        return out.toString();
    }

    /**
     * TODO: should be replaced by StringUtils.join
     * @param list
     * @param delimiter
     * @return
     */
    public static String implodeInt(Collection<Integer> list, String delimiter)
    {
        StringBuilder out = new StringBuilder();
        for(Integer item : list)
        {
            if(out.length() != 0)
                out.append(delimiter);
            out.append(item.toString());
        }
        return out.toString();
    }

    public static String implodeInt(int[] list, String delimiter)
    {
        StringBuilder out = new StringBuilder();
        for(int item : list)
        {
            if(out.length() != 0)
                out.append(delimiter);
            out.append(item);
        }
        return out.toString();
    }

    public static String join(Collection<Locale> collection)
    {
        return collection.stream()
                .map(Locale::toLanguageTag)
                .collect(Collectors.joining(","));
    }

    public static List<Locale> splitLocales(String input)
    {
        ArrayList<Locale> locales = new ArrayList<>();

        if(StringUtils.isEmpty(input))
        {
            return locales;
        }

        String[] entries = input.split(",");

        for(String entry : entries)
        {
            locales.add(Locale.forLanguageTag(entry));
        }

        return locales;
    }

    /**
     * Make first character upper case
     */
    public static String ucFirst(String input)
    {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    /**
     * Translates a string into application/x-www-form-urlencoded format using a specific encoding scheme. <br/>
     * This method uses UTF-8. <br/>
     * It's just a convenience method to get rid of the UnsupportedEncodingException.
     */
    public static String urlEncode(String str)
    {
        if(null == str) return "";
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    public static String urlDecode(String str)
    {
        if(null == str) return "";
        return URLDecoder.decode(str, StandardCharsets.UTF_8);
    }

    /**
     * Returns true if the given string contains only ASCII characters
     */
    public static boolean isASCII(CharSequence sequence)
    {
        for(int i = sequence.length() - 1; i >= 0; i--)
        {
            if(sequence.charAt(i) > '\u007f')
            {
                return false;
            }
        }
        return true;
    }

    public static String filenameChangeExt(String originalFilename, String newExt)
    {
        return originalFilename.substring(0, originalFilename.lastIndexOf('.')) + "." + newExt;
    }

    public static String getNameFromPath(String originalFilepath)
    {
        if(originalFilepath == null)
        {
            return null;
        }

        int lastUnixPos = originalFilepath.lastIndexOf('/');
        int lastWindowsPos = originalFilepath.lastIndexOf('\\');
        int index = Math.max(lastUnixPos, lastWindowsPos);
        return originalFilepath.substring(index + 1);
    }

    public static String decodeBase64(String encoded)
    {
        //decode byte array
        byte[] decoded = Base64.decodeBase64(encoded.getBytes());
        //byte to string and return it
        return new String(decoded);
    }

    public static String encodeBase64(byte[] bytes)
    {
        //decode byte array
        byte[] encoded = Base64.encodeBase64(bytes);
        //byte to string and return it
        return new String(encoded);
    }

    /**
     * Converts a string of hexadecimal characters into a byte array.
     *
     * @param hex the hex string
     * @return the hex string decoded into a byte array
     */
    public static byte[] fromHex(String hex)
    {
        byte[] binary = new byte[hex.length() / 2];
        for(int i = 0; i < binary.length; i++)
        {
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
    public static String toHex(byte[] array)
    {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0)
            return String.format("%0" + paddingLength + "d", 0) + hex;
        else
            return hex;
    }

    public static String getDurationInMinutes(int duration)
    {
        int rest = duration % 60;
        StringBuilder str = new StringBuilder();
        str.append((duration - rest) / 60);
        str.append(':');
        str.append(rest);
        if(rest < 10)
            str.append('0');
        return str.toString();
    }

    public static String formatDuration(Duration d)
    {
        long hours = d.toHours();
        long minutes = d.minusHours(hours).toMinutes();

        StringBuilder output = new StringBuilder();
        if(hours > 0)
        {
            output.append(hours);
            output.append("h ");
        }

        output.append(minutes);
        if(minutes < 10)
            output.append('0');
        output.append("m");

        return output.toString();
    }

    /**
     * Like Jsoup.clean but it preserves line breaks and spacing
     *
     * @param html
     * @param whitelist for example: Whitelist.none()
     * @return
     */
    public static String clean(String html, Whitelist whitelist)
    {
        if(html == null)
            return html;
        Document document = Jsoup.parse(html);
        document.outputSettings(new Document.OutputSettings().prettyPrint(false));//makes html() preserve line breaks and spacing
        document.select("br").append("\\n");
        document.select("p").prepend("\\n\\n");
        String s = document.html().replaceAll("\\\\n", "\n");
        return Jsoup.clean(s, "", whitelist, new Document.OutputSettings().prettyPrint(false));
    }
}
