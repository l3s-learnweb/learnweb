/* global commandEditFolder, commandBatchUpdateResources, commandOpenFolder, openResourceView */

class SelectResource {
  constructor() {
    this.items = [];
  }

  select(el) {
    if (el.length > 0) {
      for (let i = 0, l = el.length; i < l; ++i) {
        if (!el[i].classList.contains('ui-draggable-helper')) {
          this._selectElement(el[i]);
        }
      }
    } else if (el.length !== 0) {
      this._selectElement(el);
    }
  }

  _addContainerClass() {
    if (this.items.length > 0) {
      $('.res-container').addClass('res-highlight-select');
    } else {
      $('.res-container').removeClass('res-highlight-select');
    }
  }

  _selectElement(el) {
    const itemType = el.dataset.itemtype;
    const itemId = el.dataset.itemid;

    if (itemType && itemId) {
      const index = this.indexOf(itemType, itemId);
      if (index === -1) {
        el.classList.add('ui-selected');
        this.items.push({
          id: Number(itemId),
          type: itemType,
          element: el,
        });
        this._addContainerClass();
      }
    } else {
      console.error('Element type or ID is unknown', el);
    }
  }

  unselect(el) {
    const itemType = el.dataset.itemtype;
    const itemId = el.dataset.itemid;

    if (itemType && itemId) {
      const index = this.indexOf(itemType, itemId);
      if (index !== -1) {
        this.items[index].element.classList.remove('ui-selected');
        this.items.splice(index, 1);
        this._addContainerClass();
      }
    }
  }

  unselectAll() {
    $('.res-item.ui-selected').removeClass('ui-selected');
    $('.res-item.ui-draggable-dragging').removeClass('ui-draggable-dragging');
    this.items = [];
    this._addContainerClass();
  }

  selectOnly(element) {
    this.unselectAll();
    this.select(element);
  }

  /**
   * @param {number} index
   * @returns {{id: number, type: string, element: element }}
   */
  getItem(index) {
    return this.items[index];
  }

  size() {
    return this.items.length;
  }

  indexOf(itemType, itemId) {
    for (let i = 0, l = this.items.length; i < l; ++i) {
      if (this.items[i].type === itemType && this.items[i].id === Number(itemId)) {
        return i;
      }
    }
    return -1;
  }

  forEach(func) {
    for (let l = this.items.length, i = 0; i < l; ++i) {
      func(this.items[i], i, this.items);
    }
  }

  toJSON() {
    const exportItems = [];
    for (let l = this.items.length, i = 0; i < l; ++i) {
      exportItems.push({
        itemType: this.items[i].type,
        itemId: this.items[i].id,
      });
    }
    return exportItems;
  }

  getSelectedType() {
    let isContainFolders = false;
    let isContainResources = false;
    for (let i = 0, l = this.items.length; i < l; ++i) {
      if (this.items[i].type === 'resource') {
        isContainResources = true;
        if (isContainFolders) return 'mixed';
      } else if (this.items[i].type === 'folder') {
        isContainFolders = true;
        if (isContainResources) return 'mixed';
      }
    }

    return (isContainFolders ? 'folder' : 'resource') + (this.items.length > 1 ? 's' : '');
  }
}

/** @type {SelectResource} */
const selected = new SelectResource();

/**
 * Context Menu for resources
 * Do not require to re-create when resources updated
 */
function createContextMenu() {
  const $contextmenuItems = $(document.getElementById('contextmenu_items'));
  // check if container is selectable
  if (!$contextmenuItems) return;

  $.contextMenu({
    selector: '.res-container,.res-item',
    // trigger: 'none',
    build($trigger) {
      let triggerType;
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

      const items = {};
      $('li', $contextmenuItems).each((i, el) => {
        const itemMenuTypes = el.dataset.type.split('|');
        if (itemMenuTypes.includes(triggerType)) {
          if ($trigger.data(el.dataset.per) === true) {
            const name = el.textContent;

            items[name.toLowerCase()] = {
              name,
              icon: el.dataset.icon,
              action: el.dataset.action,
            };
          }
        }
      });

      return {
        callback(itemKey, opt) {
          const item = opt.items[itemKey];
          doAction(item.action);
        },
        items,
      };
    },
  });

  if (PF('learnweb').isTouchDevice()) {
    // only on mobile
    $(document).on('click', '.res-item .res-bl-menu', (e) => {
      const resItem = $(e.currentTarget).parents('.res-item');
      resItem.contextMenu({ x: e.pageX, y: e.pageY });

      return false;
    });
  }
}

