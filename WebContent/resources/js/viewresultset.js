
var no_more_results = false;
var openPreview;
var resourceCounter = 0;
var lightboxActiveResource = null;
var gridItemWidth = 190;
var tabActiveindex = 0;
var loadAheadPx = 0;

var step = 200;
var scrolling = false;

//jquery extension: uncomment function 
(function($) {
	$.fn.uncomment = function() {
		$(this).contents().each(function() {
			if ( this.nodeType === 8 ) {
				$(this).replaceWith(this.nodeValue);
				/*
				// Need to "evaluate" the HTML content,
				// otherwise simple text won't replace
				var e = $('<span>' + this.nodeValue + '</span>');
				$(this).replaceWith(e.contents());
				*/
			}
			/*
			else if ( this.hasChildNodes() ) {
				$(this).uncomment(recurse);
			} */
		});
	};
})(jQuery);


function prepareResources(resources)
{
	var padding = 9;
	var mindist = 4;
	
	resources.each(function()
	{				
		var resource = $(this);		
		
		var tempResourceId = resource.attr('id').substr(9);
		
		var image = resource.find('.smallImage').first();
		var preview = resource.find('.preview').first();
		var previewImage = resource.find('.preview img').first();		
		
		// open preview delayed on mouseover 
		image.mouseenter(function (event) 
		{
			image.addClass("hasFocus");			
			
		    setTimeout(function()
		    {	   	
		    	if(image.hasClass("hasFocus")) // open preview
		    	{				
		    		var offset = image.offset();		    		
		    		var width = preview.width() + 2*padding;
		    		var heightDiff = padding + (previewImage.attr('height') - image.height())/2;						
					var widthDiff = padding + (previewImage.attr('width') - image.width())/2;					
					var headerHeight = $('#header').height() + 5;
					var scrollTop = $(window).scrollTop();
					
					offset.left -= widthDiff;
					offset.top -= heightDiff + scrollTop;
					
					var containerWidth = $('#explore_resultset').outerWidth(true); 
					var containerHeight = $(window).height();				
					var previewHeight = preview.height() + mindist + 2*padding;
					
					if(offset.left < mindist)
						offset.left = mindist;					
					else if(offset.left + width + mindist > containerWidth)
						offset.left = containerWidth - width - mindist;					
					
					//console.log(offset.top, previewHeight, containerHeight, headerHeight, scrollTop);
					
					if(offset.top < headerHeight)
						offset.top = headerHeight;						
					else if(offset.top + previewHeight > containerHeight)
						offset.top = containerHeight - previewHeight;

					offset.top += scrollTop;
					
					openPreview.hide();
					openPreview = preview;
				
					preview.show();	
					preview.offset(offset);      		        	
		        }
			}, 320 );
		});
		
		image.mouseleave(function () {
			image.removeClass("hasFocus");
		});
		
		preview.mouseleave(function () {
			preview.hide();
		});			

		// get content for Lightbox
		var metadata = resource.find('.metadata').first();
		var embedded = metadata.children('.embedded').first();		
		var description = metadata.children('.description').first();
		var title = metadata.children('.title').first();
		var options = metadata.children('.options').first();
		var snippet = metadata.children('.snippet').first();
		
		metadata.remove();		
		
		var lightbox_open = function()
		{				
			openPreview.hide();	
			lightboxActiveResource = resource;
			
			$('#lightbox').show();
			$('#lightbox_metadata').empty();
			$('#lightbox_metadata').append(description);
			$('#lightbox_metadata').append(options);
			$('#lightbox_metadata').append(snippet);
			$('#lightbox_content .embedded').remove();
			$('#lightbox_content').append(embedded);			
			$('#lightbox_title').empty();
			$('#lightbox_title').append(title);

			embedded.uncomment(); // embedded content is out commented to prevent loading images/videos before opening lightbox
			var image = embedded.children().first();
			
			if(image.is('img')) // load image
			{
				if(image.attr('src') !== image.attr('original-src'))
			    {
					image.css("z-index",1104);
					// clone image and place it behind the small resolution image
				    var hdImage = $('<img />');
				    
				    hdImage.bind("load", function() 
				    {						    
                    	image.fadeOut(function() {
                    		image.remove();
                    	});            	
                    });				    
				    hdImage.css("z-index", 1103);				    
				    hdImage.attr('original_width', image.attr('original_width'));
				    hdImage.attr('original_height', image.attr('original_height'));

				    image.after(hdImage);
				    
				    hdImage.attr("src", image.attr('original-src'));				    				    
				}
			}
			lightbox_resize_container();
			lightbox_resize_content();
			
			if(typeof(tabViewer) !== 'undefined')
				tabActiveindex = tabViewer.getActiveIndex();
			
			//logResourceOpened([{name:'resource_id', value:tempResourceId}]);			
		};
		
		resource.on('openLightbox', lightbox_open);
		previewImage.mousedown(lightbox_open);
		image.mousedown(lightbox_open);	
	});	
	
	
	if(view === 'grid')
		resources.width(gridItemWidth);
	
	/*
		resizeGridResources();
	*/	
}

