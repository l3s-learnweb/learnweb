package de.l3s.learnweb.dashboard.tracker;

import java.time.format.DateTimeFormatter;

import org.apache.log4j.Logger;

class TrackerDashboardChartsFactory
{
    private static final Logger log = Logger.getLogger(TrackerDashboardChartsFactory.class);
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
}
