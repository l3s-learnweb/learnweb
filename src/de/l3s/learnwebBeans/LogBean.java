package de.l3s.learnwebBeans;


import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

@RequestScoped
@ManagedBean
public class LogBean {

	private String text;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	
	public String machwas()
	{
		text = "bla";
		return text;
	}
}
 