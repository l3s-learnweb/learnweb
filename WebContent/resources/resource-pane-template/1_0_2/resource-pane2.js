/** @external openFolderCommand*/
/** @external selectGroupItemCommand */
/** @external editGroupItemCommand */
/** @external createGroupItemCommand */
/** @external updateGroupItemsCommand */
/** @external updateAddResourcePaneCommand */
/** @external updateThumbnailCommand */


function scrollToElement(element) {
    //$('#right_pane .content').animate({ scrollTop: (element.offset().top + element.height() + 5 - $('#center_pane .content').height())}, 'slow');
    //console.log(element.offset().top, element.position().top, $('#center_pane .content').height(), element.height());
}

function open_timeline_view() {
    var $archive_list_view = $('#archive_list_view');
    if ($archive_list_view.is(':visible')) {
        $archive_list_view.slideToggle("slow");
        $('#list_button').toggleClass('button-active');
    }

    var $timeline_view = $('#timeline_view');
    $timeline_view.slideToggle("slow", function () {
        scroller();
        $('#timeline_button').toggleClass("button-active");
        if ($timeline_view.is(':visible')) {
            var $container = $('#container');
            $container.width($timeline_view.width());
            chart.setSize($timeline_view.width(), $container.height());
            chart.reflow();
            scrollToElement($timeline_view);
        }
    });
    return false;
}

function open_list_view() {
    var $timeline_view = $('#timeline_view');
    if ($timeline_view.is(':visible')) {
        $timeline_view.slideToggle("slow");
        $('#timeline_button').toggleClass('button-active');
    }

    var $archive_list_view = $('#archive_list_view');
    $archive_list_view.slideToggle("slow", function () {
        scroller();
        $('#list_button').toggleClass("button-active");

        if ($archive_list_view.is(':visible')) {
            scrollToElement($archive_list_view);
        }
    });
    return false;
}

var box;
function lightbox_load() {
    box = $('#lightbox');
}
function lightbox_close() {
	change_lightbox_content(false);
    box.hide();
    box.detach();
}
function lightbox_close_for_editor() {
	$('#lightbox').hide();
	$('#lightbox').detach();
}

function lightbox_resize_container() {
    var height = $(window).height() - 167;
    $('#lightbox_container').height(height < 200 ? 200 : height);
}
function lightbox_open() {
    box.appendTo(document.body);
    lightbox_resize_container();
    box.show();
}

function change_lightbox_content(showArchiveUrls){
	if(showArchiveUrls)
	{
		$('#lightbox_content').children().first().hide();
		$('#lightbox_footer').hide();
		
		$('#archive_lightbox_content').show(function(){
			$(this).find("iframe").prop("src", function(){
				return $(this).data("src");
			});
		});
	}
	else
	{
		$('#lightbox_content').children().first().show();
		$('#lightbox_footer').show(); //displays last updated info on viewing thumbnail
		$('#archive_lightbox_content').hide(function(){
			$(this).find("iframe").prop("src","");
		});
	}
}

var prevClickedElement;
function onclick_archive_url(e){
        e.preventDefault();e.stopPropagation();
        
        //reset the background of previously selected archived version
        $(prevClickedElement).parent().css('background-color','#ffffff');
        var clickedElement = e.target || e.srcElement;
        var target_src = $(clickedElement).attr('href');
        
        //setting the timestamp text to selected version
        $('#archive_timestamp').text($(clickedElement).text()); 
        $(clickedElement).parent().css('background-color','#b5ebdc');
        document.getElementById('archive_iframe').src = target_src;
        $('#archive_iframe').data('src', target_src);
        prevClickedElement = clickedElement;
}

function updateThumbnail(){
	var archive_url = $('#archive_iframe').data('src');
	lightbox_close();
	updateThumbnailCommand([{name: 'archive_url', value: archive_url}]);
}

function archiveListInitialize(){
	$('.years ul').hide();
    $('.years').click(function() {
    	$(this).find('span').toggleClass("bold");
        $(this).find('ul').slideToggle();
    });
}

function prepareCommentButton() {
    var button = $('#comment_button');
    button.hide();
    $('#commentfield').focus(function () {
        button.slideDown();
    });
    $('#comment_form').focusout(function () {
        $('#comment_button').slideUp(1000);
    });
}

function selectNewResourceLocation() {
    dialog.confirm('selectDestination', function () {
        updateAddResourcePaneCommand();
    });
}

