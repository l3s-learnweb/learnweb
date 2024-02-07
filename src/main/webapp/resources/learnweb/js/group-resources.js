/* global commandEditFolder, commandBatchUpdateResources, commandOpenFolder, openResourceView */

class SelectResource {
  constructor() {
    this.items = [];
  }

  select(el) {
    if (el.length > 0) {
      for (let i = 0, l = el.length; i < l; ++i) {
        if (!el[i].classList.contains('ui-draggable-helper')) {
          this.selectElement(el[i]);
        }
      }
    } else {
      this.selectElement(el);
    }
  }

  selectElement(el) {
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
        this.updateContainerClass();
      }
    } else {
      console.error('Element type or ID is unknown', el);
    }
  }

  updateContainerClass() {
    if (this.items.length > 0) {
      $('.res-container').addClass('res-highlight-select');
    } else {
      $('.res-container').removeClass('res-highlight-select');
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
        this.updateContainerClass();
      }
    }
  }

  unselectAll() {
    $('.res-item.ui-selected').removeClass('ui-selected');
    $('.res-item.ui-draggable-dragging').removeClass('ui-draggable-dragging');
    this.items = [];
    this.updateContainerClass();
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

function shouldShowMenuItem(el, triggerType, trigger) {
  const itemTypes = el.dataset.type.split('|');
  if (itemTypes.includes(triggerType)) {
    const itemCheck = el.dataset.check;
    return !itemCheck || trigger.data(itemCheck)
  }

  return false;
}

function beforeShowResourceMenu(menu, trigger) {
  let triggerType;
  const $resItem = $(trigger).closest('.res-item');
  if ($resItem.length > 0) {
    // if clicked by an item which is not selected, then reset selection and select only the item
    if (selected.indexOf($resItem.data('itemtype'), $resItem.data('itemid')) === -1) {
      selected.selectOnly($resItem);
    }
    triggerType = selected.getSelectedType();
  } else {
    triggerType = 'container';
    selected.unselectAll(); // clicked on a container, unselect all items
  }

  menu.links.each((i, el) => {
    el.parentNode.classList.toggle('collapse', !shouldShowMenuItem(el, triggerType, $resItem));
  });
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
  if (!$resContainer || !$resContainer.data('canorganizeresources')) return;

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
        const badge = document.createElement('span');
        badge.className = 'ui-badge ui-widget ui-badge-no-gutter ui-badge-lg ui-badge-danger';
        badge.textContent = selected.size();

        ui.helper[0].classList.add('ui-overlay-badge');
        ui.helper[0].appendChild(badge);
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
      doResourceAction('move', null, destFolderId);
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
        doResourceAction('move', null, destFolderId);
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
        doResourceAction('move', null, destFolderId);
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

  window.history.pushState({ folder_id: folderId }, null, `${window.location.pathname}?${sp}`);
}

window.addEventListener('popstate', (e) => {
  if (e.state && e.state.folder_id != null && e.state.fancybox_open == null) {
    openFolder(e.state.folder_id, false);
  }
}, false);

function doResourceAction(action, targetGroupId, targetFolderId) {
  switch (action) {
    case 'create-folder':
      $('#res_toolbar\\:menu_create_folder').trigger('click');
      break;
    case 'create-file':
      $('#res_toolbar\\:menu_upload_file').trigger('click');
      break;
    case 'create-url':
      $('#res_toolbar\\:menu_upload_url').trigger('click');
      break;
    case 'open': {
      const last = selected.getItem(selected.size() - 1);
      if (selected.size() > 0) {
        openFolder(last.id);
      } else {
        console.error('No folder selected.');
      }
      break;
    }
    case 'tag':
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
      if (targetGroupId || targetFolderId) {
        commandBatchUpdateResources([
          { name: 'action', value: 'move' },
          { name: 'destination', value: JSON.stringify({ groupId: targetGroupId, folderId: targetFolderId }) },
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
