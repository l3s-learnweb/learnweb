package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.log4j.Logger;
import javax.validation.constraints.NotBlank;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@RequestScoped
public class PasswordChangeBean extends ApplicationBean implements Serializable
{
    private static final Logger log = Logger.getLogger(PasswordChangeBean.class);
    private static final long serialVersionUID = 2237249691332567548L;

    private String parameter;
    @NotBlank
    private String password;
    @NotBlank
    private String confirmPassword;
    private User user = null;

    public PasswordChangeBean() throws SQLException
    {
        if(parameter == null || parameter.equals(""))
            parameter = getFacesContext().getExternalContext().getRequestParameterMap().get("u");

        if(parameter == null)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
            return;
        }

        String[] splits = parameter.split("_");
        if(splits.length != 2)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
            return;
        }
        int userId = Integer.parseInt(splits[0]);
        String hash = splits[1];

        user = getLearnweb().getUserManager().getUser(userId);

        if(null == user || !hash.equals(PasswordBean.createPasswordChangeHash(user)))
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
            user = null;
            return;
        }
    }

    public void checkParameter() throws SQLException
    {

    }

    public void onChangePassword()
    {
        log.debug("onChangePassword");
        UserManager um = getLearnweb().getUserManager();
        try
        {
            user.setPassword(password);
            um.save(user);
            //um.setPassword(getUser().getId(), password);
            addMessage(FacesMessage.SEVERITY_INFO, "password_changed");

            password = "";
            confirmPassword = "";
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getConfirmPassword()
    {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword)
    {
        this.confirmPassword = confirmPassword;
    }

    public String getParameter()
    {
        return parameter;
    }

    public void setParameter(String parameter)
    {
        this.parameter = parameter;
    }

    /*
    	public void preRenderView() throws ValidatorException, SQLException 
    	{ 		
    		if(parameter == null || parameter.equals("")) 
    			parameter = getFacesContext().getExternalContext().getRequestParameterMap().get("u");
    		
    		if(parameter == null)
    		{
    			addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
    			return;
    		}
    		
    		String[] splits = parameter.split("_");
    		if(splits.length != 2)
    		{
    			addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
    			return;
    		}
    		int userId = Integer.parseInt(splits[0]);
    		String hash = splits[1];
    		
    		user = getLearnweb().getUserManager().getUser(userId);
    		if(null == user || !hash.equals(PasswordBean.createPasswordChangeHash(user)))
    		{
    			addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
    			return;
    		}
    	
    		canChangePassword = true;		
    	}
    
    	public boolean canChangePassword() {
    		return canChangePassword;
    	}
    	*/

    @Override
    public User getUser()
    {
        return user;
    }

    public void validatePassword(FacesContext context, UIComponent component, Object value) throws ValidatorException
    {
        // Find the actual JSF component for the first password field.
        UIInput passwordInput = (UIInput) context.getViewRoot().findComponent("passwordform:password");

        // Get its value, the entered password of the first field.
        String password = (String) passwordInput.getValue();

        if(null != password && !password.equals(value))
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "passwords_do_not_match"));
        }
    }

    public void setDummy(String dummy)
    {

    }
}
