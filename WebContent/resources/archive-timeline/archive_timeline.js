
function archiveVersionsList(thisDayEvent){
	var no_of_versions = thisDayEvent.dayEvents.length;
	var list = "<ul>";
	for(var i=0;i<no_of_versions;i++){
		list += "<li><a href='" + thisDayEvent.dayEvents[i].url + "'>"+thisDayEvent.dayEvents[i].time+"</a></li>";
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

function json(data_var){
	$('#container').highcharts({
		chart: {
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
				cursor: 'pointer',
				point: {
					events: {
						click: function () {
				
							var date = new Date(this.x);
							var year = date.getFullYear();
							var month = date.getMonth() + 1;
							$('.responsive-calendar').responsiveCalendar(year+'-'+ month);
							
						}
					}
				}
			},
			area: {
				fillColor: {
					linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1},
					stops: [
					        [0, Highcharts.getOptions().colors[0]],
					        [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
					        ]
				},
				marker: {
					radius: 2
				},
				lineWidth: 1,
				states: {
					hover: {
						lineWidth: 1
					}
				},
				threshold: null
			}
		},

		series: [{
			type: 'column',
			name: 'Archive Versions',


			data: data_var,


		}]
	});
}