package de.l3s.learnweb.resource.glossaryNew.builders;

import de.l3s.learnweb.resource.glossaryNew.GlossaryEntry;
import de.l3s.learnweb.resource.glossaryNew.GlossaryTerm;
import org.apache.poi.ss.usermodel.Row;

import java.util.Arrays;
import java.util.List;

public class GlossaryEntryGlossaryRowBuilder extends AbstractGlossaryRowBuilder<GlossaryEntry> {

    @Override
    public GlossaryEntry build(Row row) {
        GlossaryEntry result = new GlossaryEntry();

        if (topicOneHeaderPosition >= 0) {
            result.setTopicOne(row.getCell(topicOneHeaderPosition).getStringCellValue());
        } else {
            throw new IllegalArgumentException("Topic 1 column absent in Excel file");
        }
        result.setTopicTwo(row.getCell(topicTwoHeaderPosition).getStringCellValue());
        result.setTopicThree(row.getCell(topicThreeHeaderPosition).getStringCellValue());
        result.setDescription(row.getCell(descriptionHeaderPosition).getStringCellValue());

        result.addTerm(buildTerm(row));

        return result;
    }

    private GlossaryTerm buildTerm(Row row) {
        GlossaryTerm term = new GlossaryTerm();

        term.setTerm(row.getCell(termHeaderPosition).getStringCellValue());

        if (!languageMap.containsKey(row.getCell(languageHeaderPosition).getStringCellValue())) {
            throw new IllegalArgumentException("Language " + row.getCell(languageHeaderPosition).getStringCellValue() + " does not support yet" + ". Row " + row.getRowNum());
        }
        term.setLanguage(languageMap.get(row.getCell(languageHeaderPosition).getStringCellValue()));

        String usesString = row.getCell(usesHeaderPosition).getStringCellValue();
        List<String> uses = Arrays.asList(usesString.split(","));
        term.setUses(uses);

        term.setPronounciation(row.getCell(pronunciationHeaderPosition).getStringCellValue());
        term.setAcronym(row.getCell(acronymHeaderPosition).getStringCellValue());
        term.setSource(row.getCell(sourceHeaderPosition).getStringCellValue());
        term.setPhraseology(row.getCell(phraseologyHeaderPosition).getStringCellValue());

        return term;
    }
}
