package de.l3s.learnweb.resource;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;
import org.primefaces.PrimeFaces;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.util.NlpHelper;

@Named
@ViewScoped
public class ResourceAnnotationBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -6100755972011969429L;

    private static final Pattern SPACES = Pattern.compile("\\s+");

    @Inject
    private AnnotationDao annotationDao;

    @PostConstruct
    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());
    }

    /**
     * Saves the changes in the annotated text.
     */
    public void commandSaveAnnotation(final Resource resource) {
        BeanAssert.authorized(resource.canAnnotateResource(getUser()));

        String annotatedText = Faces.getRequestParameter("annotatedText");
        resource.setTranscript(annotatedText);
        resource.save();

        annotationDao.save(new Annotation(resource.getId(), getUser().getId(), "save transcript", null, null));

        getUser().clearCaches();
        addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    /**
     * Submits the annotation as a final version.
     */
    public void commandCommitAnnotation(final Resource resource) {
        BeanAssert.authorized(resource.canAnnotateResource(getUser()));

        String transcript = Faces.getRequestParameter("annotatedText");
        resource.setTranscript(transcript);
        resource.setReadOnlyTranscript(true);
        resource.save();

        annotationDao.save(new Annotation(resource.getId(), getUser().getId(), "submit transcript", null, null));

        getUser().clearCaches();
        addGrowl(FacesMessage.SEVERITY_INFO, "Annotation Submitted");
    }

    /**
     * Stores an annotation action such as selection, de-selection, user text.
     */
    public void commandLogAnnotation(final Resource resource) {
        BeanAssert.authorized(resource.canAnnotateResource(getUser()));

        Map<String, String> params = Faces.getRequestParameterMap();
        annotationDao.save(new Annotation(resource.getId(), getUser().getId(), params.get("action"), params.get("selection"), params.get("annotation")));
    }

    /**
     * Retrieves the set of synonyms from WordNet for given selection of term.
     */
    public void commandGetDefinition() {
        String words = Faces.getRequestParameter("term");
        StringBuilder synonymsList = new StringBuilder();
        int wordCount = SPACES.split(words.trim()).length;

        if (wordCount <= 5) {
            ArrayList<String> definitions = NlpHelper.getWordnetDefinitions(words);

            for (String definition : definitions) {
                synonymsList.append(definition).append("&lt;br/&gt;");
            }

            if (definitions.isEmpty()) {
                Messages.addError("growl", "No definition available");
            } else {
                PrimeFaces.current().ajax().addCallbackParam("synonyms", synonymsList.toString());
            }
        } else {
            Messages.addError("growl", "Too many words selected");
        }
    }
}
