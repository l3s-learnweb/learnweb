package de.l3s.learnweb.dashboard.tracker;

import org.apache.log4j.Logger;
import java.time.format.DateTimeFormatter;

class TrackerDashboardChartsFactory
{
    private static final Logger log = Logger.getLogger(TrackerDashboardChartsFactory.class);
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
}
