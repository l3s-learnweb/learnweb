var selected = $([]); 

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

function update_url(resource_id, folder_id, group_id)
{	
	var page_schema = location.protocol + "//" + location.host + location.pathname;
	var query_params = location.search;
	
	if (folder_id != undefined) {
		query_params = updateUrlParameters(query_params, "folder_id", folder_id);
	}
	
	if (resource_id != undefined) {
		query_params = updateUrlParameters(query_params, "resource_id", resource_id);
	}
	
	if(group_id != undefined) {
		query_params = updateUrlParameters(query_params, "group_id", group_id);
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
	var destFolderId = null, objectsToMove = $([]);
	
	// Process destination
	if ($(this).is("[data-type=folder]")) {
		destFolderId = $(this).attr("data-resourceId");
	} else {
		destFolderId = $(this).parents(".ui-treenode").attr("data-datakey");
	}
	
	// Process elements
	if (ui.draggable.hasClass("ui-selected")){
		//console.log("has-selected", selected);
		
		var i = 1;
		selected.not('.ui-draggable-helper').each(function() {
			var type = $(this).attr("data-type");
			var resourceId = $(this).attr("data-resourceId");
			objectsToMove.push({ type: type, resourceId: resourceId });
       });
		
		console.log("Resources and/or folders ", objectsToMove, " will move to folder " + destFolderId);
	} else if (ui.draggable.is("[data-type=resource]")) {
		var resourceId = ui.draggable.attr("data-resourceId");
		
		console.log("Resource " + resourceId + " will move to folder " + destFolderId);
		objectsToMove.push({ type: "resource", resourceId: resourceId });
	} else {
		var resourceId = ui.draggable.is("[data-type=folder]") ? ui.draggable.attr("data-resourceId") : ui.draggable.attr("data-datakey");
		
		console.log("Folder " + resourceId + " will move to folder " + destFolderId);
		objectsToMove.push({ type: "folder", resourceId: resourceId });
	}
		
	moveToFolder([
	  {name: 'destFolderId', value: destFolderId},
	  {name: 'objectsToMove', value: JSON.stringify(objectsToMove)}
	]);
}

function resourceDND() {
	if (!$('#datagrid') || $('#datagrid').attr('data-isenablemoving') != 'true') {
		return;
	}
	
	$('#datagrid').selectable({
	    filter: 'div.group-resources-item',
	});
	
	/* Resources inside datagrid */
    $('#datagrid .group-resources-item').draggable({
        helper: 'clone',
        start: function(e, ui) {
     	  if ($(this).hasClass("ui-selected")){
              selected = $(".ui-selected").each(function() {
            	  $(this).addClass("ui-draggable-grayscale");
              });
              $(ui.helper).append("<div class='selected-icon'>" + (selected.length - 1) + "</div>");
          } else {
              selected = $([]);
              $(this).addClass("ui-draggable-grayscale");
              $("#datagrid div.group-resources-item").removeClass("ui-selected");
          }
     	  
          var newWidth = $('.resource-item').width();
      	  $(ui.helper).addClass("ui-draggable-helper").width(newWidth + "px");
        },
        stop: function(e) {
        	if ($(this).hasClass("ui-selected")){
                selected = $(".ui-selected").each(function() {
                   $(this).removeClass("ui-draggable-grayscale");
                });
            } else {
                $(this).removeClass("ui-draggable-grayscale");
            }
        },
        scope: 'resfolder',
        appendTo: 'body',
        revert: 'invalid',
        cursorAt: { top: 0, left: 0 },
        scroll: false,
        zIndex: ++PrimeFaces.zindex
     });
	
	$('#folderGrid .group-resources-item').droppable({
       activeClass: 'ui-state-active',
       hoverClass: 'ui-state-highlight',
       tolerance: 'pointer',
       scope: 'resfolder',
       drop: dropHandle
    });
    
    /* Folders in the tree */
    $('#folders_tree_wrap .ui-treenode:not([data-datakey="0"])').draggable({
        helper: 'clone',
        start: function(e, ui) {
     	   $(this).addClass("ui-draggable-grayscale");
     	   $(ui.helper).addClass("ui-draggable-helper");
        },
        stop: function(e) {
     	   $(this).removeClass("ui-draggable-grayscale");
        },
        scope: 'resfolder',
        appendTo: 'body',
        revert: 'invalid',
        cursorAt: { top: 0, left: 0 },
        scroll: false,
        zIndex: ++PrimeFaces.zindex
     });
    
    $('#folders_tree_wrap .ui-treenode .ui-treenode-content').droppable({
        activeClass: 'ui-state-active',
        hoverClass: 'ui-state-highlight',
        tolerance: 'pointer',
        scope: 'resfolder',
        drop: dropHandle
     });
}

function onSelectResourceCompleted(data) {
	update_header();
	
	var resource_id = extractUrlParameters(data, "resourceId");
	var type = extractUrlParameters(data, "type");
	
	if (type == "folder") {
		//update_url(0, resource_id);
	} else {
		update_url(resource_id);
	}
	
	$(".resource-item.selected").removeClass("selected");
	$(".resource-item[data-resourceId=" + resource_id + "]").addClass("selected");
	
	resourceDND();
}

function onSelectFolderCompleted(data) {
	update_header();
	
	var resource_id = extractUrlParameters(data, "resourceId");
	update_url(0, resource_id);
	
	resourceDND();
}

function extractUrlParameters(data, key) {
	if (!data) {
		return data;
	}
	
	var reg = new RegExp("&" + key + "=([^&]*)");
	return decodeURIComponent(data.match(reg)[1]);
}

$(document).ready(function() 
{
	resourceDND();	

	lightbox_load();
	
	$(window).resize(lightbox_resize_container);
	
	// register  esc key
	$(document).keydown(function(event) {
		if (event.which == 27)
			lightbox_close();		
	});
	
	$(document).on('click', '.group-resources-item', function(e) {
		var type = $(this).attr("data-type");
		var resourceId = $(this).attr("data-resourceId");
		
		if ($(e.target).parents('.resource-controls-button').length > 0) {
			console.log("Clicked on control button.");
		} else if (type !== null && resourceId !== null) {
			selectResource([
                {name: 'type', value: type},
                {name: 'resourceId', value: resourceId}
    		]);
		} else {
			console.error("Wrong resource-item attributes!");
		}		
	});	
	
	$(document).on('dblclick', '.group-resources-item.folder-item', function(e) {
		var resourceId = $(this).attr("data-resourceId");
		
		selectFolder([
	        {name: 'resourceId', value: resourceId}
		]);
	});
});
