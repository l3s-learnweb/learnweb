package de.l3s.learnweb.facts;

public class WikipediaIntroduction
{
    private static WikipediaIntroduction instance;

    public static WikipediaIntroduction getInstance()
    {
	if(instance == null)
	    instance = new WikipediaIntroduction();
	return instance;
    }

    /**
     * 
     * @param wikipediaEntity title of a wikipedia page
     * @param sentences Number of sentences to return
     * @param language Wikipedia version to use
     * @return
     */
    public String getIntroductionBySentence(String wikipediaEntity, String language, int sentences)
    {
	return "Barack Hussein Obama II is the 44th and current President of the United States. He is the first African American to hold the office and the first president born outside the continental United States.";
    }

    /**
     * 
     * @param wikipediaEntity title of a wikipedia page
     * @param language Wikipedia version to use
     * @param paragraphs Number of paragraphs to return
     * @return the paragraphs are HTML encoded
     */
    public String getIntroductionByParagraph(String wikipediaEntity, String language, int paragraphs)
    {
	return "<p>Barack Hussein Obama II (* 4. August 1961 in Honolulu, Hawaii) ist ein US-amerikanischer Politiker und seit dem 20. Januar 2009 der 44. Präsident der Vereinigten Staaten.</p>"
		+ "<p>Obama ist ein auf US-Verfassungsrecht spezialisierter Rechtsanwalt. Im Jahr 1992 schloss er sich der Demokratischen Partei an, für die er 1997 Mitglied im Senat von Illinois wurde. Im Anschluss gehörte er von 2005 bis 2008 als Junior Senator für diesen US-Bundesstaat dem Senat der Vereinigten Staaten an. Bei der Präsidentschaftswahl des Jahres 2008 errang er die Kandidatur seiner Partei und setzte sich dann gegen den Republikaner John McCain durch. Mit seinem Einzug in das Weiße Haus im Januar 2009 bekleidete erstmals ein Afroamerikaner das Amt des Präsidenten. Bei der Wahl des Jahres 2012 besiegte Obama seinen republikanischen Herausforderer Mitt Romney und wurde so für eine zweite Amtszeit bestätigt. Vizepräsident während seiner beiden Amtsperioden ist Joe Biden.</p>";
    }

    public static void main(String[] args)
    {
	WikipediaIntroduction intro = WikipediaIntroduction.getInstance();

	System.out.println(intro.getIntroductionBySentence("Barack Obama", "en", 2));
	System.out.println(intro.getIntroductionByParagraph("Barack Obama", "de", 2));
    }
}
