package de.l3s.learnweb.resource.web;

import java.io.Serial;
import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.ResourceDetailBean;
import de.l3s.learnweb.resource.archive.ArchiveUrl;
import de.l3s.learnweb.resource.archive.ArchiveUrlManager;

@Named
@ViewScoped
public class WebResourceBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -655001215017199006L;
    private static final Logger log = LogManager.getLogger(WebResourceBean.class);

    private WebResource resource;

    @PostConstruct
    public void init() {
        resource = (WebResource) Beans.getInstance(ResourceDetailBean.class).getResource();
    }

    public void archiveCurrentVersion() {
        boolean addToQueue = true;
        if (!resource.getArchiveUrls().isEmpty()) {
            // captured more than 5 minutes ago
            addToQueue = resource.getArchiveUrls().getLast().timestamp().isBefore(LocalDateTime.now().minusMinutes(5));
        }

        if (addToQueue) {
            final ArchiveUrlManager manager = Beans.getInstance(ArchiveUrlManager.class);
            String response = manager.addResourceToArchive(resource);
            if (response.equalsIgnoreCase("archive_success")) {
                addGrowl(FacesMessage.SEVERITY_INFO, "addedToArchiveQueue");
            } else if (response.equalsIgnoreCase("robots_error")) {
                addGrowl(FacesMessage.SEVERITY_ERROR, "archiveRobotsMessage");
            } else if (response.equalsIgnoreCase("generic_error")) {
                addGrowl(FacesMessage.SEVERITY_ERROR, "archiveErrorMessage");
            }
        } else {
            addGrowl(FacesMessage.SEVERITY_INFO, "archiveWaitMessage");
        }
    }

    /**
     * The method is used from JS in resource_view_archive_timeline.xhtml.
     */
    public String getArchiveTimelineJsonData() {
        TreeMap<LocalDate, Integer> monthlySeriesData = dao().getWaybackUrlDao().countSnapshotsGroupedByMonths(resource.getId(), resource.getUrl());
        JsonArray highChartsData = new JsonArray();
        monthlySeriesData.forEach((key, value) -> {
            JsonArray innerArray = new JsonArray();
            innerArray.add(key.toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC) * 1000);
            innerArray.add(value);
            highChartsData.add(innerArray);
        });
        return new Gson().toJson(highChartsData);
    }

    /**
     * The method is used from JS in resource_view_archive_timeline.xhtml.
     */
    public String getArchiveCalendarJsonData() {
        JsonObject archiveDates = new JsonObject();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
        DateTimeFormatter localizedDateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

        TreeMap<LocalDate, Integer> dailySeriesData = dao().getWaybackUrlDao().countSnapshotsGroupedByDays(resource.getId(), resource.getUrl());
        for (final Map.Entry<LocalDate, Integer> entry : dailySeriesData.entrySet()) {
            JsonObject archiveDay = new JsonObject();
            archiveDay.addProperty("number", entry.getValue());
            archiveDay.addProperty("badgeClass", "badge-warning");

            List<ArchiveUrl> archiveUrlsData = dao().getArchiveUrlDao().findByResourceId(resource.getId(), entry.getKey());
            archiveUrlsData.addAll(dao().getWaybackUrlDao().findByUrl(resource.getUrl(), entry.getKey()));

            JsonArray archiveVersions = new JsonArray();
            for (ArchiveUrl archiveUrl : archiveUrlsData) {
                JsonObject archiveVersion = new JsonObject();
                archiveVersion.addProperty("url", archiveUrl.archiveUrl());
                archiveVersion.addProperty("time", localizedDateFormat.format(archiveUrl.timestamp()));
                archiveVersions.add(archiveVersion);
            }
            archiveDay.add("dayEvents", archiveVersions);
            archiveDates.add(dateFormat.format(entry.getKey()), archiveDay);
        }
        return new Gson().toJson(archiveDates);
    }

    /**
     * Function to get short week day names for the calendar.
     */
    public List<String> getShortWeekDays() {
        DateFormatSymbols symbols = new DateFormatSymbols(getUserBean().getLocale());
        List<String> dayNames = Arrays.asList(symbols.getShortWeekdays());
        Collections.rotate(dayNames.subList(1, 8), -1);
        return dayNames.subList(1, 8);
    }

    /**
     * Function to localized month names for the calendar.
     * The method is used from JS in resource_view_archive_timeline.xhtml
     */
    public String getMonthNames() {
        DateFormatSymbols symbols = new DateFormatSymbols(getUserBean().getLocale());
        JsonArray monthNames = new JsonArray();
        for (String month : symbols.getMonths()) {
            if (!month.isBlank()) {
                monthNames.add(month);
            }
        }
        return new Gson().toJson(monthNames);
    }

    /**
     * Function to get localized short month names for the timeline.
     * The method is used from JS in resource_view_archive_timeline.xhtml
     */
    public String getShortMonthNames() {
        DateFormatSymbols symbols = new DateFormatSymbols(getUserBean().getLocale());
        JsonArray monthNames = new JsonArray();
        for (String month : symbols.getShortMonths()) {
            if (!month.isBlank()) {
                monthNames.add(month);
            }
        }
        return new Gson().toJson(monthNames);
    }
}
