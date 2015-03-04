package de.l3s.learnwebBeans;

import javax.faces.bean.ManagedBean;
import javax.faces.event.ComponentSystemEvent;

import de.l3s.learnweb.Presentation;


@ManagedBean
public class PresentationBean extends ApplicationBean {
	private Presentation presentation;
	private int id;
	
	public Presentation getPresentation() {
		return presentation;
	}

	public void setPresentation(Presentation presentation) {
		this.presentation = presentation;
	}
	
	public void preRenderView(ComponentSystemEvent e)
	{
		try
		{
			setPresentation(getLearnweb().getPresentationManager().getPresentationsById((Integer.parseInt(getFacesContext().getExternalContext().getRequestParameterMap().get("id")))));
		}
		catch(Exception ex)
		{
			
		}
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
