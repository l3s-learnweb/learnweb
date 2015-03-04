package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.validation.constraints.Size;

public class Organisation implements Serializable, Comparable<Organisation>
{
	private static final long serialVersionUID = -5187205229505825818L;
	
	// add new options add the end , don't delete options !!!!!
	// if you add 64 options you have to add one options_field{x} column in lw_organisation
	public static enum Option implements Comparable<Option>{
		Resource_Hide_Star_rating,
		Resource_Hide_Thumb_rating
	}
	
	private int id;
	@Size(min=1, max=60)
	private String title;
	private String logo;
	private String welcomeMessage;
	private String welcomePage;
	private long[] options = new long[1];

	
	/**
	 * Constructs a temporary object. Can be persisted by OrganisationManager.save()
	 * 
	 */
	public Organisation()
	{
		this.id = -1;
	}
	
	/**
	 * This constructor should only be called by OrganisationManager
	 * @param rs
	 * @throws SQLException
	 */
	protected Organisation(ResultSet rs) throws SQLException
	{
		this.id = rs.getInt("organisation_id");
		this.title = rs.getString("title");
		this.logo = rs.getString("logo");
		this.welcomePage = rs.getString("welcome_page");
		this.welcomeMessage = rs.getString("welcome_message");
		
		for(int i=0; i<1;)
			options[i] = rs.getInt("options_field"+ (++i));
	}
	
	public List<Course> getCourses() throws SQLException
	{
		return Learnweb.getInstance().getCourseManager().getCoursesByOrganisationId(id);
	}

	@Override
	public String toString() {
		return "Organisation [id=" + id + ", title=" + title + ", logo=" + logo + "]";
	}

	@Override
	public int compareTo(Organisation o) 
	{
		if(null == o)
			return -1;
		
		return title.compareTo(o.getTitle());
	}	
	
	
	/**
	 * A negative id indicates, that this object is not stored at the database
	 * @return
	 */
	public int getId() {
		return id;
	}
	
	public String getTitle() {
		return title;
	}	
	
	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

	/**
	 * This method should only be called by OrganisationManager
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getWelcomeMessage() {
		return welcomeMessage;
	}

	public void setWelcomeMessage(String welcomeMessage) {
		this.welcomeMessage = welcomeMessage;
	}

	/**
	 * The page that will be displayed after a user logs in
	 * @return
	 */
	public String getWelcomePage() 
	{
		if(null == welcomePage || welcomePage.length() == 0)
			return "myhome/resources_grid.jsf";
		
		return welcomePage;
	}

	public void setWelcomePage(String welcomePage) {
		this.welcomePage = welcomePage;
	}	
	
	public boolean getOption(Option option)
	{
		int bit = option.ordinal();
		int field = bit >> 6; 
		long bitMask = 1L << (bit % 64);		
			
		return (options[field] & bitMask) == bitMask;
	}
	
	public void setOption(Option option, boolean value)
	{
		int bit = option.ordinal();
		int field = bit >> 6;
		long bitMask = 1L << (bit % 64);			
		
		if(value) // is true set Bit to 1
		{
			options[field] |= bitMask;			
		}
		else
		{
			options[field] &= ~bitMask;
		}
	}
	
	public long[] getOptions() {
		return options;
	}
}
