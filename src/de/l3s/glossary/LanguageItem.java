package de.l3s.glossary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LanguageItem implements Serializable
{
    private static final long serialVersionUID = 7068970099338006288L;

    //private Locale[] supportedLanguages = { Locale.GERMANY, Locale.UK };

    public enum LANGUAGE
    {
        EN
        {
            @Override
            public String toString()
            {
                return "English-EN";
            }
        },
        IT
        {

            @Override
            public String toString()
            {
                return "Italian-IT";
            }
        },

        FR
        {
            @Override
            public String toString()
            {
                return "French-FR";
            }
        },
        NL
        {
            @Override
            public String toString()
            {
                return "Dutch-NL";
            }
        },
        DE
        {
            @Override
            public String toString()
            {
                return "German-DE";
            }
        }
    }

    private String value;
    private String pronounciation;
    private String acronym;
    private String references;
    private String phraseology;
    private List<String> selectedUses;
    private LANGUAGE language;
    private int termId;
    private String useLabel = "Use";

    // TODO remove this. you can use LANGUAGE.valueOf(arg0) instead. But this enum should be replaced by Locale anyway
    public LANGUAGE getEnum(String langValue)
    {
        switch(langValue)
        {
        case "English-EN":
            return language.EN;
        case "Italian-IT":
            return language.IT;
        case "French-FR":
            return language.FR;
        case "Dutch-NL":
            return language.NL;
        case "German-DE":
            return language.DE;
        default:
            return null;
        }
    }

    public void updateUseLabel()
    {
        String label = ""; // = StringHelper.implode(getSelectedUses(), ", ");

        List<String> useLabel = new ArrayList<String>(getSelectedUses());
        for(String u : useLabel)
        {
            label = label + u + ", ";
        }

        if(label.contains(","))
        {
            label = label.trim().substring(0, label.lastIndexOf(","));
            if(!label.trim().isEmpty())
                setUseLabel(label);

            else
            {
                setUseLabel("Use");
            }
        }

        //StringHelper.implode(getSelectedUses(), ", ");
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        if(value != null)
            this.value = value;
        else
            this.value = "";
    }

    @Override
    public String toString()
    {
        return String.format("Item[value=%s]", value);
    }

    public String getPronounciation()
    {
        return pronounciation;
    }

    public void setPronounciation(String pronounciation)
    {
        if(pronounciation != null)
            this.pronounciation = pronounciation;
        else
            this.pronounciation = "";
    }

    public String getAcronym()
    {
        return acronym;
    }

    public void setAcronym(String acronym)
    {
        if(acronym != null)
            this.acronym = acronym;
        else
            this.acronym = "";
    }

    public String getReferences()
    {
        return references;
    }

    public void setReferences(String references)
    {
        if(references != null)
            this.references = references;
        else
            this.references = "";
    }

    public String getPhraseology()
    {
        return phraseology;
    }

    public void setPhraseology(String phraseology)
    {
        if(phraseology != null)
            this.phraseology = phraseology;
        else
            this.phraseology = "";
    }

    public List<String> getSelectedUses()
    {
        return selectedUses;
    }

    public void setSelectedUses(List<String> selectedUses)
    {
        this.selectedUses = new ArrayList<String>();
        if(!selectedUses.isEmpty())
            this.selectedUses = selectedUses;

    }

    public de.l3s.glossary.LanguageItem.LANGUAGE getLanguage()
    {
        return language;
    }

    public void setLanguage(de.l3s.glossary.LanguageItem.LANGUAGE language)
    {
        this.language = language;
    }

    public int getTermId()
    {
        return termId;
    }

    public void setTermId(int termId)
    {
        this.termId = termId;
    }

    public String getUseLabel()
    {
        return useLabel;
    }

    public void setUseLabel(String useLabel)
    {
        //        Logger.getLogger(this.getClass()).debug("setUseLabel: " + useLabel);
        this.useLabel = useLabel;
    }

}
