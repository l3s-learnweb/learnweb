var chart; //handle to access the HighCharts methods

//Slide transitions between calendar and timeline view
function returnToTimeline() {
    resizeChart();
    //$('#archive_timeline_calendar').hide("slide", { direction: "right" }, 1000);
    //$('#archive_timeline_container').show("slide", { direction: "left" }, 1000);
    $('#archive_timeline_container').show().fadeTo(1000, 1.0);
    $('#archive_timeline_calendar').fadeTo(1000, 0.0).hide();

    return false;
}

//Creation of list of links to the archived versions in the calendar
function archiveVersionsList(thisDayEvent) {
    var no_of_versions = thisDayEvent.dayEvents.length;
    var list = "<ul style='padding-left:10px;list-style-type: circle;'>";
    for (var i = 0; i < no_of_versions; i++) {
        list += "<li><a href='" + thisDayEvent.dayEvents[i].url + "' target='_blank'>" + thisDayEvent.dayEvents[i].time + "</a></li>";
    }
    list += "</ul>";
    return list;
}

function addLeadingZero(num) {
    if (num < 10) {
        return "0" + num;
    } else {
        return "" + num;
    }
}

//To handle resizing of the timeline on window resize event
var id;
$(window).on('resize', function () {
    clearTimeout(id);
    id = setTimeout(function () {
        if ($('#archive_timeline_container').is(':visible')) {
            resizeChart();
        }
    }, 500);
});

// To resize the highchart to fit the container width on window resize
function resizeChart() {
    var $container = $('#archive_timeline_container');
    var $view = $('#archive_timeline_view');

    $container.width($view.width());
    chart.setSize($view.width(), $container.height());
    chart.reflow();
}

// Initialize highcharts with data from lw_resource_archiveurl
function loadTimeline(data_var) {
    //jsondata = JSON.parse(data_var);

    chart = new Highcharts.Chart({
        chart: {
            renderTo: 'archive_timeline_container',
            zoomType: 'x'
        },
        credits: {
            enabled: false
        },
        title: {
            text: timeline_title
        },
        subtitle: {
            text: document.ontouchstart === undefined ?
                msgClickZoom : msgTouchZoom
        },
        xAxis: {
            type: 'datetime',
            //tickInterval: 30 * 24 * 3600000,
            minRange: 365 * 24 * 3600000 // 1 year
        },
        yAxis: {
            title: {
                text: no_of_versions
            }
        },
        tooltip: {
            headerFormat: '<b>{point.key}</b><br/>',
            xDateFormat: '%b, %Y'


        },
        legend: {
            enabled: false
        },
        plotOptions: {
            series: {
                color: '#489a83',
                cursor: 'pointer',
                point: {
                    events: {
                        click: function () {
                            var date = new Date(this.x);
                            var year = date.getFullYear();
                            var month = date.getMonth() + 1;
                            //$('#archive_timeline_calendar').show("slide", { direction: "right" }, 1000);
                            $('#archive_timeline_calendar').show().fadeTo(1000, 1.0);
                            $('#archive_timeline_container').fadeTo(1000, 0.0).hide();
                            //$('#archive_timeline_container').hide("slide", { direction: "left" }, 1000);
                            $('.responsive-calendar').responsiveCalendar(year + '-' + month);
                        }
                    }
                }
            }
        },

        series: [{
            type: 'column',
            name: timeline_series_name,
            data: data_var
        }]
    });
    $('.highcharts-container').css('overflow', '');
    $('#archive_timeline_view').hide();
}