
var lightboxActiveResource = null;
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
	console.log('resize');
	
	// resize lightbox container
	var height = $(window).height() - 167;
	
	if(height < 200)
		height = 200;
	
	var titleHeight = 0;//$('#lightbox_title').height() + 10;
	
	$('#lightbox_container').height(height);
	$('#lightbox_content').height(height-titleHeight);
}

function lightbox_load()
{	
	box = $('#lightbox');
	lightboxActiveResource = $("#gallery .resource").first();
	$('#lightbox_content').append(lightboxActiveResource.clone());
}
function lightbox_open()
{	
	if(lightboxActiveResource == null){
		lightbox_load();
	}
	box.appendTo(document.body);
	lightbox_resize_container();
	box.show();
}

$(window).on('load', function()
{	
	setTimeout(function(){
	    $("[data-lazy]").each(function (index, el) {
	    	var element = $(el);
	    	var url = element.attr('data-lazy');
	    	element.attr('data', url);
	    });	
	}, 500);
});

$(document).resize(lightbox_resize_container);
