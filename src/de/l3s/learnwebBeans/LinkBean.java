package de.l3s.learnwebBeans;

import java.io.IOException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.User;


@ManagedBean
@RequestScoped
public class LinkBean extends ApplicationBean{

	private String url;
	
	public LinkBean(){
		
	}
	
	public void redirect(){
		
	}

	public String getUrl() {
		FacesContext facesContext = getFacesContext();
		ExternalContext ext = facesContext.getExternalContext();
		HttpServletRequest servletRequest = (HttpServletRequest) ext.getRequest();
		HttpServletResponse servletResponse = (HttpServletResponse)ext.getResponse();
		
		url = servletRequest.getParameter("link");
		User user = UtilBean.getUserBean().getUser();

		if(url!=null && url!=""){
			try {
				System.out.println(user.getUsername()+" opens: "+url);
				servletResponse.sendRedirect(url);
				log(Action.open_link, 0, url);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
}
