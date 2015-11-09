function scrollToElement(element)
{
	//$('#right_pane .content').animate({ scrollTop: (element.offset().top + element.height() + 5 - $('#center_pane .content').height())}, 'slow');
	//console.log(element.offset().top, element.position().top, $('#center_pane .content').height(), element.height());
}

function open_timeline_view(){
	if($('#archive_list_view').is(':visible'))
	{
		$('#archive_list_view').slideToggle("slow");
		$('#list_button').toggleClass('button-active');
	}

	$('#timeline_view').slideToggle("slow",function(){
		scroller();
		$('#timeline_button').toggleClass("button-active");
		if($('#timeline_view').is(':visible')){
			$('#container').width($('#timeline_view').width());
			chart.setSize($('#timeline_view').width(), $('#container').height());
			chart.reflow();
			scrollToElement($('#timeline_view'));
		}
	});
	return false;
}

function open_list_view(){
	if($('#timeline_view').is(':visible'))
	{
		$('#timeline_view').slideToggle("slow");
		$('#timeline_button').toggleClass('button-active');
	}

	$('#archive_list_view').slideToggle("slow",function(){
		scroller();
		$('#list_button').toggleClass("button-active");
		
		if($('#archive_list_view').is(':visible')){
			scrollToElement($('#archive_list_view'));
		}
	});
	return false;
}


var box;

function lightbox_close()
{
	box.hide();
	box.detach();
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
}
function lightbox_open()
{
	box.appendTo(document.body);
	lightbox_resize_container();
	box.show();
};

function prepareCommentButton()
{
	var button = $('#comment_button');
	
	button.hide();
	
	$('#commentfield').focus(function() {
		button.slideDown();
	});
	$('#comment_form').focusout(function() {
		$('#comment_button').slideUp(1000);
	});
}

function update_url(resource_id)
{
	var updated_url = window.location.protocol + "//" + window.location.host + window.location.pathname + "?";
	var query_params = window.location.search.substring(1);
	if(query_params.indexOf("resource_id") > -1)
	{	
		var params = query_params.split("&");
		for(var i=0; i<params.length;i++)
		{
			var param = params[i].split("=");
			if(param[0] == "resource_id")
				updated_url += param[0] + "=" + resource_id + "&";
			else
				updated_url += param[0] + "=" + param[1] + "&";
		}
	}
	else
		updated_url += query_params + "&resource_id=" + resource_id + "?"; 
	updated_url = updated_url.substring(0, updated_url.length - 1);
	window.history.pushState({"url":window.location.href}, "resource_id" + resource_id, updated_url);
	//document.title = resource_title;
}

//To detect if its an initial page load or a reload from the History entry in Safari.
var popped = ('state' in window.history), initialURL = location.href;
window.onpopstate = function(e){
	var initialPop = !popped && location.href == initialURL;
	popped = true;
	if(initialPop) return;
	
	location.reload(true);
};

$(document).ready(function() 
{				
	//lightbox_resize_container();	
	$(window).resize(lightbox_resize_container);
	
	// register  esc key
	$(document).keydown(function(event) {
		if (event.which == 27)
			lightbox_close();		
	});
	
	if(document.getElementById("#container") != null)
		loadCharts();
});
