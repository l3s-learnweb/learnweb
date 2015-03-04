package de.l3s.learnwebAdminBeans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@RequestScoped
public class InfoBean extends ApplicationBean 
{
	private String info;

	public InfoBean()
	{
		Runtime rt = Runtime.getRuntime();
		info = "total: "+(rt.totalMemory()/1024/1024) +"mb - free:"+ (rt.freeMemory()/1024/1024)+"mb - max:"+ (rt.maxMemory()/1024/1024);
	}

	public String getInfo() {
		return info;
	}
	
	
}
