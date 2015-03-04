package de.l3s.searchlogclient.jaxb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="CommentonSearch")
public class CommentonSearch {
	
	private String commentText;
	private int userId;
	private String username;
	private String timestamp;
	private int resultsetId;
	private int commentId;
	
	private String time;
	
	//Default Constructor
	public CommentonSearch(){}
	
	//Parameterized Constructor1
	public CommentonSearch(String comment, int userId, int resultsetId, String timestamp,String username){
		this.commentText=comment;
		this.userId=userId;
		this.resultsetId=resultsetId;
		this.timestamp=timestamp;
		this.username = username;
	}
	
	public CommentonSearch(String comment, int userId, String username, String timestamp){
		this.commentText = comment;
		this.userId = userId;
		this.username = username;
		this.timestamp = timestamp;
	}

	@XmlElement
	public String getCommentText() {
		return commentText;
	}

	public void setCommentText(String comment) {
		this.commentText = comment;
	}

	@XmlElement	
	public int getUserId() {
		return userId;
	}
	
	public void setUserId(int userId) {
		this.userId = userId;
	}
	
	@XmlElement
	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	@XmlElement
	public int getResultsetId() {
		return resultsetId;
	}

	public void setResultsetId(int resultsetId) {
		this.resultsetId = resultsetId;
	}

	@XmlElement	
	public int getCommentId() {
		return commentId;
	}

	public void setCommentId(int commentId) {
		this.commentId = commentId;
	}

	@XmlElement
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@XmlElement
	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}
}
