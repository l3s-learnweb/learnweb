package de.l3s.util;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.poi.ss.formula.eval.NotImplementedException;

/**
 * Stores an URL as ASCII encoded bytes
 * 
 * @author Philipp
 *
 */
public class URL
{
    private byte[] url; // the ASCII encoded URL 

    /**
     * 
     * @param url A UTF-8 encoded string
     * 
     * @throws URISyntaxException
     */
    public URL(String url) throws URISyntaxException
    {
        this.url = StringHelper.convertUnicodeURLToAscii(url).getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * 
     * @param url an ASCII encoded URL
     */
    public URL(byte[] url)
    {
        this.url = url;
    }

    /**
     * Returns an ASCII encoded string
     */
    @Override
    public String toString()
    {
        return new String(url, StandardCharsets.US_ASCII);
    }

    /**
     * Returns an UTf8 encoded String
     * 
     * @return
     */
    public String toUTF8String()
    {
        throw new NotImplementedException("");
        // TODO decode puny and percent-encoding
        //return new String(url, StandardCharsets.US_ASCII);
    }

    public byte[] getBytes()
    {
        return url;
    }

}
