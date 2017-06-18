package jcdashboard.model;

import java.util.Date;

public class UserLog implements java.io.Serializable {

	private long logEntryId;
	private Date timestamp;
	private int userId;
	private String sessionId;
	private byte actionId;
	private String action;
	private int targetId;
	private String params;

	public UserLog() {
	}

	public UserLog(long logEntryId, int userId, String sessionId,
			byte actionId, String action, int targetId, String params) {
		this.logEntryId = logEntryId;
		this.userId = userId;
		this.sessionId = sessionId;
		this.actionId = actionId;
		this.action = action;
		this.targetId = targetId;
		this.params = params;
	}

	public long getLogEntryId() {
		return this.logEntryId;
	}

	public void setLogEntryId(long logEntryId) {
		this.logEntryId = logEntryId;
	}

	public Date getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public int getUserId() {
		return this.userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getSessionId() {
		return this.sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public byte getActionId() {
		return this.actionId;
	}

	public void setActionId(byte actionId) {
		this.actionId = actionId;
	}

	public String getAction() {
		return this.action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public int getTargetId() {
		return this.targetId;
	}

	public void setTargetId(int targetId) {
		this.targetId = targetId;
	}

	public String getParams() {
		return this.params;
	}

	public void setParams(String params) {
		this.params = params;
	}

}