function resizeGridResources()
{ 
	var innerWidth = $('#explore_resultset').width() - 21;
	
	gridItemWidth = 100 / Math.floor($('#explore_resultset').innerWidth()/190) +'%';

	$('#explore_resultset .resource').width(gridItemWidth);
}

function lightbox_close()
{
	openPreview.hide();	
	
	$('#lightbox').hide();
	logEndTime([{name:'resource_id', value:lightboxActiveResource.attr('id').substring(9)}]); //To log lightbox close 
	$('#lightbox_content .embedded').remove();	

	lightboxActiveResource = null;
}

function lightbox_resize_container()
{	
	// resize lightbox container
	var height = $(window).height() - 137;
	
	if(height < 200)
		height = 200;
	
	var titleHeight = $('#lightbox_title').height() + 10;
	
	$('#lightbox_container').height(height);
	$('#lightbox_content').height(height-titleHeight);
	$('#lightbox_content .large').height(height-titleHeight);	
}

function lightbox_next()
{
	if(lightboxActiveResource == null)
		return
		
	var next = $(lightboxActiveResource).next();	
	
	if(!next.hasClass('resource')) {
		next = $('#explore_resultset .resource').first();
	}

	logEndTime([{name:'resource_id', value:lightboxActiveResource.attr('id').substring(9)}]); //To log lightbox close for a particular resource
	
	next.trigger('openLightbox');
}

function lightbox_prev()
{
	if(lightboxActiveResource == null)
		return;
		
	var prev = lightboxActiveResource.prev('.resource');	
	
	// test if current resource is first resource
	if(!prev.hasClass('resource')) {
		prev = $('#explore_resultset .resource').last();
	}

	logEndTime([{name:'resource_id', value:lightboxActiveResource.attr('id').substring(9)}]);

	prev.trigger('openLightbox');	
}

function lightbox_resize_content()
{	
	// resize and center lightbox content
	var outer = $('#lightbox_content');
	var inner = outer.find('.embedded').first().children();	

	if(inner.first().attr('width') === '100%' || inner.first().attr('type') === 'application/x-shockwave-flash')
	{
		inner.css({
		   position:'absolute',
		   left: 0,
		   top: 0,
		   width: outer.width(),
		   height: outer.height()
		});
		
		return;
	}

	if(typeof inner.first().attr('original_width') == 'undefined')
	{
		inner.attr('original_width', inner.width());
		inner.attr('original_height', inner.height());
	}	

	var iwidth = inner.first().attr('original_width');
	var iheight = inner.first().attr('original_height');		

	if(iwidth > outer.width()) {
		var ratio = outer.width() / iwidth;
		iheight = Math.ceil(iheight * ratio);
		iwidth = outer.width();
	}

	if(iheight > outer.height()) {
		var ratio = outer.height() / iheight;
		iwidth = Math.ceil(iwidth * ratio);
		iheight = outer.height();
	}

	inner.css({
	   position:'absolute',
	   left: (outer.width() - iwidth)/2,
	   top: (outer.height() - iheight)/2,
	   width: iwidth+'px',
	   height: iheight+'px'
	});
}

