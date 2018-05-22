package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.faces.model.SelectItem;

public class ResourceMetadataField implements Serializable
{
    private static final long serialVersionUID = -7698089608547415349L;

    public enum MetadataType
    { // represents primeface input types
        INPUT_TEXT,
        INPUT_TEXTAREA,
        AUTOCOMPLETE,
        ONE_MENU,
        ONE_MENU_EDITABLE,
        MULTIPLE_MENU,
        FULLWIDTH_HEADER,
        FULLWIDTH_DESCRIPTION
    }

    private String name; // the name of this field, will be used as SOLR column name
    private String label; // label on the website, is replaced by a translated term if available
    private String info; // an explanation, displayed as tooltip
    private MetadataType type; // represents primeface input types
    private List<String> options = new LinkedList<String>(); // default options for some input types like OneMenu
    private transient List<SelectItem> optionsList; // options wrapped into select itmes
    private boolean moderatorOnly = false; // only admins and moderators have write access
    private boolean required = false;

    public ResourceMetadataField(String name, String label, MetadataType type)
    {
        super();
        this.name = name;
        this.label = label;
        this.type = type;
    }

    public ResourceMetadataField(String name, MetadataType type, boolean moderatorOnly)
    {
        super();
        this.name = name;
        this.label = name;
        this.type = type;
        this.moderatorOnly = moderatorOnly;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public MetadataType getType()
    {
        return type;
    }

    public void setType(MetadataType type)
    {
        this.type = type;
    }

    public List<String> getOptions()
    {
        return options;
    }

    public List<SelectItem> getOptionsList()
    {
        if(null == optionsList)
        {
            optionsList = new ArrayList<SelectItem>(options.size());

            for(String option : options)
            {
                optionsList.add(new SelectItem(option, option));
            }
        }
        return optionsList;
    }

    public List<String> completeText(String query)
    {
        return null;
    }

    public void setOptions(List<String> options)
    {
        this.options = options;
    }

    public boolean isModeratorOnly()
    {
        return moderatorOnly;
    }

    public void setModeratorOnly(boolean moderatorOnly)
    {
        this.moderatorOnly = moderatorOnly;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getInfo()
    {
        return info;
    }

    public void setInfo(String info)
    {
        this.info = info;
    }

    public boolean isRequired()
    {
        return required;
    }

    public void setRequired(boolean required)
    {
        this.required = required;
    }

}