function isFileDrag(e) {
  return e.dataTransfer && e.dataTransfer.types.includes('Files');
}

function createDropArea(resContainerId) {
  $(window).on('drop dragover', (e) => {
    e.stopPropagation();
    e.preventDefault();
  });

  $(document).on('dragenter dragover', `#${resContainerId}`, (e) => {
    if (isFileDrag(e.originalEvent)) {
      $(e.currentTarget).addClass('ui-state-dragenter');
    }
  }).on('dragleave dragend drop', `#${resContainerId}`, (e) => {
    if (isFileDrag(e.originalEvent)) {
      $(e.currentTarget).removeClass('ui-state-dragenter');
    }
  }).on('drop', `#${resContainerId}`, (e) => {
    // Actually we don't need a check for files.length, but we need to access e.originalEvent.dataTransfer before triggering second event
    if (e.nested || !isFileDrag(e.originalEvent)) return true;
    e.nested = true;

    $('#res_toolbar\\:menu_upload_file').trigger('click');

    window.formLoadedCallback = () => {
      $('#create_resource_form\\:fileUploadInput').trigger(e);
    };
  });
}

function addWebsiteShortcode(url) {
  $('#res_toolbar\\:menu_upload_url').trigger('click');

  window.formLoadedCallback = () => {
    $('#create_resource_form\\:website_url').val(url);
    $('#create_resource_form\\:next_step_btn').trigger('click');
  };
}

function createSelectable(resContainerId) {
  const $resContainer = $(document.getElementById(resContainerId));
  // check if container is selectable
  if (!$resContainer || !$resContainer.data('canselectresources')) return;

  $(document).on('click', '.res-item .res-selector', function (e) { // select using keyboard hot keys
    const resItem = this.closest('.res-item');
    const itemType = resItem.dataset.itemtype;
    const itemId = resItem.dataset.itemid;

    if (selected.indexOf(itemType, itemId) !== -1) {
      selected.unselect(resItem);
    } else {
      selected.select(resItem);
    }

    return false;
  }).on('dblclick', '.res-item .res-selector', (e) => {
    e.stopPropagation();
  });

  $(document).on('click', '.res-item', function (e) { // select using keyboard hot keys
    if (e.shiftKey && selected.size() > 0) { // select all between
      const prevSelected = selected.getItem(selected.size() - 1);
      selected.select(this);
      const lastSelected = selected.getItem(selected.size() - 1);
      if (prevSelected.id !== lastSelected.id) {
        let selectAll = false;
        // eslint-disable-next-line consistent-return
        $('.res-item').each((i, el) => {
          const itemType = el.dataset.itemtype;
          const itemId = Number(el.dataset.itemid);

          if ((itemId === prevSelected.id && itemType === prevSelected.type) || (itemId === lastSelected.id && itemType === lastSelected.type)) {
            if (selectAll) return false;
            selectAll = true;
          } else if (selectAll) {
            selected.select(el);
          }
        });
      }
    } else if (e.ctrlKey || e.metaKey) { // add to selected or remove if already selected
      const itemType = this.dataset.itemtype;
      const itemId = this.dataset.itemid;
      if (selected.indexOf(itemType, itemId) !== -1) {
        selected.unselect(this);
      } else {
        selected.select(this);
      }
    } else {
      selected.selectOnly(this);
    }

    return false;
  });
}

