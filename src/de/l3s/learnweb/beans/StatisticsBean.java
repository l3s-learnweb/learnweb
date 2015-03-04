package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.SQLException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnwebBeans.ApplicationBean;
import de.l3s.util.Sql;

@ManagedBean
@RequestScoped
public class StatisticsBean extends ApplicationBean implements Serializable
{
	private static final long serialVersionUID = 8540469716342151138L;
	private Long users;
	private Long groups;
	private Long resources;
	private Long ratedResourcesCount;
	private Long taggedResourcesCount;
	private Long commentedResourcesCount;
	private double ratedResourcesAverage;
	private double taggedResourcesAverage;
	private double commentedResourcesAverage;
	private Long rateCount;
	private Long tagCount;
	private Long commentCount;
	private BigDecimal averageSessionTime;
	
	public StatisticsBean()
	{
	    /*
	     * Weitere Statistiken:
	     * 
	     * Mitglieder pro Gruppe
	     * SELECT COUNT(*) AS groups, o.users FROM ( SELECT group_id, COUNT(user_id) as users FROM `lw_group` LEFT JOIN lw_group_user USING (group_id) GROUP BY group_Id) o GROUP BY o.users  
	     *
	     */
		try {
		    	users = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user WHERE deleted = 0");
		    	groups = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_group WHERE deleted = 0");
		    	resources = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_resource WHERE deleted = 0");
		    
			ratedResourcesCount = (Long) Sql.getSingleResult("SELECT (SELECT count(DISTINCT resource_id) FROM `lw_resource_rating`) + (SELECT count(DISTINCT resource_id) FROM `lw_thumb`)");
			taggedResourcesCount = (Long) Sql.getSingleResult("SELECT count(DISTINCT resource_id) FROM lw_resource_tag");
			commentedResourcesCount = (Long) Sql.getSingleResult("SELECT count(DISTINCT resource_id) FROM lw_comment");

			rateCount = (Long) Sql.getSingleResult("SELECT (SELECT count(*) FROM `lw_resource_rating`) + (SELECT count(*) FROM `lw_thumb`)");
			tagCount = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_resource_tag");
			commentCount = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_comment");

			ratedResourcesAverage = (double)rateCount / (double)ratedResourcesCount;
			taggedResourcesAverage = (double)tagCount / (double)taggedResourcesCount;
			commentedResourcesAverage = (double)commentCount / (double)commentedResourcesCount;
			
			averageSessionTime = (BigDecimal) Sql.getSingleResult("SELECT avg(diff) / 60 FROM (SELECT count(*) as t, UNIX_TIMESTAMP(max(timestamp)) -  UNIX_TIMESTAMP(min(timestamp)) AS diff FROM `lw_user_log` GROUP BY session_id) AS DE");			
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	

	public static void main(String[] args)
	{
		new StatisticsBean();
	}


	public Long getRatedResourcesCount() {
		return ratedResourcesCount;
	}


	public Long getTaggedResourcesCount() {
		return taggedResourcesCount;
	}


	public Long getCommentedResourcesCount() {
		return commentedResourcesCount;
	}


	public double getRatedResourcesAverage() {
		return ratedResourcesAverage;
	}


	public double getTaggedResourcesAverage() {
		return taggedResourcesAverage;
	}


	public double getCommentedResourcesAverage() {
		return commentedResourcesAverage;
	}


	public Long getRateCount() {
		return rateCount;
	}


	public Long getTagCount() {
		return tagCount;
	}


	public Long getCommentCount() {
		return commentCount;
	}


	public BigDecimal getAverageUsageTime() {
		return averageSessionTime;
	}


	public Long getUsers()
	{
	    return users;
	}


	public Long getGroups()
	{
	    return groups;
	}


	public Long getResources()
	{
	    return resources;
	}
	
	
	
	
}
