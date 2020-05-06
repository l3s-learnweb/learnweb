package de.l3s.util;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rita.wordnet.RiWordnet;

public final class NlpHelper
{
    private static final Logger log = LogManager.getLogger(NlpHelper.class);

    private static final Pattern PUNCTUATION = Pattern.compile("\\p{P}");

    /**
     * Retrieves the set of synonyms from WordNet for given selection of word
     */
    public static ArrayList<String> getRitaWordnetDefinitions(String words)
    {
        ArrayList<String> synonymsList = new ArrayList<>();
        words = PUNCTUATION.matcher(words).replaceAll(""); // remove punctuation characters

        RiWordnet wordnet = new RiWordnet();
        String[] pos = wordnet.getPos(words);
        for(final String p : pos)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(words).append('(').append(p).append(") - ");
            sb.append(wordnet.getDescription(words, p));

            String[] synonyms = wordnet.getAllSynsets(words, p);
            if(synonyms != null && synonyms.length > 0)
            {
                sb.append(": ");

                for(int j = 0, len = Math.min(synonyms.length, 10); j < len; j++)
                {
                    if(j != 0) sb.append(", ");
                    sb.append(synonyms[j]);
                }
            }

            sb.append(".");

            log.debug(sb);
            synonymsList.add(sb.toString());
        }

        return synonymsList;
    }
}
