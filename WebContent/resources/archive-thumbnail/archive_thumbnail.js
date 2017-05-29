var cf;
var timeMaps = {};

function loadTimeMaps()
{
	timeMaps = {}
	var j = 0;
	$('.timeline-event-box').each(function(i, obj) {
		if($(obj).hasClass("selected"))
			timeMaps[i] = j++;
	});	
}

function timelineEventSelect()
{
	if(Object.keys(timeMaps).length === 0)
    	cf.moveTo(PF('tmlnbar').getSelectedIndex());
	else if(Object.keys(timeMaps).length > 0)
		cf.moveTo(timeMaps[PF('tmlnbar').getSelectedIndex()]);
}

function loadCF(){
	cf = new ContentFlow('contentFlow', {reflectionColor: "#000000", circularFlow: false, startItem: 'visible', reflectionHeight: 0.3, maxItemHeight:385});
	cf._init();
}

$(document).ready(function(){
	loadCF();
	loadTimeMaps();	
});