package de.l3s.learnweb.resource.web;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.CartesianTime;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.data.NumericPoint;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.primefaces.model.charts.optionconfig.title.Title;
import org.primefaces.model.charts.optionconfig.tooltip.Tooltip;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.component.charts.BarTimeChartDataSet;
import de.l3s.learnweb.component.charts.CartesianTimeImpl;
import de.l3s.learnweb.resource.ResourceDetailBean;
import de.l3s.learnweb.resource.archive.ArchiveUrl;
import de.l3s.learnweb.resource.archive.ArchiveUrlManager;

@Named
@ViewScoped
public class WebResourceBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -655001215017199006L;

    private WebResource resource;

    private transient LocalDate selectedDate;
    private transient BarChartModel timelineModel;
    private transient ScheduleModel calendarModel;
    private transient List<NumericPoint> timelineData;

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
                if (response) {
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public BarChartModel getArchiveTimelineModel() {
        if (timelineModel == null) {
            CartesianScales scales = new CartesianScales();
            CartesianLinearAxes xAxes = new CartesianLinearAxes();
            xAxes.setType("time");
            CartesianTime time = new CartesianTimeImpl();
            time.setTooltipFormat("MMMM yyyy");
            time.setMinUnit("month");
            xAxes.setTime(time);
            scales.addXAxesData(xAxes);

            Title title = new Title();
            title.setDisplay(true);
            title.setText(getLocaleMessage("archive.timeline_click_zoom"));

            BarChartOptions options = new BarChartOptions();
            options.setScales(scales);
            options.setTitle(title);

            Tooltip tooltip = new Tooltip();
            tooltip.setMode("index");
            tooltip.setIntersect(false);
            options.setTooltip(tooltip);
            Legend legend = new Legend();
            legend.setDisplay(false);
            options.setLegend(legend);

            BarTimeChartDataSet dataSet = new BarTimeChartDataSet();
            dataSet.setLabel(getLocaleMessage("archive.timeline_series_name"));
            dataSet.setBackgroundColor("rgb(74,163,130)");

            List<Object> dataVal = (List<Object>) (List) getTimelineData();
            dataSet.setData(dataVal);

            ChartData data = new ChartData();
            data.addChartDataSet(dataSet);

            timelineModel = new BarChartModel();
            timelineModel.setData(data);
            timelineModel.setOptions(options);
            timelineModel.setExtender("chartExtender");
        }

        return timelineModel;
    }

    private List<NumericPoint> getTimelineData() {
        if (timelineData == null) {
            TreeMap<LocalDate, Integer> monthlySeriesData = dao().getWaybackUrlDao().countSnapshotsGroupedByMonths(resource.getId(), resource.getUrl());

            timelineData = new ArrayList<>();
            monthlySeriesData.forEach((key, value) -> timelineData.add(new NumericPoint(key.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(), value)));

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
