package de.l3s.learnweb.resource;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Messages;
import org.primefaces.PrimeFaces;

import com.google.gson.Gson;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.util.NlpHelper;

@Named
@ViewScoped
public class ResourceAnnotationBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -6100755972011969429L;
    private static final Logger log = LogManager.getLogger(ResourceAnnotationBean.class);

    private static final Pattern SPACES = Pattern.compile("\\s+");
    private Resource resource;

    @Inject
    private AnnotationDao annotationDao;

    @PostConstruct
    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());

        resource = Beans.getInstance(ResourceDetailBean.class).getResource();
    }

    public void convert() {
        if (resource.getType() == ResourceType.text) {
            if (resource.getMainFile() != null) {
                try {
                    String lines = Files.readString(resource.getMainFile().getActualFile().toPath());
                    lines = lines.replace("\n", "<br/>");
                    resource.setTranscript(lines);
                    resource.save();
                    return;
                } catch (IOException e) {
                    log.error("Error reading file while extracting transcript", e);
                }
            }
        }

        addGrowl(FacesMessage.SEVERITY_ERROR, "Unfortunately, we could not extract the text from the file.");
    }

    public String getAnnotationsAsJson() {
        List<Annotation> annotations = annotationDao.findAllByResourceId(resource.getId());
        return new Gson().toJson(annotations);
    }

    /**
     * Saves the changes in the annotated text.
     */
    public void commandSaveAnnotation(final Resource resource) {
        BeanAssert.authorized(resource.canAnnotateResource(getUser()));

        String annotatedText = Faces.getRequestParameter("annotatedText");
        resource.setTranscript(annotatedText);
        resource.save();

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

        getUser().clearCaches();
        addGrowl(FacesMessage.SEVERITY_INFO, "Annotation Submitted");
    }

    /**
     * Stores an annotation action such as selection, de-selection, user text.
     */
    public void commandLogAnnotation(final Resource resource) {
        BeanAssert.authorized(resource.canAnnotateResource(getUser()));

        Map<String, String> params = Faces.getRequestParameterMap();
        Annotation annotation = new Annotation();
        annotation.setResourceId(resource.getId());
        annotation.setUserId(getUser().getId());
        annotation.setAction(params.get("action"));
        annotation.setSelection(params.get("selection"));
        annotation.setAnnotation(params.get("annotation"));
        annotationDao.save(annotation);
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
