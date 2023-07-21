package de.l3s.util;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.IndexWordSet;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import net.sf.extjwnl.dictionary.Dictionary;

public final class NlpHelper {
    private static final Logger log = LogManager.getLogger(NlpHelper.class);

    private static final Pattern PUNCTUATION = Pattern.compile("\\p{P}");

    /**
     * Retrieves the set of synonyms from WordNet for given selection of word.
     */
    public static ArrayList<String> getWordnetDefinitions(String str) {
        final ArrayList<String> synonymsList = new ArrayList<>();
        str = PUNCTUATION.matcher(str).replaceAll(""); // remove punctuation characters

        try {
            Dictionary wordnet = Dictionary.getDefaultResourceInstance();
            IndexWordSet words = wordnet.lookupAllIndexWords(str);
            for (IndexWord word : words.getIndexWordArray()) {
                StringBuilder sb = new StringBuilder();
                sb.append(str).append('(').append(word.getPOS().getLabel()).append(") - ");

                int senseCount = 0;
                for (Synset synset : word.getSenses()) {
                    if (senseCount++ != 0) {
                        sb.append("; ");
                    }
                    sb.append(synset.getGloss()).append(": ");

                    int synonymCount = 0;
                    for (Word synonym : synset.getWords()) {
                        if (synonymCount++ != 0) {
                            sb.append(", ");
                        }
                        sb.append(synonym.getLemma());
                    }
                }

                log.debug(sb);
                synonymsList.add(sb.append(".").toString());
            }
        } catch (JWNLException e) {
            log.error("WordNet exception", e);
        }

        return synonymsList;
    }
}
