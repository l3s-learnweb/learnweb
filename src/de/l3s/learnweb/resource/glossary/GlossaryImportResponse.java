package de.l3s.learnweb.resource.glossary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GlossaryImportResponse
{
    private List<ParsingError> listOfErrors = new ArrayList<>();
    private int amountOfEntries;

    public List<ParsingError> getListOfErrors()
    {
        return Collections.unmodifiableList(listOfErrors);
    }

    public int getAmountOfEntries()
    {
        return amountOfEntries;
    }

    public void setAmountOfEntries(final int amountOfEntries)
    {
        this.amountOfEntries = amountOfEntries;
    }

    public boolean isSuccessful()
    {
        return this.listOfErrors.isEmpty();
    }

    public void addErrors(final List<ParsingError> errors)
    {
        listOfErrors.addAll(errors);
    }
}
