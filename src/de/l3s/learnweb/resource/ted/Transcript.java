package de.l3s.learnweb.resource.ted;

import java.util.LinkedList;
import java.util.List;

import de.l3s.util.StringHelper;

public class Transcript
{
    private String languageCode;
    private List<Paragraph> paragraphs = new LinkedList<>();

    public String getLanguageCode()
    {
        return languageCode;
    }

    public void setLanguageCode(String languageCode)
    {
        this.languageCode = languageCode;
    }

    /**
     * 
     * @return all Paragraphs of this transcript ordered by their startTime
     */
    public List<Paragraph> getParagraphs()
    {
        return paragraphs;
    }

    public void setParagraphs(List<Paragraph> paragraphs)
    {
        this.paragraphs = paragraphs;
    }

    public void addParagraph(int startTime, String text)
    {
        getParagraphs().add(new Paragraph(startTime, text));
    }

    public class Paragraph
    {
        private int startTime;
        private String text;

        public Paragraph(int startTime, String text)
        {
            super();
            this.startTime = startTime;
            this.text = text;
        }

        public int getStartTime()
        {
            return startTime;
        }

        public String getStartTimeInMinutes()
        {
            int seconds = startTime / 1000;
            return StringHelper.getDurationInMinutes(seconds);
        }

        public String getText()
        {
            return text;
        }
    }

}
