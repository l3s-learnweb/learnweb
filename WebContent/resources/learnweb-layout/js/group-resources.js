/** @external openFolderCommand*/
/** @external selectGroupItemCommand */
/** @external editGroupItemCommand */
/** @external createGroupItemCommand */
/** @external updateGroupItemsCommand */
/** @external updateAddResourcePaneCommand */

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

    if (folder_id !== undefined) {
        query_params = updateUrlParameters(query_params, "folder_id", folder_id);
    }

    if (resource_id !== undefined) {
        query_params = updateUrlParameters(query_params, "resource_id", resource_id);
    }

    if (group_id !== undefined) {
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
    if ($dataGrid.hasClass("not-selectable"))
        return;

    $dataGrid.selectable({
        filter: 'div.group-resources-item',
        cancel: 'div.group-resources-item',
        start: function (e) {
            if (!(e.ctrlKey || e.metaKey)) {
                selected.clear();
            }
        },
        stop: function () {
            selected.add($(".ui-selected"));
        },
        cancel: "input,textarea,button,select,option,.cancel"
    });

    if (!$dataGrid || !$dataGrid.closest('#resourcesView').attr('data-canMoveResources')) {
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

            var newWidth = $('.res-grid-item').width();
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

            var newWidth = $('.res-grid-item').width();
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

/* Context menu */
/**
 * @param {{canAddResources?: boolean,
 *          canViewResource?: boolean,
 *          canEditResource?: boolean,
 *          canDeleteResource?: boolean,
 *          canAnnotateResource?: boolean}} options
 * @param type
 * @param e
 */
function showContextMenu(options, type, e) {
    var $contextMenu = $("#context-menu");

    $contextMenu.children().each(function () {
        var action = $(this).attr('data-action');
        var permission = $(this).attr('data-per');
        if (action === "open-folder") {
            $(this).toggle(type === "folder" && options[permission] || false);
        } else {
            $(this).toggle(options[permission] || false);
        }
    });

    $contextMenu.finish().show().css({
        top: e.pageY + "px",
        left: e.pageX + "px"
    });
}

function hideContextMenu() {
    var $contextMenu = $(".resource-context-menu");
    $contextMenu.hide();
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
        case 'new-file':
            createGroupItemCommand([{name: 'type', value: 'newFile'}, {name: 'docType', value: extraAttr1}]);
            break;
        case 'create-folder':
            createGroupItemCommand([{name: 'type', value: 'folder'}]);
            break;
        case 'upload-file':
            createGroupItemCommand([{name: 'type', value: 'file'}]);
            break;
        case 'add-website':
            createGroupItemCommand([{name: 'type', value: 'url'}]);
            break;
        case 'add-glossary2':
            createGroupItemCommand([{name: 'type', value: 'glossary2'}]);
            break;
        case 'add-survey':
            createGroupItemCommand([{name: 'type', value: 'survey'}]);
            break;
        case 'open-folder':
            var last = selected.getItem(selected.getSize() - 1);
            if (selected.getSize() > 0 && last.type === "folder") {
                openFolder(last.id);
            } else if (selected.getSize() > 0 && last.type === "group") {
                openGroup(last.id);
            } else {
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
                    update_url(0);
                });
            } else {
                console.error("No resources selected.");
            }
            break;
        case 'remove':
            updateSelectedItemsCommand([
                {name: 'action', value: 'remove'},
                {name: 'items', value: selected.getItemsAsJson()}
            ]);
            update_url(0);
            break;
        default:
            console.log("Unimplemented or unsupported action: ", action);
    }
}

