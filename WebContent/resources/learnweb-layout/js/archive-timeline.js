/* global Highcharts, no_of_versions, timeline_title, timeline_series_name, msgClickZoom, msgTouchZoom */

let chart; // handle to access the HighCharts methods


// Show timeline/list of snapshots in resource_view
function archiveOpenTimeline() {
  const $archiveListBtn = $('#archive_list_btn');
  const $archiveTimelineBtn = $('#archive_timeline_btn');
  const $archiveListView = $('#archive_list_view');
  const $archiveTimelineView = $('#archive_timeline_view');

  if ($archiveListView.is(':visible')) {
    $archiveListView.slideToggle('slow');
    $archiveListBtn.removeClass('ui-state-active');
  }

  $archiveTimelineView.slideToggle('slow', () => {
    $archiveTimelineBtn.addClass('ui-state-active');
    if ($archiveTimelineView.is(':visible')) {
      const $container = $('#archive_timeline_container');
      $container.show().width($archiveTimelineView.width());
      chart.setSize($archiveTimelineView.width(), $container.height());
      chart.reflow();
    }
  });
}

function archiveOpenList() {
  const $archiveListBtn = $('#archive_list_btn');
  const $archiveTimelineBtn = $('#archive_timeline_btn');
  const $archiveListView = $('#archive_list_view');
  const $archiveTimelineView = $('#archive_timeline_view');

  if ($archiveTimelineView.is(':visible')) {
    $archiveTimelineView.slideToggle('slow');
    $archiveTimelineBtn.removeClass('ui-state-active');
  }

  $archiveListView.slideToggle('slow', () => {
    $archiveListBtn.addClass('ui-state-active');
  });
}

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
function archiveVersionsList(thisDayEvent) {
  let list = "<ul style='padding-left:10px;list-style-type: circle;'>";
  for (let i = 0, l = thisDayEvent.dayEvents.length; i < l; i++) {
    list += `<li><a href='${thisDayEvent.dayEvents[i].url}' target='_blank'>${thisDayEvent.dayEvents[i].time}</a></li>`;
  }
  list += '</ul>';
  return list;
}

function addLeadingZero(num) {
  if (num < 10) {
    return `0${num}`;
  }
  return `${num}`;
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
      text: timeline_title,
    },
    subtitle: {
      text: document.ontouchstart === undefined
        ? msgClickZoom : msgTouchZoom,
    },
    xAxis: {
      type: 'datetime',
      // tickInterval: 30 * 24 * 3600000,
      minRange: 365 * 24 * 3600000, // 1 year
    },
    yAxis: {
      title: {
        text: no_of_versions,
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
        color: '#489a83',
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
      name: timeline_series_name,
      data: dataVar,
    }],
  });
  $('.highcharts-container').css('overflow', '');
  $('#archive_timeline_view').hide();
}
