package de.l3s.learnweb.resource;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jakarta.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;

public class MetadataField implements Serializable {
    @Serial
    private static final long serialVersionUID = -7698089608547415349L;

    /**
     * Represents primefaces input types.
     */
    public enum MetadataType {
        FULLWIDTH_HEADER,
        FULLWIDTH_DESCRIPTION,
        INPUT_TEXT,
        INPUT_TEXTAREA,
        ONE_MENU,
        ONE_MENU_EDITABLE,
        MULTIPLE_MENU,
        AUTOCOMPLETE,
        AUTOCOMPLETE_MULTIPLE,
    }

    private String name; // the name of this field, will be used as SOLR column name
    private String label; // label on the website, is replaced by a translated term if available
    private String info; // an explanation, displayed as tooltip
    private MetadataType type; // represents primefaces input types
    private LinkedList<String> options = new LinkedList<>(); // default options for some input types like OneMenu
    private boolean moderatorOnly = false; // only admins and moderators have write access
    private boolean required = false;

    // cache
    private transient List<SelectItem> optionsList; // options wrapped into select items

    public MetadataField(final String name, final String label, final MetadataType type) {
        this.name = name;
        this.label = label;
        this.type = type;
    }

    public MetadataField(final String name, final String label, final MetadataType type, final boolean required) {
        this(name, label, type);
        this.required = required;
    }

    public MetadataField(final String name, final MetadataType type, final boolean moderatorOnly) {
        this(name, name, type);
        this.moderatorOnly = moderatorOnly;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MetadataType getType() {
        return type;
    }

    public void setType(MetadataType type) {
        this.type = type;
    }

    public LinkedList<String> getOptions() {
        return options;
    }

    public void setOptions(LinkedList<String> options) {
        this.options = options;
    }

    public List<SelectItem> getOptionsList() {
        if (null == optionsList) {
            optionsList = new ArrayList<>(options.size());
            options.forEach(option -> optionsList.add(new SelectItem(option, option)));
        }
        return optionsList;
    }

    public List<String> completeText(String query) {
        if (StringUtils.isEmpty(query)) {
            return getOptions();
        }
        return getOptions().stream().filter(option -> StringUtils.containsIgnoreCase(option, query)).toList();
    }

    public boolean isModeratorOnly() {
        return moderatorOnly;
    }

    public void setModeratorOnly(boolean moderatorOnly) {
        this.moderatorOnly = moderatorOnly;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

}
