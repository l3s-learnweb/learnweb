package de.l3s.util;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.poi.ss.formula.eval.NotImplementedException;

/**
 * Stores a URL as ASCII encoded bytes.
 *
 * @author Philipp Kemkes
 */
public class URL {
    private final byte[] url; // the ASCII encoded URL

    /**
     * @param url A UTF-8 encoded string
     */
    public URL(String url) throws URISyntaxException {
        this.url = UrlHelper.toAscii(url).getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * @param url an ASCII encoded URL
     */
    public URL(byte[] url) {
        this.url = url;
    }

    /**
     * Returns an ASCII encoded string.
     */
    @Override
    public String toString() {
        return new String(url, StandardCharsets.US_ASCII);
    }

    /**
     * Returns an UTf8 encoded String.
     *
     * TODO @astappiev: decode puny and percent-encoding
     */
    public String toUTF8String() {
        throw new NotImplementedException("");
    }

    public byte[] getBytes() {
        return url;
    }

}
