package de.l3s.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.codec.binary.Base64;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.ocpsoft.prettytime.PrettyTime;

public class StringHelper
{
    /**
     * If the string is longer than maxLength it is split at the nearest blankspace
     * 
     * @param str
     * @param maxLength
     * @return
     */
    public static String shortnString(String str, int maxLength)
    {
	if(null == str)
	    return "";

	if(str.length() > maxLength)
	{

	    int endIdx = maxLength;
	    while(endIdx > 0 && str.charAt(endIdx) != ' ' && str.charAt(endIdx) != '\n')
		endIdx--;

	    str = str.substring(0, endIdx) + "...";
	}
	return str;
    }

    public static boolean empty(String str)
    {
	if(null == str)
	    return true;
	if(str.length() == 0)
	    return true;
	return false;
    }

    public static String implode(Collection<String> list, String delim)
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

    public static String getPrettyDate(Date date, Locale locale)
    {
	PrettyTime p = new PrettyTime();
	p.setLocale(locale);
	System.out.println(p.format(date));
	return p.format(date);
    }

    /**
     * Remove last character if it is ','
     * 
     * @param str
     * @return String
     */
    public static String removeLastComma(String str)
    {
	if(str.endsWith(","))
	{
	    str = str.substring(0, str.length() - 1);
	}
	return str;
    }

}
