package de.l3s.learnweb.resource.web;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.TreeMap;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.omnifaces.util.Beans;
import org.primefaces.event.ItemSelectEvent;
import org.primefaces.model.DefaultScheduleEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleModel;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.ResourceDetailBean;
import de.l3s.learnweb.resource.archive.ArchiveUrl;
import de.l3s.learnweb.resource.archive.ArchiveUrlManager;
import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.color.RGBAColor;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.datapoint.XYDataPoint;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.options.BarOptions;
import software.xdev.chartjs.model.options.LegendOptions;
import software.xdev.chartjs.model.options.Plugins;
import software.xdev.chartjs.model.options.Title;
import software.xdev.chartjs.model.options.tooltip.TooltipOptions;

@Named
@ViewScoped
public class WebResourceBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -655001215017199006L;

    private WebResource resource;

    private transient LocalDate selectedDate;
    private transient String timelineModel;
    private transient ScheduleModel calendarModel;
    private transient ArrayList<XYDataPoint> timelineData;

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
            try {
                final ArchiveUrlManager manager = Beans.getInstance(ArchiveUrlManager.class);
                Boolean response = manager.addResourceToArchive(resource);
                if (Boolean.TRUE.equals(response)) {
                    addGrowl(FacesMessage.SEVERITY_INFO, "addedToArchiveQueue");
                } else {
                    addGrowl(FacesMessage.SEVERITY_ERROR, "archiveErrorMessage");
                }
            } catch (IOException e) {
                addGrowl(FacesMessage.SEVERITY_ERROR, "archiveRobotsMessage");
            }
        } else {
            addGrowl(FacesMessage.SEVERITY_INFO, "archiveWaitMessage");
        }
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(final LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    public void timelineItemSelect(ItemSelectEvent event) {
        selectedDate = LocalDate.ofInstant(Instant.ofEpochMilli(getTimelineData().get(event.getItemIndex()).getX().longValue()), ZoneOffset.UTC);
    }

    public void backToTimeline() {
        selectedDate = null;
    }

    public String getArchiveTimelineModel() {
        if (timelineModel == null) {
            timelineModel = new BarChart()
                .setData(new BarData()
                    .addDataset(new BarDataset()
                        .setLabel(getLocaleMessage("archive.timeline_series_name"))
                        .setDataUnchecked(getTimelineData())
                        .setBackgroundColor(new RGBAColor(74, 163, 130))))
                .setOptions(new BarOptions()
                    .setPlugins(new Plugins()
                        .setTitle(new Title()
                            .setDisplay(true)
                            .setText(getLocaleMessage("archive.timeline_click_zoom")))
                        .setTooltip(new TooltipOptions()
                            .setMode("index"))
                        .setLegend(new LegendOptions()
                            .setDisplay(false))))
                .toJson();
        }

        return timelineModel;
    }

    private ArrayList<XYDataPoint> getTimelineData() {
        if (timelineData == null) {
            TreeMap<LocalDate, Integer> monthlySeriesData = dao().getArchiveUrlDao().countSnapshotsByMonths(resource.getId());

            timelineData = new ArrayList<>();
            monthlySeriesData.forEach((key, value) -> timelineData.add(new XYDataPoint(key.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(), value)));

            timelineModel = null; // make sure the model is recreated
        }
        return timelineData;
    }

    public ScheduleModel getArchiveCalendarModel() {
        if (calendarModel == null) {
            ScheduleModel model = new DefaultScheduleModel();

            for (final ArchiveUrl archiveUrl : resource.getArchiveUrls()) {
                DefaultScheduleEvent<?> scheduleEventAllDay = DefaultScheduleEvent.builder()
                    .url(archiveUrl.archiveUrl())
                    .startDate(archiveUrl.timestamp())
                    .build();
                model.addEvent(scheduleEventAllDay);
            }
            this.calendarModel = model;
        }
        return calendarModel;
    }
}
