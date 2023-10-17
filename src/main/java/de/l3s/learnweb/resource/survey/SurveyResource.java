package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;

public class SurveyResource extends Resource {
    @Serial
    private static final long serialVersionUID = 3431955030925189235L;

    private LocalDateTime openDate;
    private LocalDateTime closeDate;
    private boolean quiz = false; // show only one question at once
    private boolean singleResponse = false; // limit users to take the survey only one time
    private boolean disableAutosave = false; // if true users will need to complete full survey before answering

    private transient List<SurveyPage> pages;

    /**
     * Do nothing constructor.
     */
    public SurveyResource() {
        super(StorageType.LEARNWEB, ResourceType.survey, ResourceService.learnweb);
    }

    /**
     * Copy constructor.
     */
    protected SurveyResource(SurveyResource other) {
        super(other);
        setOpenDate(other.getOpenDate());
        setCloseDate(other.getCloseDate());
        setQuiz(other.isQuiz());
        setSingleResponse(other.isSingleResponse());
        setDisableAutosave(other.isDisableAutosave());

        for (SurveyPage page : other.getPages()) {
            pages.add(new SurveyPage(page));
        }
    }

    @Override
    public SurveyResource cloneResource() {
        return new SurveyResource(this);
    }

    @Override
    public void clearCaches() {
        super.clearCaches();
        pages = null;
    }

    @Override
    protected void postConstruct() {
        super.postConstruct();

        if (getMetadataValue("openDate") != null) {
            setOpenDate(LocalDateTime.ofEpochSecond(Long.parseLong(getMetadataValue("openDate")), 0, ZoneOffset.UTC));
        }
        if (getMetadataValue("closeDate") != null) {
            setCloseDate(LocalDateTime.ofEpochSecond(Long.parseLong(getMetadataValue("closeDate")), 0, ZoneOffset.UTC));
        }

        quiz = getMetadataValueBoolean("quiz");
        singleResponse = getMetadataValueBoolean("singleResponse");
        disableAutosave = getMetadataValueBoolean("disableAutosave");
    }

    @Override
    public Resource save() {
        if (openDate != null) {
            setMetadataValue("openDate", String.valueOf(openDate.toInstant(ZoneOffset.UTC).getEpochSecond()));
        } else {
            removeMetadataValue("openDate");
        }
        if (closeDate != null) {
            setMetadataValue("closeDate", String.valueOf(closeDate.toInstant(ZoneOffset.UTC).getEpochSecond()));
        } else {
            removeMetadataValue("closeDate");
        }

        setMetadataValueBoolean("quiz", quiz);
        setMetadataValueBoolean("singleResponse", singleResponse);
        setMetadataValueBoolean("disableAutosave", disableAutosave);

        // save normal resource fields
        super.save();

        Learnweb.dao().getSurveyDao().savePages(this);
        return this;
    }

    @Override
    public String toString() {
        return "Survey" + super.toString();
    }

    public LocalDateTime getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDateTime openDate) {
        this.openDate = openDate;
    }

    public LocalDateTime getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(LocalDateTime closeDate) {
        this.closeDate = closeDate;
    }

    public boolean isValidDate() {
        LocalDateTime currentDate = LocalDateTime.now();
        if (openDate != null && openDate.isAfter(currentDate)) {
            return false;
        }
        if (closeDate != null && closeDate.isBefore(currentDate)) {
            return false;
        }
        return true;
    }

    public boolean isQuiz() {
        return quiz;
    }

    public void setQuiz(final boolean quiz) {
        this.quiz = quiz;
    }

    public boolean isSingleResponse() {
        return singleResponse;
    }

    public void setSingleResponse(final boolean singleResponse) {
        this.singleResponse = singleResponse;
    }

    public boolean isDisableAutosave() {
        return disableAutosave;
    }

    public void setDisableAutosave(final boolean disableAutosave) {
        this.disableAutosave = disableAutosave;
    }

    public List<SurveyPage> getPages() {
        if (null == pages) {
            if (getId() == 0) {
                return new ArrayList<>();
            }

            pages = Learnweb.dao().getSurveyDao().findPagesAndVariantsByResourceId(getId());
        }
        return pages;
    }
}
