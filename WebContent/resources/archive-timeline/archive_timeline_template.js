//Slide transitions between calendar and timeline view
function open_timeline(){
	$('#calendar').hide("slide", { direction: "right" }, 1000);
	$('#container').show("slide", { direction: "left" }, 1000);
	return false;
}

//Creation of list of links to the archived versions
function archiveVersionsList(thisDayEvent){
	var no_of_versions = thisDayEvent.dayEvents.length;
	var list = "<ul style='padding-left:10px;list-style-type: circle;'>";
	for(var i=0;i<no_of_versions;i++){
		list += "<li><a href='" + thisDayEvent.dayEvents[i].url + "' target='_blank'>"+thisDayEvent.dayEvents[i].time+"</a></li>";
	}
	list +="</ul>";
	return list;
	
}

function addLeadingZero(num) {
	if (num < 10) {
		return "0" + num;
	} else {
		return "" + num;
	}
}

/*function handleCalendarData(xhr, status, args)
{	
	calendar_data = args.calendarData;
	calendar_data = JSON.parse(calendar_data);
	loadCalendar(calendar_data);
}

function handleJsonData(xhr, status, args)
{	
	data_var = args.timelineData;
	loadTimeline();
}*/

function loadTimeline(data_var){
	//jsondata = JSON.parse(data_var);
	
	$('#container').highcharts({
		chart: {
			renderTo: 'container',
			zoomType: 'x'
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
							$('#calendar').show("slide", { direction: "right" }, 1000);
							$('#container').hide("slide", { direction: "left" }, 1000);
							$('.responsive-calendar').responsiveCalendar(year+'-'+ month);
						}
					}
				}
			}
		},

		series: [{
			type: 'column',
			name: timeline_series_name ,
			data: data_var,
		}]
	});
	
	$('#timeline_view').hide();

}