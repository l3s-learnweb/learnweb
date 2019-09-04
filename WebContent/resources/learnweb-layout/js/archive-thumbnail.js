var timeMaps = {};
var maxItemHeight = 325;

function loadTimeMaps()
{
	timeMaps = {};
	var j = 0;
	$('.timeline-event-box').each(function(i, obj) {
		if($(obj).hasClass("selected"))
			timeMaps[i] = j++;
	});	
}

function timelineEventSelect()
{
	if(Object.keys(timeMaps).length === 0)
    	PF('contentFlow').cf.moveTo(PF('tmlnbar').getSelectedIndex());
	else if(Object.keys(timeMaps).length > 0)
		PF('contentFlow').cf.moveTo(timeMaps[PF('tmlnbar').getSelectedIndex()]);
}

function loadCF(){
	var cf = new ContentFlow('contentFlow', {
		reflectionColor: "#000000",
		circularFlow: false,
		visibleItems: 3,
		scrollWheelSpeed: 1.5,
		startItem: 'visible',
		reflectionHeight: 0.3,
		maxItemHeight: maxItemHeight
	});
	cf._init();
}

function loadMaxItemSize(){
	
	if(screen.width < 1300)
		maxItemHeight = 325;
	else if(screen.width >= 1300 && screen.width < 1600)
		maxItemHeight = 425;
	else if(screen.width >=1600 && screen.width < 2100)
		maxItemHeight = 600;
	else if(screen.width >=2100)
		maxItemHeight = 700;
}

$(document).ready(function(){
	loadMaxItemSize();
	// loadCF();
	loadTimeMaps();
});