//To detect if its an initial page load or a reload from the History entry in Safari.
var popped = false, initialURL = location.href;
window.onpopstate = function () {
    var initialPop = !popped && location.href == initialURL;
    popped = true;
    if (initialPop) return;
    location.reload(true);
};

function update_url(resource_id, folder_id, group_id) {
    var page_schema = location.protocol + "//" + location.host + location.pathname;
    var query_params = location.search;

    if (folder_id != undefined) {
        query_params = updateUrlParameters(query_params, "folder_id", folder_id);
    }

    if (resource_id != undefined) {
        query_params = updateUrlParameters(query_params, "resource_id", resource_id);
    }

    if (group_id != undefined) {
        query_params = updateUrlParameters(query_params, "group_id", group_id);
    }

    var updated_url = page_schema + query_params;
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

function resourceDND() {
    var $dataGrid = $('#datagrid');

    // disable drag and drop for the activity log
    if($dataGrid.hasClass("not-selectable"))
    	return;
    
    $dataGrid.selectable({
        filter: 'div.group-resources-item',
        cancel: 'div.group-resources-item',
        start: function (e) {
            if (!(e.ctrlKey||e.metaKey)) {
                selected.clear();
            }
        },
        stop: function () {
            selected.add($(".ui-selected"));
        }
    });

    if (!$dataGrid || $dataGrid.attr('data-isenabledmoving') != 'true') {
        return;
    }

    $dataGrid.find('.group-resources-item').draggable({
        helper: 'clone',
        start: function (e, ui) {
            if (!$(this).hasClass("ui-selected")) {
                selected.clearAndAdd(this);
            }

            selected.forEachElement(function (el) {
                $(el).addClass("ui-draggable-greyscale");
            });

            if (selected.getSize() > 1) {
                $(ui.helper).append("<div class='selected-icon'>" + selected.getSize() + "</div>");
            }

            var newWidth = $('.resource-item').width();
            $(ui.helper).addClass("ui-draggable-helper").width(newWidth + "px");
        },
        stop: function () {
            selected.forEachElement(function (el) {
                $(el).removeClass("ui-draggable-greyscale");
            });
        },
        scope: 'resfolder',
        appendTo: 'body',
        revert: 'invalid',
        cursorAt: {top: 0, left: 0},
        scroll: false,
        zIndex: ++PrimeFaces.zindex
    });

    $('#folderGrid').find('.group-resources-item').droppable({
        activeClass: 'ui-state-active',
        hoverClass: 'ui-state-highlight',
        tolerance: 'pointer',
        scope: 'resfolder',
        drop: function () {
            var destFolderId = $(this).attr("data-itemId");
            doAction('move', null, destFolderId);
        }
    });

    /* Folders in the tree */
    var $foldersTree = $('#folders_tree_wrap');
    $foldersTree.find('.ui-treenode:not([data-datakey="0"])').draggable({
        helper: 'clone',
        start: function (e, ui) {
            selected.clearAndAdd(this);

            selected.forEachElement(function (el) {
                $(el).addClass("ui-draggable-greyscale");
            });

            var newWidth = $('.resource-item').width();
            $(ui.helper).addClass("ui-draggable-helper").width(newWidth + "px");
        },
        stop: function () {
            selected.forEachElement(function (el) {
                $(el).removeClass("ui-draggable-greyscale");
            });
        },
        scope: 'resfolder',
        appendTo: 'body',
        revert: 'invalid',
        cursorAt: {top: 0, left: 0},
        scroll: false,
        zIndex: ++PrimeFaces.zindex
    });

    $foldersTree.find('.ui-treenode .ui-treenode-content').droppable({
        activeClass: 'ui-state-active',
        hoverClass: 'ui-state-highlight',
        tolerance: 'pointer',
        scope: 'resfolder',
        drop: function dropHandle() {
            var destFolderId = $(this).parents(".ui-treenode").attr("data-datakey");
            doAction('move', null, destFolderId);
        }
    });

    /* Breadcrumbs */
    var $breadcrumbs = $('#breadcrumbs');
    $breadcrumbs.find('li').droppable({
        activeClass: 'ui-state-active',
        hoverClass: 'ui-state-highlight',
        tolerance: 'pointer',
        scope: 'resfolder',
        drop: function dropHandle() {
            var destFolderId = $(this).attr("data-folderId");
            doAction('move', null, destFolderId);
        }
    });
}
function check_tag(tagName){
	
	
}

function load_editor() {
	if ($("#right_pane .editor_preview").length) {
		сonnectEditor("iframeEditor", "embedded");
	}

}

function load_lightbox_editor() {
    
	lightbox_open();
	var canBeEdited = ($('#ed_can_be_edited').val() == 'true');

	$('#lightbox_background').click(function() {
		lightbox_close_for_editor();
	});
	$('#lightbox_close').click(function() {
		lightbox_close_for_editor();
	});

	var mode = canBeEdited ? "edit" : "view";
	сonnectEditor("lightbox_editor", mode);
 }

/* Context menu */
function showContextMenu(className, e) {
    $(".resource-context-menu").addClass(className + "-cntx").finish().toggle().css({
        top: e.pageY + "px",
        left: e.pageX + "px"
    });
}

function hideContextMenu() {
    $(".resource-context-menu").hide().removeClass(function (index, css) {
        return (css.match(/\b\S+-cntx/g) || []).join(' ');
    });
}

function openFolder(folderId) {
    update_url(0, folderId);

    openFolderCommand([
        {name: 'itemId', value: folderId}
    ]);
}

function openGroup(folderId) {
	update_url(0, 0, folderId);
	openFolderCommand([
	    {name: 'itemId', value: folderId}
	]);
}

function doAction(action, extraAttr1, extraAttr2) {
    switch (action) {
        case 'create-folder':
            createGroupItemCommand([{name: 'type', value: 'folder'}]);
            break;
        case 'upload-file':
            createGroupItemCommand([{name: 'type', value: 'file'}]);
            break;
        case 'add-website':
            createGroupItemCommand([{name: 'type', value: 'url'}]);
            break;
        case 'add-glossary':
            createGroupItemCommand([{name: 'type', value: 'glossary'}]);
            break;
        case 'open-folder':
            var last = selected.getItem(selected.getSize() - 1);
            if (selected.getSize() > 0 && last.type == "folder") {
                openFolder(last.id);
            }
            else if(selected.getSize() > 0 && last.type == "group") {
            	openGroup(last.id);
            }
            else {
                console.error("No folder selected.");
            }
            break;
        case 'add-tag':
            if (selected.getSize() > 0) {
                dialog.confirm('addTag', function () {
                    var $tagInput = $('#modal_tag_name');
                    var tagName = $tagInput.val().trim();
                    $tagInput.val('');

                    updateGroupItemsCommand([
                        {name: 'action', value: 'add-tag'},
                        {name: 'tag', value: tagName},
                        {name: 'items', value: selected.getItemsAsJson()}
                    ]);
                });
            } else {
                console.error("No resources selected.");
            }
            break;
        case 'copy':
            if (selected.getSize() > 0) {
                dialog.confirm('selectDestination', function () {
                    updateGroupItemsCommand([
                        {name: 'action', value: 'copy'},
                        {name: 'items', value: selected.getItemsAsJson()}
                    ]);
                });
            } else {
                console.error("No resources selected.");
            }
            break;
        case 'move':
            if (extraAttr1 || extraAttr2) {
                updateGroupItemsCommand([
                    {name: 'action', value: 'move'},
                    {name: 'destination', value: JSON.stringify({'groupId': extraAttr1, 'folderId': extraAttr2})},
                    {name: 'items', value: selected.getItemsAsJson()}
                ]);
            } else if (selected.getSize() > 0) {
                dialog.confirm('selectDestination', function () {
                    updateGroupItemsCommand([
                        {name: 'action', value: 'move'},
                        {name: 'items', value: selected.getItemsAsJson()}
                    ]);
                });
            } else {
                console.error("No resources selected.");
            }
            break;
        case 'edit':
            if (selected.getSize() === 1) {
                var item = selected.getItem(0);
                editGroupItemCommand([
                    {name: 'itemType', value: item.type},
                    {name: 'itemId', value: item.id}
                ]);
            } else {
                console.error("No resources selected.");
            }
            break;
        case 'delete':
            if (selected.getSize() > 0) {
                dialog.confirm('deleteConfirm', function () {
                    updateGroupItemsCommand([
                        {name: 'action', value: 'delete'},
                        {name: 'items', value: selected.getItemsAsJson()}
                    ]);
                });
            } else {
                console.error("No resources selected.");
            }
            break;
        default:
            console.log("Unimplemented or unsupported action: ", action);
    }
}

$(document).ready(function () {
    resourceDND();

    lightbox_load();
    load_editor();
    $(window).resize(lightbox_resize_container);

    // register esc key
    $(document).keydown(function (e) {
        if (e.which == 27)
            lightbox_close();
    });

    $(document).on('click', '.group-resources-item', function (e) {
        if (e.shiftKey && selected.getSize() > 0) {
            var previous = selected.getItem(selected.getSize() - 1);
            selected.add(this);
            var current = selected.getItem(selected.getSize() - 1);
            if (previous.id !== current.id) {
                var isFound = false;
                $('#datagrid').find('.group-resources-item').each(function (i, el) {
                    var elId = el.getAttribute("data-itemId");
                    var elType = el.getAttribute("data-itemType");
                    if ((elId == previous.id && elType == previous.type) || (elId == current.id && elType == current.type)) {
                        isFound = !isFound;
                        if (!isFound) return false;
                    } else if (isFound) {
                        selected.add(el);
                    }
                });
            }
        } else if (e.ctrlKey || e.metaKey) {
            selected.add(this);
        } else {
            selected.clearAndAdd(this)
        }

        selected.selectLastItem();
    });
    
        //for resource_yell.html
    $(document).on('click', '.group-resources2-item', function (e) {
        if (e.shiftKey && selected.getSize() > 0) {
            var previous = selected.getItem(selected.getSize() - 1);
            selected.add(this);
            var current = selected.getItem(selected.getSize() - 1);
            if (previous.id !== current.id) {
                var isFound = false;
                $('#rdetail').find('.group-resources2-item').each(function (i, el) {
                    var elId = el.getAttribute("data-itemId");
                    var elType = el.getAttribute("data-itemType");
                    if ((elId == previous.id && elType == previous.type) || (elId == current.id && elType == current.type)) {
                        isFound = !isFound;
                        if (!isFound) return false;
                    } else if (isFound) {
                        selected.add(el);
                    }
                });
            }
        } else if (e.ctrlKey || e.metaKey) {
            selected.add(this);
        } else {
            selected.clearAndAdd(this)
        }

        selected.selectLastItem();
    });

    $(document).on('click', '.resource-controls a', function (e) {
        var action = (this.className.match(/action-[^\s]+/) || []).pop().replace('action-', '');
        var element = $(this).parents('.group-resources-item')[0];
        e.preventDefault();
        e.stopPropagation();

        selected.clearAndAdd(element);
        doAction(action);
    });

    $(document).on('dblclick', '.group-resources-item.folder-item', function () {
        var folderId = $(this).attr("data-itemId");
        var folderType = $(this).attr("data-itemType");
        if(folderId && folderType == "folder") {
            openFolder(folderId);
        }
        else if(folderId && folderType == "group") {
        	openGroup(folderId);
        }
    });

    $(document).on("contextmenu", ".datagrid", function (e) {
        e.preventDefault();
        e.stopPropagation();
        showContextMenu("datagrid", e);
    });

    $(document).on("contextmenu", ".group-resources-item", function (e) {
        e.preventDefault();
        e.stopPropagation();

        var resource = $(e.target).parents(".group-resources-item")[0];
        var id = resource.getAttribute("data-itemId"),
            type = resource.getAttribute("data-itemType");

        if (!selected.selectIfExists(type, id)) {
            selected.clearAndAdd(resource);
            selected.selectLastItem();
        }

        if (selected.getSize() == 1) {
            showContextMenu("single-" + type, e);
        } else if (selected.getSelectedType() == "resources") {
            showContextMenu("resources", e);
        } else {
            alert("Unfortunately, we don't support batch operation with folders.");
        }
    });

    $(document).on("mousedown", function (e) {
        if (!$(e.target).parents(".resource-context-menu").length) {
            hideContextMenu();
        }
    });

    $(document).on("click", ".resource-context-menu > .context-menu-item", function () {
        var action = (this.className.match(/action-[^\s]+/) || []).pop().replace('action-', '');
        doAction(action);
        hideContextMenu();
    });
    
    archiveListInitialize();
});

function ConfirmDialog() {
    this.confirm = function (dialogId, successCallback) {
        PF(dialogId).show();

        var className = (selected.getSize() > 1 ? 'plural' : 'single') + ' ' + selected.getSelectedType();
        $('#' + dialogId + 'Dialog').addClass(className).on('hide', function () {
            $(this).removeClass(className).off();
        }).on('click', '.confirm', function (e) {
            e.preventDefault();
            e.stopPropagation();

            PF(dialogId).hide();
            if (successCallback) successCallback();
            $(this).off();
        });
    };
}
var dialog = new ConfirmDialog();

function SelectedItems() {
    this.items = [];

    this.addElement = function (element) {
        if (element && element.nodeType === 1) {
            var elementType = element.getAttribute("data-itemType");
            var elementId = element.getAttribute("data-itemId");

            if (elementType && elementId) {
                var index = this.inSelected(elementType, elementId);
                if (index === -1) {
                    element.className += " ui-selected";
                } else {
                    this.items.splice(index, 1);
                }

                this.items.push({
                    id: elementId,
                    type: elementType,
                    element: element
                });
            } else if (element.getAttribute("data-nodetype") == "folder") {
                this.items.push({
                    id: element.getAttribute("data-datakey"),
                    type: 'folder',
                    element: element
                });
            } else {
                console.error("Broken data.", element);
            }
        } else {
            console.error("Wrong type.", element);
        }
    };

    this.add = function (element) {
        if (element.length > 0) {
            for (var i = 0, l = element.length; i < l; ++i) {
                if (!element[i].classList.contains('ui-draggable-helper')) {
                    this.addElement(element[i]);
                }
            }
        } else if (element.length !== 0) {
            this.addElement(element);
        }
    };

    this.clear = function () {
        $(".group-resources-item.ui-selected").removeClass("ui-selected");

        this.items = [];
    };

    this.clearAndAdd = function (element) {
        this.clear();
        this.add(element);
    };

    this.getSize = function () {
        return this.items.length;
    };

    this.getItem = function (index) {
        return this.items[index];
    };

    this.getItemsAsJson = function () {
        var exportItems = [];
        for (var l = this.items.length, i = 0; i < l; ++i) {
            exportItems.push({
                itemType: this.items[i].type,
                itemId: this.items[i].id
            })
        }
        return JSON.stringify(exportItems);
    };

    this.inSelected = function (itemType, itemId) {
        for (var l = this.items.length, i = 0; i < l; ++i) {
            if (this.items[i].type == itemType && this.items[i].id == itemId) {
                return i;
            }
        }

        return -1;
    };

    this.selectItem = function (type, id) {
        selectGroupItemCommand([
            {name: 'itemType', value: type},
            {name: 'itemId', value: id}
        ]);

        if (type == "folder") {
            //update_url(0, id);
        } else if(type == "resource"){
            update_url(id);
        }
    };

    this.selectItemByIndex = function (index) {
        var item = this.items[index];
        this.selectItem(item.type, item.id);
    };

    this.selectLastItem = function () {
        if (this.items.length) {
            this.selectItemByIndex(this.items.length - 1);
        } else {
            console.error("Items is empty on selectLastItem call");
        }
    };

    this.selectIfExists = function (type, id) {
        for (var l = this.items.length, i = 0; i < l; ++i) {
            if (this.items[i].type == type && this.items[i].id == id) {
                this.selectItemByIndex(i);
                return true;
            }
        }

        return false;
    };

    this.forEach = function (func) {
        for (var l = this.items.length, i = 0; i < l; ++i) {
            func(this.items[i], i, this.items)
        }
    };

    this.forEachElement = function (func) {
        for (var l = this.items.length, i = 0; i < l; ++i) {
            func(this.items[i].element, i, this.items);
        }
    };

    this.getSelectedType = function () {
        var isFolders = false, isResources = false;
        for (var l = this.items.length, i = 0; i < l; ++i) {
            if (this.items[i].type == 'resource') {
                if (isFolders) {
                    return 'mixed';
                } else if (!isResources) {
                    isResources = true;
                }
            } else if (this.items[i].type == 'folder') {
                if (isResources) {
                    return 'mixed';
                } else if (!isFolders) {
                    isFolders = true;
                }
            }
        }

        return isFolders ? 'folders' : 'resources';
    };

    return this;
}
var selected = new SelectedItems();

(function ($) {
    $.each(['show', 'hide'], function (i, ev) {
        var el = $.fn[ev];
        $.fn[ev] = function () {
            this.trigger(ev);
            return el.apply(this, arguments);
        };
    });
})(jQuery);