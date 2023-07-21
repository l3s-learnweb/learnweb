/* global Highcharts, translatedShortMonthsNames, translatedMonthsNames,
          msgTimelineNumVersions, msgTimelineTitle, msgTimelineSeriesName, msgTimelineClickZoom, msgTimelineTouchZoom */

let chart; // handle to access the HighCharts methods

// Slide transitions between calendar and timeline view
function returnToTimeline() {
  resizeChart();
  // $('#archive_timeline_calendar').hide("slide", { direction: "right" }, 1000);
  // $('#archive_timeline_container').show("slide", { direction: "left" }, 1000);
  $('#archive_timeline_container').show().fadeTo(1000, 1.0);
  $('#archive_timeline_calendar').fadeTo(1000, 0.0).hide();
  return false;
}

// Creation of list of links to the archived versions in the calendar
function archiveVersionsList(events) {
  let list = '<ul class=\'ps-2 mb-0\'>';
  for (let i = 0, l = events.length; i < l; i++) {
    list += `<li><a href='${events[i].url}' target='_blank'>${events[i].time}</a></li>`;
  }
  list += '</ul>';
  return list;
}

function addLeadingZero(num) {
  return num < 10 ? `0${num}` : `${num}`;
}

// To handle resizing of the timeline on window resize event
let id;
$(window).on('resize', () => {
  clearTimeout(id);
  id = setTimeout(() => {
    if ($('#archive_timeline_container').is(':visible')) {
      resizeChart();
    }
  }, 500);
});

// To resize the highchart to fit the container width on window resize
function resizeChart() {
  const $container = $('#archive_timeline_container');
  const $view = $('#archive_timeline_view');

  $container.width($view.width());
  chart.setSize($view.width(), $container.height());
  chart.reflow();
}

// Initialize highcharts with data from lw_resource_archiveurl
function loadTimeline(dataVar) {
  // jsondata = JSON.parse(dataVar);

  chart = new Highcharts.Chart({
    chart: {
      renderTo: 'archive_timeline_container',
      zoomType: 'x',
    },
    credits: {
      enabled: false,
    },
    title: {
      text: msgTimelineTitle,
    },
    subtitle: {
      text: document.ontouchstart === undefined ? msgTimelineClickZoom : msgTimelineTouchZoom,
    },
    xAxis: {
      type: 'datetime',
      // tickInterval: 30 * 24 * 3600000,
      minRange: 365 * 24 * 3600000, // 1 year
    },
    yAxis: {
      title: {
        text: msgTimelineNumVersions,
      },
    },
    tooltip: {
      headerFormat: '<b>{point.key}</b><br/>',
      xDateFormat: '%b, %Y',
    },
    legend: {
      enabled: false,
    },
    plotOptions: {
      series: {
        color: '#4aa382',
        cursor: 'pointer',
        point: {
          events: {
            click() {
              const date = new Date(this.x);
              const year = date.getFullYear();
              const month = date.getMonth() + 1;
              // $('#archive_timeline_calendar').show("slide", { direction: "right" }, 1000);
              $('#archive_timeline_calendar').show().fadeTo(1000, 1.0);
              $('#archive_timeline_container').fadeTo(1000, 0.0).hide();
              // $('#archive_timeline_container').hide("slide", { direction: "left" }, 1000);
              $('.responsive-calendar').responsiveCalendar(`${year}-${month}`);
            },
          },
        },
      },
    },

    series: [{
      type: 'column',
      name: msgTimelineSeriesName,
      data: dataVar,
    }],
  });
  $('.highcharts-container').css('overflow', '');
}

function loadCalendar(calendarData) {
  $('.responsive-calendar').responsiveCalendar({
    allRows: false,
    translateMonths: translatedMonthsNames,
    events: calendarData,
    onActiveDayHover(events) {
      const theDate = `${this.dataset.year}-${addLeadingZero(this.dataset.month)}-${addLeadingZero(this.dataset.day)}`;

      const $calendarCell = $(this).parent();
      if (!$calendarCell.hasClass('has-popover')) {
        const tooltip = $calendarCell.popover({
          trigger: 'click',
          html: true,
          content: $(archiveVersionsList(events[theDate].dayEvents)),
        }).addClass('has-popover');

        $(window).on('mousedown', () => {
          tooltip.popover('hide');
        });
      }
    },
  });
}
