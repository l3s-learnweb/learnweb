/** @external selectGroupItemCommand */
/** @external editGroupItemCommand */
/** @external createGroupItemCommand */
/** @external openFolderCommand */
/** @external updateSelectedItemsCommand */

/** @external updateGroupItemsCommand */
/** @external updateAddResourcePaneCommand */


function SelectResource() {
    this.items = [];
}

SelectResource.prototype.select = function(el) {
    if (el.length > 0) {
        for (var i = 0, l = el.length; i < l; ++i) {
            if (!el[i].classList.contains('ui-draggable-helper')) {
                this._selectElement(el[i]);
            }
        }
    } else if (el.length !== 0) {
        this._selectElement(el);
    }
};

SelectResource.prototype._selectElement = function(el) {
    var itemType = el.dataset.itemtype;
    var itemId = el.dataset.itemid;

    if (itemType && itemId) {
        var index = this.indexOf(itemType, itemId);
        if (index === -1) {
            el.classList.add('ui-selected');
            this.items.push({
                id: Number(itemId),
                type: itemType,
                element: el
            });
        }
    } else {
        console.error('Element type or ID is unknown', el);
    }
};

SelectResource.prototype.unselect = function(el) {
    var itemType = el.dataset.itemtype;
    var itemId = el.dataset.itemid;

    if (itemType && itemId) {
        var index = this.indexOf(itemType, itemId);
        if (index !== -1) {
            this.items[index].element.classList.remove('ui-selected');
            this.items.splice(index, 1);
        }
    }
};

SelectResource.prototype.unselectAll = function() {
    $('.res-item.ui-selected').removeClass('ui-selected');
    $('.res-item.ui-draggable-dragging').removeClass('ui-draggable-dragging');
    this.items = [];
};

SelectResource.prototype.selectOnly = function(element) {
    this.unselectAll();
    this.select(element);
};

/**
 * @param {number} index
 * @returns {{id: number, type: string, element: element }}
 */
SelectResource.prototype.getItem = function(index) {
    return this.items[index];
};

SelectResource.prototype.size = function() {
    return this.items.length;
};

SelectResource.prototype.indexOf = function(itemType, itemId) {
    for (var i = 0, l = this.items.length; i < l; ++i) {
        if (this.items[i].type === itemType && this.items[i].id === Number(itemId)) {
            return i;
        }
    }
    return -1;
};

SelectResource.prototype.forEach = function(func) {
    for (var l = this.items.length, i = 0; i < l; ++i) {
        func(this.items[i], i, this.items)
    }
};

SelectResource.prototype.toJSON = function() {
    var exportItems = [];
    for (var l = this.items.length, i = 0; i < l; ++i) {
        exportItems.push({
            itemType: this.items[i].type,
            itemId: this.items[i].id
        })
    }
    return exportItems;
};

SelectResource.prototype.getSelectedType = function() {
    var isContainFolders = false, isContainResources = false;
    for (var i = 0, l = this.items.length; i < l; ++i) {
        if (this.items[i].type === 'resource') {
            isContainResources = true;
            if (isContainFolders) return 'mixed';
        } else if (this.items[i].type === 'folder') {
            isContainFolders = true;
            if (isContainResources) return 'mixed';
        }
    }

    return (isContainFolders ? 'folder' : 'resource') + (this.items.length > 1 ? 's' : '');
};

/** @type {SelectResource} */
var selected = new SelectResource();


/**
 * Context Menu for resources
 * Do not require to re-create when resources updated
 */
function createContextMenu() {
    var $contextmenuItems = $(document.getElementById('contextmenu_items'));
    // check if container is selectable
    if (!$contextmenuItems)
        return;

    $.contextMenu({
        selector: '.res-container,.res-item',
        // trigger: 'none',
        build: function ($trigger) {
            var triggerType;
            if (!$trigger.hasClass('res-item')) {
                triggerType = 'container';
                selected.unselectAll(); // clicked on a container, unselect all items
            } else {
                // if clicked by an item which is not selected, then reset selection and select only the item
                if (selected.indexOf($trigger.data('itemtype'), $trigger.data('itemid')) === -1) {
                    selected.selectOnly($trigger);
                }
                triggerType = selected.getSelectedType();
            }

            var items = {};
            $('li', $contextmenuItems).each(function (i, el) {
                var itemMenuTypes = el.dataset.type.split('|');
                if (itemMenuTypes.includes(triggerType)) {
                    if ($trigger.data(el.dataset.per) === true) {
                        var name = el.textContent;

                        items[name.toLowerCase()] = {
                            name: name,
                            icon: el.dataset.icon,
                            action: el.dataset.action
                        };
                    }
                }
            });

            return {
                callback: function (itemKey, opt) {
                    var item = opt.items[itemKey];
                    doAction(item.action);
                },
                items: items
            };
        }
    });
}

