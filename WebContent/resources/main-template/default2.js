/****************************************************************
This file will be loaded on every page. 
Include only methods which are required on every page.
****************************************************************/

/**
 * Method required for the search field
 */
function removeViewstate(searchForm)
{
	$(searchForm).find("[name='javax.faces.ViewState']").remove();
}

function footerToggle()
{
	var footer = $('#footer');
	
	if(footer.css('display') == 'none')
		setPreference('helptext_hide', 'false');
	else
		setPreference('helptext_hide', 'true');
	
	footer.slideToggle();

	return false;
}

function footerClose()
{
	$('#footer').hide();
	setPreference('helptext_hide', 'true');
	return false;
}

var defaultVisible = 8;

function updateCarousel() {
	$('.filter-vCarousel').each(function () {
		var $wrapper = $(this);
		var $container = $($wrapper.find('.vCarousel-container'));
		var height = $(":first-child", $container).outerHeight(true);
		
		var totalRecords = $container.children().length;
        if (defaultVisible < totalRecords) {
        	$wrapper.addClass('vCarousel-small');
        	$container.css({ "max-height" : (defaultVisible - 1)*height });
        	
            $($wrapper.find('.vCarousel-expand')).on('click', function(e) {
            	e.preventDefault();
            	$wrapper.addClass('vCarousel-expanded');
            	$container.css({ "max-height" : defaultVisible*height });
                return false;
            });
        }
	});
}

function setPreference(prefKey, prefValue)
{
	setPreferenceRemote([{name:'key', value:prefKey}, {name:'value', value:prefValue}]);
}

var myMarket = "en-US";

$(document).ready(function()
{	
	$("#group_menu > .panelmenu").each(function( index ) 
	{
		var group = $(this);
		var links = group.children('div');
		
		group.find('h3 .iconarrow').click(function()
		{
			links.slideToggle();
			group.toggleClass('active');
			return true;
		});		
	});

	
	if(typeof market != 'undefined')
	{
		if(market == "de")
			myMarket = "de-DE";
		else if(market == "pt")
			myMarket = "pt-BR";
		else if(market == "it")
			myMarket = "it-IT";
	}
	
	$("#searchfield").autocomplete({
        source: function (request, response) {
            $.ajax({
                url: "http://api.bing.com/osjson.aspx?Query=" + encodeURIComponent(request.term) + "&Market="+ myMarket +"&JsonType=callback&JsonCallback=?",
                dataType: "jsonp",
    
                success: function (data) {
                    var suggestions = [];
                    $.each(data[1], function (i, val) {
                        suggestions.push(val);
                    });
                    response(suggestions);
               
                    var logQuerySuggestionAsync = function() {
                    	logQuerySuggestion([{name:'query', value:request.term},{name:'suggestions', value:suggestions},{name:'market', value:myMarket}]);
                    };
                    setTimeout(logQuerySuggestionAsync, 0);
                }
            });
        }
    });
});