$(document).ready(function () {
    resourceDND();

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
                    if ((elId === previous.id && elType === previous.type) || (elId === current.id && elType === current.type)) {
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
                $('.group-resources2-item').each(function (i, el) {
                    var elId = el.getAttribute("data-itemId");
                    var elType = el.getAttribute("data-itemType");
                    if ((elId === previous.id && elType === previous.type) || (elId === current.id && elType === current.type)) {
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

    //for resources_list_view.xhtml only
    $(document).on('click', '.resource-controls2 a', function (e) {
        var action = (this.className.match(/action-[^\s]+/) || []).pop().replace('action-', '');
        var element = $(this).parents('.group-resources2-item')[0];
        e.preventDefault();
        e.stopPropagation();

        selected.clearAndAdd(element);
        doAction(action);
    });

    $(document).on('dblclick', '.group-resources-item[data-itemtype="folder"]', function () {
        var folderId = $(this).attr("data-itemId");
        var folderType = $(this).attr("data-itemType");
        if (folderId && folderType === "folder") {
            openFolder(folderId);
        } else if (folderId && folderType === "group") {
            openGroup(folderId);
        }
    });

    $(document).on("contextmenu", ".datagrid", function (e) {
        e.preventDefault();
        e.stopPropagation();

        var $resourcesView = $(this).closest('#resourcesView');
        showContextMenu({
            canAddResources: $resourcesView.attr('data-canAddResources') === "true"
        }, "datagrid", e);
    });

    $(document).on("contextmenu", ".group-resources-item", function (e) {
        e.preventDefault();
        e.stopPropagation();

        var $resource = $(this);
        var id = $resource.attr("data-itemId"),
            type = $resource.attr("data-itemType");

        if (!selected.selectIfExists(type, id)) {
            selected.clearAndAdd($resource);
            selected.selectLastItem();
        }

        if (selected.getSize() === 1) {
            var item = selected.getItem(0);

            showContextMenu({
                canViewResource: item.element.getAttribute('data-canViewResource') === "true",
                canEditResource: item.element.getAttribute('data-canEditResource') === "true",
                canDeleteResource: item.element.getAttribute('data-canDeleteResource') === "true",
                canAnnotateResource: item.element.getAttribute('data-canAnnotateResource') === "true"
            }, item.type, e);
        } else if (selected.getSelectedType() === "resources") {
            var canEditResource = true, canDeleteResource = true, canAnnotateResource = true;

            selected.forEachElement(function (element) {
                if (element.getAttribute("data-canEditResource") !== "true") {
                    canEditResource = false;
                }
                if (element.getAttribute("data-canDeleteResource") !== "true") {
                    canDeleteResource = false;
                }
                if (element.getAttribute("data-canAnnotateResource") !== "true") {
                    canAnnotateResource = false;
                }
            });

            showContextMenu({
                canViewResource: true,
                canEditResource: canEditResource,
                canDeleteResource: canDeleteResource,
                canAnnotateResource: canAnnotateResource
            }, "resources", e);
        } else {
            alert("Unfortunately, we don't support batch operation with folders.");
        }
    });

    $(document).on("mousedown", function (e) {
        if (!$(e.target).parents(".resource-context-menu").length) {
            hideContextMenu();
        }
    });

    $(document).on("update", ".nano", function () {
        hideContextMenu();
    });

    $(document).on("click", ".resource-context-menu > .context-menu-item", function () {
        var action = $(this).attr('data-action');
        doAction(action);
        hideContextMenu();
    });
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
            } else if (element.getAttribute("data-nodetype") === "folder") {
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

        //for resources_yell only
        $(".group-resources2-item.ui-selected").removeClass("ui-selected");

        this.items = [];
    };

    this.clearAndAdd = function (element) {
        this.clear();
        this.add(element);
    };

    this.getSize = function () {
        return this.items.length;
    };

    /**
     * @param index
     * @returns {{id: string, type: string, element: element }}
     */
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
            if (this.items[i].type === itemType && this.items[i].id === itemId) {
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

        if (type === "folder") {
            //update_url(0, id);
        } else if (type === "resource") {
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
            if (this.items[i].type === type && this.items[i].id === id) {
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
            if (this.items[i].type === 'resource') {
                if (isFolders) {
                    return 'mixed';
                } else if (!isResources) {
                    isResources = true;
                }
            } else if (this.items[i].type === 'folder') {
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