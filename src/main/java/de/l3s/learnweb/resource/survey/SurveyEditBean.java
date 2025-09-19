package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.omnifaces.util.Beans;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDetailBean;

@Named
@ViewScoped
public class SurveyEditBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 669288862248912801L;

    private SurveyResource resource;

    @Inject
    private SurveyDao surveyDao;

    @PostConstruct
    public void onLoad() {
        Resource baseResource = Beans.getInstance(ResourceDetailBean.class).getResource();
        resource = surveyDao.convertToSurveyResource(baseResource).orElseThrow(BeanAssert.NOT_FOUND);
        BeanAssert.hasPermission(resource.canModerateResource(getUser()));
    }

    public void onAddPage() {
        List<SurveyPage> pages = resource.getPages();
        SurveyPage page = new SurveyPage();
        page.setOrder(pages.size());
        page.setResourceId(resource.getId());
        surveyDao.savePage(page);
        pages.add(page);
    }

    /**
     * @param direction set -1 to move upward or 1 to move down
     */
    public void onMovePage(SurveyPage page, int direction) {
        int oldOrder = page.getOrder();
        page.setOrder(oldOrder + direction); // move selected question
        surveyDao.savePage(page);

        SurveyPage neighbor = resource.getPages().get(page.getOrder());
        neighbor.setOrder(oldOrder); // move neighbor question
        surveyDao.savePage(neighbor);
        resource.getPages().sort(Comparator.comparingInt(SurveyPage::getOrder));
    }

    public void onDuplicatePage(SurveyPage page) {
        SurveyPage duplicate = new SurveyPage(page);
        duplicate.setOrder(resource.getPages().size());
        surveyDao.savePage(duplicate);
        resource.getPages().add(duplicate);
    }

    public void onDeletePage(SurveyPage page) {
        page.setDeleted(true);
        surveyDao.savePage(page);
        resource.getPages().remove(page);
    }

    public List<SurveyPage> getPages() {
        return resource.getPages().stream().filter(page -> !page.isDeleted()).toList();
    }

    public List<SurveyQuestion> getExposedQuestions(SurveyPage targetPage) {
        return getPages().stream()
            .filter(p -> p.getOrder() < targetPage.getOrder())
            .flatMap(p -> p.getQuestions().stream())
            .filter(SurveyQuestion::isExposable)
            .toList();
    }

    public List<SurveyQuestionOption> getExposedQuestionOptions(SurveyPage page) {
        List<SurveyQuestion> questions = getExposedQuestions(page);
        for (SurveyQuestion q : questions) {
            if (q.getId() == page.getRequiredQuestionId()) {
                if (q.getType().isOptions()) {
                    return q.getActiveOptions();
                } else {
                    break;
                }
            }
        }

        return List.of();
    }
}
