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

function update_url(resource_id, folder_id)
{
	var page_schema = location.protocol + "//" + location.host + location.pathname;
	var query_params = location.search;
	
	if (folder_id == undefined) {
		folder_id = $("#folderGrid").data("selectedFolderid");
	}
	
	if (folder_id != undefined) {
		query_params = updateUrlParameters(query_params, "folder_id", folder_id);
	}
	
	if (resource_id != undefined) {
		query_params = updateUrlParameters(query_params, "resource_id", resource_id);
	}
	
	updated_url = page_schema + query_params;
	
	window.history.pushState({"url": location.href}, "resource_id" + resource_id, updated_url);
	popped = true;
	//document.title = resource_title;
}

function updateUrlParameters(url, key, value) {
	var re = new RegExp("([?&])" + key + "=.*?(&|$)", "i");
	var separator = url.indexOf('?') !== -1 ? "&" : "?";
	if (url.match(re)) {
		return url.replace(re, '$1' + key + "=" + value + '$2');
	} else {
		return url + separator + key + "=" + value;
	}
}

//To detect if its an initial page load or a reload from the History entry in Safari.
var popped = false, initialURL = location.href;
window.onpopstate = function(e){
	var initialPop = !popped && location.href == initialURL;
	popped = true;
	if(initialPop) return;
	
	location.reload(true);
};



function dropHandle(event, ui) {
	var destFolderId = $(this).is("[data-folderId]") ? $(this).attr("data-folderId") : $(this).parents(".ui-treenode").attr("data-datakey");
	
	if (ui.draggable.is("[data-resourceId]")) {
		var resourceId = ui.draggable.attr("data-resourceId");
		console.log("Resource " + resourceId + " moved to folder " + destFolderId);
   
		moveToFolder([
          {name: 'destFolderId', value: destFolderId},
          {name: 'type', value: 'resource'},
          {name: 'objectId', value: resourceId}
		]);
	} else {
		var folderId = ui.draggable.is("[data-folderId]") ? ui.draggable.attr("data-folderId") : ui.draggable.attr("data-datakey");
		console.log("Folder " + folderId + " moved to folder " + destFolderId);
		
		moveToFolder([
          {name: 'destFolderId', value: destFolderId},
          {name: 'type', value: 'folder'},
          {name: 'objectId', value: folderId}
		]);
	}
}

function resourceDND() {
	
	if (!$('#datagrid') || $('#datagrid').attr('data-isenablemoving') != 'true') {
		return;
	}
	
	$('#folderGrid .resource_panel').draggable({
        helper: 'clone',
        scope: 'resfolder',
        zIndex: ++PrimeFaces.zindex
     });
	
    $('#resourceGrid .resource_panel').draggable({
       helper: 'clone',
       scope: 'resfolder',
       zIndex: ++PrimeFaces.zindex
    });
    
    $('#folders_tree_wrap .ui-treenode:not([data-datakey="0"])').draggable({
        helper: 'clone',
        scope: 'resfolder',
        zIndex: ++PrimeFaces.zindex
     });

    $('#folderGrid .resource_panel').droppable({
       activeClass: 'ui-state-active',
       hoverClass: 'ui-state-highlight',
       tolerance: 'pointer',
       scope: 'resfolder',
       drop: dropHandle
    });
    
    $('#folders_tree_wrap .ui-treenode .ui-treenode-content').droppable({
        activeClass: 'ui-state-active',
        hoverClass: 'ui-state-highlight',
        tolerance: 'pointer',
        scope: 'resfolder',
        drop: dropHandle
     });
}

$(document).ready(function() 
{
	resourceDND();
	
	//lightbox_resize_container();	
	$(window).resize(lightbox_resize_container);
	
	// register  esc key
	$(document).keydown(function(event) {
		if (event.which == 27)
			lightbox_close();		
	});

});
