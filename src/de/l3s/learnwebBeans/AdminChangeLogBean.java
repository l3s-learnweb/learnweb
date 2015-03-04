package de.l3s.learnwebBeans;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.faces.bean.ManagedBean;

import org.hibernate.validator.constraints.NotBlank;

@ManagedBean


public class AdminChangeLogBean  extends ApplicationBean{
	
	@NotBlank
	private String message;
	private List<String> changelogmessages;
	
	public void update() throws SQLException
	{
		java.sql.PreparedStatement pstmt = getLearnweb().getConnectionStatic().prepareStatement("INSERT INTO admin_change_log (message)	VALUES (?)");
		pstmt.setString(1,message);
	    pstmt.executeUpdate();
	    /*getLearnweb().setAdminMessage(message);*/
		
	}
	
	
	

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}




	public void setChangelogmessages(LinkedList<String> changelogmessages) {
		this.changelogmessages = changelogmessages;
	}




	public List<String> getChangelogmessages() throws SQLException {
		changelogmessages=getLearnweb().getChangeLog();
		return changelogmessages;
	}
	

}
