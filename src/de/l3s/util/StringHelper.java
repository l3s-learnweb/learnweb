package de.l3s.util;

import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.Collection;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

public class StringHelper
{
    private static final Logger log = Logger.getLogger(StringHelper.class);

    /**
     * If the string is longer than maxLength it is split at the nearest blankspace
     *
     * @param str
     * @param maxLength
     * @return
     */
    public static String shortnString(String str, int maxLength)
    {
        if(maxLength < 3)
        {
            throw new IllegalArgumentException("maxLength must be greater than 3");
        }

        if(null == str)
            return "";

        if(str.length() > maxLength)
        {

            int endIdx = maxLength - 3;
            while(endIdx > 0 && str.charAt(endIdx) != ' ' && str.charAt(endIdx) != '\n')
                endIdx--;

            str = str.substring(0, endIdx) + "...";
        }
        return str;
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

    public static String implode(Iterable<String> list, String delim)
    {
        StringBuilder out = new StringBuilder();
        for(String item : list)
        {
            if(out.length() != 0)
                out.append(delim);
            out.append(item);
        }
        return out.toString();
    }

    public static String implodeInt(Collection<Integer> list, String delim)
    {
        StringBuilder out = new StringBuilder();
        for(Integer item : list)
        {
            if(out.length() != 0)
                out.append(delim);
            out.append(item.toString());
        }
        return out.toString();
    }

    public static String implodeInt(int[] list, String delim)
    {
        StringBuilder out = new StringBuilder();
        for(int item : list)
        {
            if(out.length() != 0)
                out.append(delim);
            out.append(Integer.toString(item));
        }
        return out.toString();
    }

    /**
     * Make first character upper case
     *
     * @param input
     * @return
     */
    public static String ucFirst(String input)
    {
        return input.substring(0, 1).toUpperCase() + input.substring(1, input.length());
    }

    /**
     * Translates a string into application/x-www-form-urlencoded format using a specific encoding scheme. <br/>
     * This method uses UTF-8. <br/>
     * It's just a convenience method to get rid of the UnsupportedEncodingException.
     *
     * @param str
     * @return
     */
    public static String urlEncode(String str)
    {
        if(null == str)
            return "";
        try
        {
            return URLEncoder.encode(str, "UTF-8");
        }
        catch(UnsupportedEncodingException e)
        {
            // should never happen
            throw new RuntimeException(e);
        }
    }

    public static String urlDecode(String str)
    {
        if(null == str)
            return "";
        try
        {
            return URLDecoder.decode(str, "UTF-8");
        }
        catch(UnsupportedEncodingException e)
        {
            // should never happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if the given string contains only ASCII characters
     *
     * @param sequence
     * @return
     */
    public static boolean isASCII(CharSequence sequence)
    {
        for(int i = sequence.length() - 1; i >= 0; i--)
        {
            if(!(sequence.charAt(i) <= '\u007f'))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Encodes the domain using punycode and the query using percent-encoding
     *
     * @param url
     * @return
     * @throws URISyntaxException
     */
    public static String convertUnicodeURLToAscii(String url) throws URISyntaxException
    {
        if(url != null)
        {
            url = url.trim();
            // Handle international domains by detecting non-ascii and converting them to punycode
            if(!isASCII(url))
            {
                URI uri = new URI(url);

                // URI needs a scheme to work properly with authority parsing
                if(uri.getScheme() == null)
                {
                    uri = new URI("http://" + url);
                }

                String scheme = uri.getScheme() != null ? uri.getScheme() + "://" : null;
                String authority = uri.getRawAuthority() != null ? uri.getRawAuthority() : ""; // includes domain and port
                String path = uri.getRawPath() != null ? uri.getRawPath() : "";
                String queryString = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";

                // Must convert domain to punycode separately from the path
                url = scheme + IDN.toASCII(authority) + path + queryString;

                // Convert path from unicode to ascii encoding
                url = new URI(url).toASCIIString();
            }
        }
        return url;
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
     * Like Jsoup.clean but it preserves linebreaks and spacing
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
        document.outputSettings(new Document.OutputSettings().prettyPrint(false));//makes html() preserve linebreaks and spacing
        document.select("br").append("\\n");
        document.select("p").prepend("\\n\\n");
        String s = document.html().replaceAll("\\\\n", "\n");
        return Jsoup.clean(s, "", whitelist, new Document.OutputSettings().prettyPrint(false));
    }
}
