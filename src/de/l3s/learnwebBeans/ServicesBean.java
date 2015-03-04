package de.l3s.learnwebBeans;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ComponentSystemEvent;

import de.l3s.interwebj.AuthCredentials;
import de.l3s.interwebj.IllegalResponseException;
import de.l3s.interwebj.InterWeb;
import de.l3s.interwebj.AuthorizationInformation.ServiceInformation;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;

@ManagedBean
@ViewScoped
public class ServicesBean extends ApplicationBean implements Serializable 
{	
	private static final long serialVersionUID = 8811380147552369046L;

	private String action = null;
	private String iwToken = null;
	private String iwSecret = null;
	private List<ServiceInformation> services = new LinkedList<ServiceInformation>();
	private User user; 
	
	public ServicesBean() throws SQLException, IllegalResponseException, IOException
	{
		System.out.println("ServicesBean()");
		user = getUser();
	}
	
	public String getAuthorizeUrl() throws IllegalResponseException
	{
		return user.getInterweb().getAuthorizeUrl(UtilBean.getLearnwebBean().getBaseUrl()+"myhome/services.jsf?action=auth");
	}

	// viewParam: action
	public void setAction(String action) 
	{
		this.action = action;
	}	

	public String getAction() {
		return action;
	}

	// viewParam: iw_token
	public void setIwToken(String iwToken) 
	{
		this.iwToken = iwToken;
	}

	// viewParam: iw_secret
	public void setIwSecret(String iwSecret)
	{
		this.iwSecret = iwSecret;
	}

	public String getIwToken() {
		return iwToken;
	}	

	public String getIwSecret()
	{
		return iwSecret;
	}
	
	public List<ServiceInformation> getServices()
	{
		return services;
	}

	public void preRenderView(ComponentSystemEvent ev) throws IOException, SQLException 
	{
		System.out.println("preRenderView");
		UserBean userBean = UtilBean.getUserBean();
		if(!userBean.isLoggedIn())
		{
			return;
		}
	
		if(action != null && action.equals("auth") && iwToken != null)
		{			
			AuthCredentials authCredentials = new AuthCredentials(iwToken, iwSecret);
			user.setInterwebToken(authCredentials);
			getLearnweb().getUserManager().save(user);			
		}
		/*
		else if(user.isLoggedInInterweb())
		{
			String referer = getFacesContext().getExternalContext().getRequestHeaderMap().get("referer");
			
			if(referer != null && !referer.startsWith(Util.getLearnwebBean().getBaseUrl()))
			{
				System.out.println("reload getAuthorizationInformation");
				user.getInterweb().getAuthorizationInformation(false);
			}
		}*/
		
		
		if(user.isLoggedInInterweb())
		{		
			//Course role = user.getCourse();		
			
			List<ServiceInformation> services;
			try {
				services = user.getInterweb().getAuthorizationInformation(true).getServices();
			}
			catch (IOException e) {
				addMessage(FacesMessage.SEVERITY_FATAL, "Interweb timeout");
				e.printStackTrace();
				return;
			} 
			catch (IllegalResponseException e) {
				addMessage(FacesMessage.SEVERITY_FATAL, "Interweb error");
				e.printStackTrace();
				this.services.clear();
				return;
			}
			
			this.services.clear();
			for(ServiceInformation service : services)
			{			
				String id = service.getId();
				
				if(id.equals("Bing") ||
						id.equals("Google") 						
						/*id.equals("Blogger") && role.getOption(Option.Services_Hide_Blogger) ||
						id.equals("Delicious") && role.getOption(Option.Services_Hide_Delicious) ||
						id.equals("Facebook") && role.getOption(Option.Services_Hide_Facebook) ||
						id.equals("Flickr") && role.getOption(Option.Services_Hide_Flickr) ||
						id.equals("GroupMe") && role.getOption(Option.Services_Hide_GroupMe) ||
						id.equals("Ipernity") && role.getOption(Option.Services_Hide_Ipernity) ||
						id.equals("LastFm") && role.getOption(Option.Services_Hide_lastfm) ||
						id.equals("SlideShare") && role.getOption(Option.Services_Hide_SlideShare) ||
						id.equals("Vimeo") && role.getOption(Option.Services_Hide_Vimeo) ||
						id.equals("YouTube") && role.getOption(Option.Services_Hide_YouTube)*/)
					continue;
				
				this.services.add(service);
			}
		}
	}
	
	public void test()
	{
		System.out.println("test call");
	}
	
	public String signOutFromService(String serviceId) throws IOException, IllegalResponseException
	{System.out.println("drin");
		user.getInterweb().revokeAuthorizationOnService(serviceId);
		return null;
	}

	public String signInOnService(ServiceInformation service) throws IOException, IllegalResponseException
	{
		System.out.println("signing in on service " + service.getId());
		InterWeb interWeb = user.getInterweb();
		String callback = UtilBean.getLearnwebBean().getBaseUrl() + "/myhome/services.jsf";
		interWeb.authorizeService(service, callback);
		return null;
	}

	public String signOutFromInterweb()
	{
		System.out.println("sign out from interweb");
		user.setInterwebToken(null);
		return null;
	}		
	
	public String getInterwebUsername() throws IllegalResponseException
	{
		return user.getInterweb().getUsername();
	}
	
	public boolean isUserAllowedToLogoutFromInterweb() throws SQLException
	{	
		return false; //user.getCourse().getOption(Option.Services_Allow_user_to_logout_from_interweb);
	}
	
	public boolean isLoginBasedService(Object obj)
	{	
		ServiceInformation service = (ServiceInformation) obj;
		return service.getAuthorizationType().equals("login");
	}
}
