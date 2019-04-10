package de.l3s.learnweb.resource.glossary.builders;

import de.l3s.learnweb.resource.glossary.GlossaryEntry;
import de.l3s.learnweb.resource.glossary.GlossaryTerm;
import org.apache.poi.ss.usermodel.Row;

import java.util.Arrays;
import java.util.List;

public class GlossaryEntryGlossaryRowBuilder extends AbstractGlossaryRowBuilder<GlossaryEntry> {

    @Override
    public GlossaryEntry build(Row row) {
        GlossaryEntry result = new GlossaryEntry();

        if (topicOneHeaderPosition >= 0) {
            result.setTopicOne(getStringValueForCell(row.getCell(topicOneHeaderPosition)));
        } else {
            throw new IllegalArgumentException("Topic 1 column absent in Excel file");
        }
        result.setTopicTwo(getStringValueForCell(row.getCell(topicTwoHeaderPosition)));
        result.setTopicThree(getStringValueForCell(row.getCell(topicThreeHeaderPosition)));
        result.setDescription(getStringValueForCell(row.getCell(descriptionHeaderPosition)));

        result.addTerm(buildTerm(row));

        return result;
    }

    private GlossaryTerm buildTerm(Row row) {
        GlossaryTerm term = new GlossaryTerm();

        term.setTerm(getStringValueForCell(row.getCell(termHeaderPosition)));

        if (!languageMap.containsKey(getStringValueForCell(row.getCell(languageHeaderPosition)))) {
            throw new IllegalArgumentException("Language " + getStringValueForCell(row.getCell(languageHeaderPosition)) + " does not support yet" + ". Row " + row.getRowNum());
        }
        term.setLanguage(languageMap.get(getStringValueForCell(row.getCell(languageHeaderPosition))));

        String usesString = getStringValueForCell(row.getCell(usesHeaderPosition));
        List<String> uses = Arrays.asList(usesString.split(","));
        term.setUses(uses);

        term.setPronounciation(getStringValueForCell(row.getCell(pronunciationHeaderPosition)));
        term.setAcronym(getStringValueForCell(row.getCell(acronymHeaderPosition)));
        term.setSource(getStringValueForCell(row.getCell(sourceHeaderPosition)));
        term.setPhraseology(getStringValueForCell(row.getCell(phraseologyHeaderPosition)));

        return term;
    }
}