function createSelectableArea(resContainerId) {
  const $resContainer = $(document.getElementById(resContainerId));
  // check if container is selectable
  if (!$resContainer || !$resContainer.data('canselectresources')) return;

  if (PF('learnweb').isTouchDevice()) return; // disable on mobile device
  $resContainer.selectable({
    filter: '.res-item',
    cancel: 'input,textarea,button,select,option,.cancel,.res-item',
    start(e) {
      // noinspection JSUnresolvedVariable
      if (!(e.ctrlKey || e.metaKey)) {
        selected.unselectAll();
      }
    },
    selecting(event, ui) {
      selected.select(ui.selecting);
    },
    unselecting(event, ui) {
      selected.unselect(ui.unselecting);
    },
  });
}

/**
 * Drag & Drop for resources
 * Require to re-create when resources updated (re-bind events)
 */
function createDragAndDrop(resContainerId, resBreadcrumbsId, foldersTreeId) {
  if (PF('learnweb').isTouchDevice()) return; // disable on mobile device

  const $resContainer = $(document.getElementById(resContainerId));

  // check if abe to move resources here
  if (!$resContainer || !$resContainer.data('canmoveresources')) return;

  $('.res-item', $resContainer).draggable({
    addClasses: false,
    helper: 'clone',
    scope: 'resources',
    appendTo: 'body',
    revert: 'invalid',
    cursorAt: { top: 0, left: 0 },
    scroll: false,
    zIndex: ++PrimeFaces.zindex,
    delay: 100,
    start(e, ui) {
      if (!this.classList.contains('ui-selected')) {
        selected.selectOnly(this);
      }

      selected.forEach((item) => {
        item.element.classList.add('ui-draggable-dragging');
      });

      ui.helper[0].classList.add('ui-draggable-helper');
      ui.helper[0].style.width = `${selected.getItem(0).element.offsetWidth}px`;

      if (selected.size() > 1) {
        ui.helper[0].classList.add('has-badge');
        ui.helper[0].setAttribute('data-count', selected.size());
      }
    },
    stop() {
      selected.forEach((item) => {
        item.element.classList.remove('ui-draggable-dragging');
      });
    },
  });

  $('.res-item[data-itemtype="folder"]', $resContainer).droppable({
    tolerance: 'pointer',
    scope: 'resources',
    greedy: true,
    drop() {
      const destFolderId = this.dataset.itemid;
      doAction('move', null, destFolderId);
    },
  });

  const $resBreadcrumbs = $(document.getElementById(resBreadcrumbsId));
  if ($resBreadcrumbs.length) {
    $('li', $resBreadcrumbs).droppable({
      tolerance: 'pointer',
      scope: 'resources',
      greedy: true,
      drop() {
        const destFolderId = this.dataset.folderid;
        doAction('move', null, destFolderId);
      },
    });
  }

  const $foldersTree = $(document.getElementById(foldersTreeId));
  if ($foldersTree.length) {
    $('.ui-treenode', $foldersTree).draggable({
      addClasses: false,
      helper: 'clone',
      scope: 'resources',
      appendTo: 'body',
      revert: 'invalid',
      cursorAt: { top: 0, left: 0 },
      scroll: false,
      zIndex: ++PrimeFaces.zindex,
      delay: 100,
      start(e, ui) {
        selected.selectOnly(this);

        selected.forEach((item) => {
          item.element.classList.add('ui-draggable-dragging');
        });

        ui.helper[0].classList.add('ui-draggable-helper');
        ui.helper[0].style.width = `${selected.getItem(0).element.offsetWidth}px`;
      },
      stop() {
        selected.forEach((item) => {
          item.element.classList.remove('ui-draggable-dragging');
        });
      },
    }).droppable({
      tolerance: 'pointer',
      scope: 'resources',
      greedy: true,
      drop() {
        const destFolderId = this.dataset.datakey;
        doAction('move', null, destFolderId);
      },
    });
  }
}

function openFolder(folderId, pushHistoryState = true) {
  if (pushHistoryState) {
    pushHistoryStateOnOpenFolder(folderId);
  }

  selected.unselectAll();
  commandOpenFolder([
    { name: 'folderId', value: folderId },
  ]);
}

function pushHistoryStateOnOpenFolder(folderId) {
  const sp = new URLSearchParams(window.location.search);
  const oldFolderId = sp.get('folder_id') || 0;
  sp.set('folder_id', folderId);

  if (!window.history.state || window.history.state.folder_id == null) {
    window.history.replaceState({ folder_id: oldFolderId }, null);
  }

  const newUrl = `${window.location.protocol}//${window.location.host}${window.location.pathname}?${sp.toString()}`;
  window.history.pushState({ folder_id: folderId }, null, newUrl);
}

