package de.l3s.learnwebBeans;

public class FacebookMessageBean extends ApplicationBean {
	
	private String wizardURL;
	
	public FacebookMessageBean() {
		
	}

	public String getWizardURL() {
		return wizardURL;
	}

	public void setWizardURL(String wizardURL) {
		this.wizardURL = wizardURL;
	}

}
