/**
 * Method required to open archive versions in a new tab 

function archive_url_select(){
	if(PF('archive_versions_menu').getSelectedValue() != 0)
		window.open(PF('archive_versions_menu').getSelectedValue(),'_blank');
} */

function open_timeline_view(){
	$('#timeline_view').slideToggle("slow",function(){scroller();});
	return false;
}

function open_list_view(){
	$('#archive_list_view').slideToggle("slow",function(){scroller();});
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
	var height = $(window).height() - 137;
	
	if(height < 200)
		height = 200;
	
	var titleHeight = 0;//$('#lightbox_title').height() + 10;
	
	$('#lightbox_container').height(height);
	$('#lightbox_content').height(height-titleHeight);
}

function lightbox_load()
{
	box = $('#lightbox');
	console.log('load box');
}
function lightbox_open()
{
	//var box = $('#lightbox');
	
	box.appendTo(document.body);
	lightbox_resize_container();
	box.show();
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
});
