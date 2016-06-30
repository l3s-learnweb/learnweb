/****************************************************************
This file will be loaded on every page. 
Include only methods which are required on every page.
****************************************************************/

/**
 * Method required for the search field
 */
var lightboxActiveResource = null;
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

var box;

function lightbox_close()
{
	box.hide();
	box.detach();
}

function lightbox_next()
{
	if(lightboxActiveResource == null)
		return;
		
	var next = $(lightboxActiveResource).next('.resource');	
	
	if(!next.hasClass('resource')) {
		next = $('#gallery .resource').first();
	}
	lightboxActiveResource = next;
	$('#lightbox_content .resource').remove();

	$('#lightbox_content').append(lightboxActiveResource.clone());
	lightbox_open();
	//next.trigger('openLightbox');
}

function lightbox_prev()
{
	if(lightboxActiveResource == null)
		return;
		
	var prev = lightboxActiveResource.prev('.resource');	
	
	// test if current resource is first resource
	if(!prev.hasClass('resource')) {
		prev = $('#gallery .resource').last();
	}
	lightboxActiveResource = prev;
	$('#lightbox_content .resource').remove();

	$('#lightbox_content').append(lightboxActiveResource.clone());
	lightbox_open();
	//prev.trigger('openLightbox');	
}

function lightbox_resize_container()
{	
	// resize lightbox container
	var height = $(window).height() - 167;
	
	if(height < 200)
		height = 200;
	
//	var titleHeight = $('#lightbox_title').height() + 10;
	
	$('#lightbox_container').height(height);
//	$('#lightbox_content').height(height-titleHeight);
}


function lightbox_load()
{
	box = $('#lightbox');
	lightboxActiveResource = $("#gallery .resource").first();
	$('#lightbox_content').append(lightboxActiveResource.clone());
}
function lightbox_open()
{
	//lightboxActiveResource;	

	box.appendTo(document.body);
	lightbox_resize_container();
	box.show();
};

// store preferences in user account settings
function setPreference(prefKey, prefValue)
{
	setPreferenceRemote([{name:'key', value:prefKey}, {name:'value', value:prefValue}]);
}

var myMarket = "en-US";

$(document).ready(function()
{	
	lightbox_load();
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

	// initialize search field auto completion
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
