package jcdashboard.model;

import java.util.Date;

public class User implements java.io.Serializable {

	private int userId;
	private String username;
	private String email;
	private Date registrationDate;

	public User() {
	}

	public User(int userId, String username, String email, Date registrationDate) {
		this.userId = userId;
		this.username = username;
		this.email = email;
		this.registrationDate = registrationDate;
	}

	public int getUserId() {
		return this.userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Date getRegistrationDate() {
		return this.registrationDate;
	}

	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}

}