function createSelectable(resContainerId) {
    var $resContainer = $(document.getElementById(resContainerId));
    // check if container is selectable
    if (!$resContainer || !$resContainer.data('canselectresources'))
        return;

    $resContainer.on('click', '.res-item', function (e) { // select using keyboard hot keys
        e.preventDefault();

        if (e.shiftKey && selected.size() > 0) { // select all between
            var prevSelected = selected.getItem(selected.size() - 1);
            selected.select(this);
            var lastSelected = selected.getItem(selected.size() - 1);
            if (prevSelected.id !== lastSelected.id) {
                var selectAll = false;
                $('.res-item').each(function (i, el) {
                    var itemType = el.dataset.itemtype;
                    var itemId = Number(el.dataset.itemid);

                    if ((itemId === prevSelected.id && itemType === prevSelected.type) || (itemId === lastSelected.id && itemType === lastSelected.type)) {
                        if (selectAll) return false;
                        else selectAll = true;
                    } else if (selectAll) {
                        selected.select(el);
                    }
                });
            }
        } else if (e.ctrlKey || e.metaKey) { // add to selected or remove if already selected
            var itemType = this.dataset.itemtype;
            var itemId = this.dataset.itemid;
            if (selected.indexOf(itemType, itemId) !== -1) {
                selected.unselect(this);
            } else {
                selected.select(this);
            }
        } else {
            selected.selectOnly(this)
        }
    });

    $resContainer.selectable({
        filter: '.res-item',
        cancel: '.res-item',
        start: function (e) {
            // noinspection JSUnresolvedVariable
            if (!(e.ctrlKey || e.metaKey)) {
                selected.unselectAll();
            }
        },
        selecting: function (event, ui) {
            selected.select(ui.selecting);
        },
        unselecting: function (event, ui) {
            selected.unselect(ui.unselecting);
        },
        cancel: 'input,textarea,button,select,option,.cancel,.res-item'
    });
}

/**
 * Drag & Drop for resources
 * Require to re-create when resources updated (re-bind events)
 */
function createDragAndDrop(resContainerId, resBreadcrumbsId, foldersTreeId) {
    var $resContainer = $(document.getElementById(resContainerId));

    // check if abe to move resources here
    if (!$resContainer || !$resContainer.data('canmoveresources'))
        return;

    $('.res-item', $resContainer).draggable({
        addClasses: false,
        helper: 'clone',
        start: function (e, ui) {
            if (!this.classList.contains('ui-selected')) {
                selected.selectOnly(this);
            }

            selected.forEach(function (item) {
                item.element.classList.add('ui-draggable-dragging');
            });

            ui.helper[0].classList.add('ui-draggable-helper');
            ui.helper[0].style.width = selected.getItem(0).element.offsetWidth + 'px';

            if (selected.size() > 1) {
                ui.helper[0].classList.add('has-badge');
                ui.helper[0].setAttribute('data-count', selected.size());
            }
        },
        stop: function () {
            selected.forEach(function (item) {
                item.element.classList.remove('ui-draggable-dragging');
            });
        },
        scope: 'resources',
        appendTo: 'body',
        revert: 'invalid',
        cursorAt: {top: 0, left: 0},
        scroll: false,
        zIndex: ++PrimeFaces.zindex
    });

    $('.res-item[data-itemtype="folder"]', $resContainer).droppable({
        tolerance: 'pointer',
        scope: 'resources',
        drop: function () {
            var destFolderId = this.dataset.itemid;
            doAction('move', null, destFolderId);
        }
    });

    var $resBreadcrumbs = $(document.getElementById(resBreadcrumbsId));
    if ($resBreadcrumbs) {
        $('li', $resBreadcrumbs).droppable({
            tolerance: 'pointer',
            scope: 'resources',
            drop: function () {
                var destFolderId = this.dataset.folderid;
                doAction('move', null, destFolderId);
            }
        });
    }

    var $foldersTree = $(document.getElementById(foldersTreeId));
    if ($foldersTree) {
        $('.ui-treenode', $foldersTree).draggable({
            addClasses: false,
            helper: 'clone',
            start: function (e, ui) {
                selected.selectOnly(this);

                selected.forEach(function (item) {
                    item.element.classList.add('ui-draggable-dragging');
                });

                ui.helper[0].classList.add('ui-draggable-helper');
                ui.helper[0].style.width = selected.getItem(0).element.offsetWidth + 'px';
            },
            stop: function () {
                selected.forEach(function (item) {
                    item.element.classList.remove('ui-draggable-dragging');
                });
            },
            scope: 'resources',
            appendTo: 'body',
            revert: 'invalid',
            cursorAt: {top: 0, left: 0},
            scroll: false,
            zIndex: ++PrimeFaces.zindex
        }).droppable({
            tolerance: 'pointer',
            scope: 'resources',
            drop: function () {
                var destFolderId = this.dataset.datakey;
                doAction('move', null, destFolderId);
            }
        });
    }
}