// call resizeend after the resize
var rtime = new Date(1, 1, 2000, 12, 0, 0);
var lastResize = new Date(1, 1, 2000, 12, 0, 0);
var timeout = false;
var delta = 250;

$(window).resize(function() 
{
 	
	rtime = new Date();
    if (timeout === false) {
        timeout = true;
        setTimeout(resizeend, delta);
    }
});

function resizeend() {
	
    if (new Date() - rtime < delta) {
        setTimeout(resizeend, delta);
    } 
    else {   	
    	if(view === 'grid')
    		resizeGridResources();
    	
    	//testIfResultsFillPage();
    	//lightbox_resize_container();
    	//lightbox_resize_content();
    	/*
    	// neccessary for ie and safari
    	setTimeout(function() {
    		resizeend();
    	}, 400);
    	*/
    	timeout = false;
    }               
}

function tabselection(current_tab){
	if(current_tab == "currentsearch"){
			exploretabViewer.select(1);
	}
}

function close_searchbar()
{	
	$('#explore_right_bar').animate({width:'0%'},1000);
	if(view === 'list')
	{
		$('.list_view').animate({width:'100%'},1000);
	}
	else if(view === 'float')
	{
		$('.float_view').animate({width:'100%'},1000);
	}
	else
	{
		$('.grid_view').animate({width:'100%'},1000);
	}
	$('#search_loading_more_results,#search_nothing_found,#search_no_more_results').css('width','100%');
	$('#explore_right_bar').removeClass('right_bar').css('display','none');
	return false;
}

function open_searchbar()
{	
	if(view === 'list')
	{
		$('#explore_right_bar').css('width','31%');
		$('.list_view').css('width','61%');
		$('#explore_right_bar').addClass('web_right_bar').css('display','inline-block');
		$('#search_loading_more_results,#search_nothing_found,#search_no_more_results').css('width','61%');	
	}
	else if(view === 'float')
	{
		$('#explore_right_bar').css('width','24%');
		$('.float_view').css('width','75%');
		$('#explore_right_bar').addClass('right_bar').css('display','inline-block');
		$('#search_loading_more_results,#search_nothing_found,#search_no_more_results').css('width','75%');
	}
	else
	{
		$('#explore_right_bar').css('width','24%');
		$('.grid_view').css('width','75%');
		$('#explore_right_bar').addClass('right_bar').css('display','inline-block');
		$('#search_loading_more_results,#search_nothing_found,#search_no_more_results').css('width','75%');
	}	

	
	return false;
}

$(document).ready(function() 
{	
	openPreview = $('#explorenew_results'); // initialize openPreview with an element that can be hidden.
	prepareResources($('#explore_resultset .resource'));	
	
	//lightbox_resize_container();
	
	if(view === 'grid' || view === "float"){
		resizeGridResources();
		$('.grid_view').css('width','75%');
		$('.float_view').css('width','75%');
	}
	else if(view === 'list')
		{				
			$('#explore_right_bar').removeClass('right_bar').addClass('web_right_bar');
			$('#search_loading_more_results,#search_nothing_found,#search_no_more_results').css('width','61%');
		}
	
	// register cursor left/right and esc key
	$(document).keydown(function(event) {
		if(event.which === 37)
			lightbox_prev();
		else if (event.which === 39)
			lightbox_next();
		else if (event.which === 27)
			lightbox_close();		
	});
	
	$('.resourceWebLink').click(function(){
		var tempResourceId = $(this).closest('div.resource').attr('id').substring(9);
		logResourceOpened([{name:'resource_id', value:tempResourceId}]);
		return true;
	});
});