window.addEventListener('popstate', (e) => {
  if (e.state && e.state.folder_id != null && e.state.fancybox_open == null) {
    openFolder(e.state.folder_id, false);
  }
}, false);

function doAction(action, extraAttr1, extraAttr2) {
  switch (action) {
    case 'create-folder':
      $('#res_toolbar\\:menu_create_folder').trigger('click');
      break;
    case 'upload-file':
      $('#res_toolbar\\:menu_upload_file').trigger('click');
      break;
    case 'add-website':
      $('#res_toolbar\\:menu_upload_url').trigger('click');
      break;
    case 'open-folder': {
      const last = selected.getItem(selected.size() - 1);
      if (selected.size() > 0) {
        openFolder(last.id);
      } else {
        console.error('No folder selected.');
      }
      break;
    }
    case 'add-tag':
      if (selected.size() > 0) {
        createConfirmDialog('addTag', () => {
          const $tagInput = document.getElementById('modal_tag_name');
          const tagName = $tagInput.value.trim();
          $tagInput.value = '';

          commandBatchUpdateResources([
            { name: 'action', value: 'add-tag' },
            { name: 'tag', value: tagName },
            { name: 'items', value: JSON.stringify(selected) },
          ]);
        });
      } else {
        console.error('No resources selected.');
      }
      break;
    case 'copy':
      if (selected.size() > 0) {
        createConfirmDialog('selectDestination', () => {
          commandBatchUpdateResources([
            { name: 'action', value: 'copy' },
            { name: 'items', value: JSON.stringify(selected) },
          ]);
        });
      } else {
        console.error('No resources selected.');
      }
      break;
    case 'move':
      if (extraAttr1 || extraAttr2) {
        commandBatchUpdateResources([
          { name: 'action', value: 'move' },
          { name: 'destination', value: JSON.stringify({ groupId: extraAttr1, folderId: extraAttr2 }) },
          { name: 'items', value: JSON.stringify(selected) },
        ]);
      } else if (selected.size() > 0) {
        createConfirmDialog('selectDestination', () => {
          commandBatchUpdateResources([
            { name: 'action', value: 'move' },
            { name: 'items', value: JSON.stringify(selected) },
          ]);
        });
      } else {
        console.error('No resources selected.');
      }
      break;
    case 'edit':
      if (selected.size() === 1) {
        const item = selected.getItem(0);
        if (item.type === 'folder') {
          commandEditFolder([
            { name: 'itemId', value: item.id },
          ]);
        } else {
          openResourceView($('[data-resview="grid"]'), $(item.element), true);
        }
      } else {
        console.error('No resources selected.');
      }
      break;
    case 'delete':
      if (selected.size() > 0) {
        createConfirmDialog('deleteConfirm', () => {
          commandBatchUpdateResources([
            { name: 'action', value: 'delete' },
            { name: 'items', value: JSON.stringify(selected) },
          ]);
        });
      } else {
        console.error('No resources selected.');
      }
      break;
    case 'download':
      if (selected.size() > 0) {
        PF('downloadConfirm').show();
      } else {
        console.error('No resources selected.');
      }
      break;
    default:
      console.error('Unimplemented or unsupported action: ', action);
  }
}

// eslint-disable-next-line
function openItems() {
  const itemId = this.dataset.itemid;
  const itemType = this.dataset.itemtype;

  if (itemId && itemType === 'resource') {
    openResourceView($('[data-resview="grid"]'), $(this));
  } else if (itemId && itemType === 'folder') {
    openFolder(itemId);
  }
}

function createConfirmDialog(dialogId, successCallback) {
  const dialog = PF(dialogId);
  dialog.show();

  dialog.jq.off('click', '.confirm').one('click', '.confirm', (e) => {
    PF(dialogId).hide();
    if (successCallback) successCallback();
    $(e.currentTarget).off();
    return false;
  });
}
