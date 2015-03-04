package de.l3s.learnweb.beans;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean
@SessionScoped
public class Customer
{
    private String firstName;

    private Person selectedPerson = new Person();

    private List<Person> persons = new ArrayList<Person>();

    private boolean editSelectedPerson;

    public void setPersons(List<Person> persons)
    {
	this.persons = persons;
    }

    public List<Person> getPersons()
    {
	if(persons.size() == 0)
	{
	    persons.add(new Person());
	}
	return persons;
    }

    public String edit(Person person)
    {
	editSelectedPerson = true;
	selectedPerson = person;

	return "editCustomer.xhtml";
    }

    public String addNewUser()
    {
	persons.add(selectedPerson);

	selectedPerson = new Person();

	return "tablepersons.xhtml";
    }

    public String save()
    {
	return "tablepersons.xhtml";
    }

    public String deletePerson(Person persons)
    {
	getPersons().remove(persons);
	return null;
    }

    public String add()
    {
	selectedPerson = new Person();
	editSelectedPerson = false;
	return "editCustomer.xhtml";
    }

    public String quit()
    {
	return "tablepersons.xhtml";
    }

    public String getFirstName()
    {
	return firstName;
    }

    public void setFirstName(String firstName)
    {
	this.firstName = firstName;
	System.out.println(firstName);
    }

    public Person getSelectedPerson()
    {
	return selectedPerson;
    }

    public void setSelectedPerson(Person selectedPerson)
    {
	this.selectedPerson = selectedPerson;
    }

    public boolean isEditSelectedPerson()
    {
	return editSelectedPerson;
    }

    public void setEditSelectedPerson(boolean editSelectedPerson)
    {
	this.editSelectedPerson = editSelectedPerson;
    }

}
