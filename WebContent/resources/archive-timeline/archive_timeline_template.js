var data_var = "";
function open_timeline(){
	$('#calendar').hide("slide", { direction: "right" }, 1000);
	$('#container').show("slide", { direction: "left" }, 1000);
	return false;
}
function archiveVersionsList(thisDayEvent){
	var no_of_versions = thisDayEvent.dayEvents.length;
	var list = "<ul>";
	for(var i=0;i<no_of_versions;i++){
		var time = new Date(thisDayEvent.dayEvents[i].time);
		list += "<li><a href='" + thisDayEvent.dayEvents[i].url + "'>"+time.toUTCString()+"</a></li>";
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
function handleCalendarData(xhr, status, args)
{	
	calendar_data = args.calendarData;
	calendar_data = JSON.parse(calendar_data);
	loadCalendar(calendar_data);
}

function handleJsonData(xhr, status, args)
{	
	data_var = args.timelineData;
	loadTimeline();
}



function loadTimeline(){
	jsondata = JSON.parse(data_var);
	$('#container').highcharts({
		chart: {
			renderTo: 'container',
			zoomType: 'x'
		},
		title: {
			text: 'Archive Url Versions'
		},
		subtitle: {
			text: document.ontouchstart === undefined ?
					'Click and drag in the plot area to zoom in' :
						'Pinch the chart to zoom in'
		},
		xAxis: {
			type: 'datetime',
			//tickInterval: 30 * 24 * 3600000,
			minRange:  24 * 3600000 // fourteen days
		},
		yAxis: {
			title: {
				text: 'Number of versions'
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
			name: 'Archive Versions',
			data: jsondata,
		}]
	});
	
	

}