function openResource(resourceId) {
    PF('learnweb').updateSearchParams({'resource_id': resourceId});

    selectGroupItemCommand([
        {name: 'itemType', value: 'resource'},
        {name: 'itemId', value: resourceId}
    ]);
}

function openFolder(folderId) {
    PF('learnweb').updateSearchParams({'folder_id': folderId, 'resource_id': null});

    openFolderCommand([
        {name: 'itemId', value: folderId}
    ]);
}

function openGroup(groupId) {
    PF('learnweb').updateSearchParams({'group_id': groupId}, true);

    openFolderCommand([
        {name: 'itemId', value: groupId}
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
            var last = selected.getItem(selected.size() - 1);
            if (selected.size() > 0 && last.type === 'folder') {
                openFolder(last.id);
            } else if (selected.size() > 0 && last.type === 'group') {
                openGroup(last.id);
            } else {
                console.error('No folder selected.');
            }
            break;
        case 'add-tag':
            if (selected.size() > 0) {
                createConfirmDialog('addTag', function () {
                    var $tagInput = document.getElementById('modal_tag_name');
                    var tagName = $tagInput.value.trim();
                    $tagInput.value = '';

                    updateGroupItemsCommand([
                        {name: 'action', value: 'add-tag'},
                        {name: 'tag', value: tagName},
                        {name: 'items', value: JSON.stringify(selected)}
                    ]);
                });
            } else {
                console.error('No resources selected.');
            }
            break;
        case 'copy':
            if (selected.size() > 0) {
                createConfirmDialog('selectDestination', function () {
                    updateGroupItemsCommand([
                        {name: 'action', value: 'copy'},
                        {name: 'items', value: JSON.stringify(selected)}
                    ]);
                });
            } else {
                console.error('No resources selected.');
            }
            break;
        case 'move':
            if (extraAttr1 || extraAttr2) {
                updateGroupItemsCommand([
                    {name: 'action', value: 'move'},
                    {name: 'destination', value: JSON.stringify({'groupId': extraAttr1, 'folderId': extraAttr2})},
                    {name: 'items', value: JSON.stringify(selected)}
                ]);
            } else if (selected.size() > 0) {
                createConfirmDialog('selectDestination', function () {
                    updateGroupItemsCommand([
                        {name: 'action', value: 'move'},
                        {name: 'items', value: JSON.stringify(selected)}
                    ]);
                });
            } else {
                console.error('No resources selected.');
            }
            break;
        case 'edit':
            if (selected.size() === 1) {
                var item = selected.getItem(0);
                editGroupItemCommand([
                    {name: 'itemType', value: item.type},
                    {name: 'itemId', value: item.id}
                ]);
            } else {
                console.error('No resources selected.');
            }
            break;
        case 'delete':
            if (selected.size() > 0) {
                createConfirmDialog('deleteConfirm', function () {
                    updateGroupItemsCommand([
                        {name: 'action', value: 'delete'},
                        {name: 'items', value: JSON.stringify(selected)}
                    ]);
                    PF('learnweb').updateSearchParams({'resource_id': null});
                });
            } else {
                console.error('No resources selected.');
            }
            break;
        case 'remove':
            updateSelectedItemsCommand([
                {name: 'action', value: 'remove'},
                {name: 'items', value: JSON.stringify(selected)}
            ]);
            PF('learnweb').updateSearchParams({'resource_id': null});
            break;
        default:
            console.log('Unimplemented or unsupported action: ', action);
    }
}

function openItems() {
    var itemId = this.dataset.itemid;
    var itemType = this.dataset.itemtype;

    if (itemId && itemType === 'resource') {
        openResource(itemId);
    } else if (itemId && itemType === 'folder') {
        openFolder(itemId);
    } else if (itemId && itemType === 'group') {
        openGroup(itemId);
    }
}

function createConfirmDialog(dialogId, successCallback) {
    PF(dialogId).show();

    var $dialog = $('#' + dialogId + 'Dialog');
    $dialog.find('.collapse').hide();
    $dialog.find('.type-' + selected.getSelectedType()).show();

    $dialog.on('click', '.confirm', function (e) {
        e.preventDefault();
        e.stopPropagation();

        PF(dialogId).hide();
        if (successCallback) successCallback();
        $(this).off();
    });
}
