var select_res_lightbox = new lightbox_utils();

function confirmSubmitMessage()
{
	var confirmMessage = "Are you sure you want to submit? You will no longer be able to submit any more resources."
	//var no_selected_resources = $('.group-resources-item').length;
	if(resources_to_select > 0)
		confirmMessage = "You should select " + resources_to_select + " more resource" + (resources_to_select > 1 ? "s" : "") +" for your submission. " + confirmMessage;
	return confirm(confirmMessage);
}

function hideAddSign(){
	$('#add_resource_sign').hide();
}

function selectResources(){
	updateSelectedItemsCommand([
	      {name: 'action', value: 'add'},
	      {name: 'items', value: selectedResources.getItemsAsJson()}
	]);
	selectedResources.clear();
	select_res_lightbox.close();
}

function updateMaxResources(){
	var no_selected_resources = $('.group-resources-item').length;
	resources_to_select = max_resources - no_selected_resources;
	if(resources_to_select == 0)
		$('#add_resource_sign').hide();
	if(no_selected_resources > 0)
		$('#submit_resources_button').show();
	
	$('#add_resource_sign').on('click', function(){
		select_res_lightbox.open();
	});
}

function selectResourceDND() {
    var $dataGrid = $('#resourceGrid');

    // disable drag and drop for the activity log
    if($dataGrid.hasClass("not-selectable"))
    	return;
    
    $dataGrid.selectable({
        filter: 'div.group-resources-item2',
        
        selecting: function(event, ui) { 
            if ($(".ui-selected, .ui-selecting").length > resources_to_select) {
              $('.ui-selecting').removeClass("ui-selecting");
            }
        },
        unselecting: function(event, ui) {
        	var elementToRemove = ui.unselecting;
        	selectedResources.removeElement(elementToRemove);
        },
        start: function (e) {
            if (!(e.ctrlKey||e.metaKey)) {
                selectedResources.clear();
            }
        },
        stop: function () {
            selectedResources.add($(".ui-selected"));
        },
        cancel: 'a'
    });
}

$(document).ready(function(){
	selectResourceDND();
	select_res_lightbox.load();
	$(window).resize(select_res_lightbox.resize_container);
	$('#add_resource_sign').on('click', function(){
		select_res_lightbox.open();
	});
	
    // register esc key
    $(document).keydown(function (e) {
        if (e.which == 27)
        	select_res_lightbox.close();
    });
    
    $(document).on('click', '.resource-controls2 a', function (e) {
        var action = (this.className.match(/action-[^\s]+/) || []).pop().replace('action-', '');
        var element = $(this).parents('.group-resources-item')[0];
        e.preventDefault();
        e.stopPropagation();

        
        selected.clearAndAdd(element);
        if(action === "delete")
        	doAction("remove");
    });
   
    updateMaxResources();
});

function newSelectedItems() {
	this.removeElement = function (element) {
		if (element && element.nodeType === 1) {
	        var elementType = element.getAttribute("data-itemType");
	        var elementId = element.getAttribute("data-itemId");

	        if (elementType && elementId) {
	            var index = this.inSelected(elementType, elementId);
	            if (index !== -1) {
	                this.items.splice(index, 1);
	            }
	        }
		}
	};
}

newSelectedItems.prototype = new SelectedItems();
newSelectedItems.prototype.selectItem = function (type, id) {};
newSelectedItems.prototype.clear = function () {
    $(".group-resources-item2.ui-selected").removeClass("ui-selected");
    this.items = [];
};

var selectedResources = new newSelectedItems();

function lightbox_utils()
{
	this.box;
	
	this.load = function() {	
		this.box = $('.submit-resources');
	}

	this.close = function() {
		this.box.hide();
		this.box.detach();
	}

	this.open = function() {
		this.box.appendTo(document.body);
		this.resize_container();
		this.box.show();
	};

	this.resize_container = function() {	
		var height = $(window).height() - 167;		
		$('#lightbox_container').height(height < 200 ? 200 : height);
	}

}