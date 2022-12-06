/* global Pickr */
/* global commandSaveLog, commandSubmitResource, commandSaveResource, commandSetSynonyms */
/* global noteId:writable, readOnly */

// let escape_key_flag = false;
let isEditAnnotation = false;
let selectedNodeId;
let tags = {};
const deleteSelectionText = document.getElementById('delete_selection').innerHTML;

function saveTranscriptAction(word, annotation, action) {
  commandSaveLog([
    { name: 'word', value: word },
    { name: 'user_annotation', value: annotation },
    { name: 'action', value: action },
  ]);
}

function openTagsDiv() {
  if ($('.tran-tabs-overlay').width() === 0) {
    $('.tran-tabs-overlay').width('256px');
    $('.right_bar').css('margin-right', '256px');
    PF('tabViewVar').select(0);
  }
}

function closeTagsDiv() {
  $('.tran-tabs-overlay').width('0px');
  $('.right_bar').css('margin-right', '0px');
  $('.tran-note').removeClass('ui-state-hover');
  $('.ui-selected').removeClass('ui-selected');
}

function selectTab(id) {
  PF('tabViewVar').select(id);
}

$(() => {
  const selectedElements = $('.tran-note');

  if (readOnly) {
    $(document).on('click', '.tran-note', (e) => {
      const $this = $(e.currentTarget);
      // eslint-disable-next-line no-alert
      if (window.confirm(`${deleteSelectionText}(${this.text()})?`)) {
        saveTranscriptAction($this.text(), $this.attr('data-title'), 'deselection');
        delete tags[$this.attr('id')];
        updateTagList();
        $this.contents().unwrap();
      }
    });
  }

  selectedElements.each(function () {
    const selectedElement = $(this);

    // Initializing tags list with already existing tags in the transcript
    if (selectedElement.attr('data-title')) tags[selectedElement.id] = selectedElement.attr('data-title');

    // Initializing toggle only on the elements which has a data-title or data-content
    if (selectedElement.attr('data-content')) {
      selectedElement.attr('data-content', selectedElement.attr('data-content').replace(/&lt;br\/&gt;/g, '<br/>'));
      selectedElement.attr('data-bs-toggle', 'popover');
    } else if (selectedElement.attr('data-title')) {
      selectedElement.attr('data-bs-toggle', 'tooltip');
    }
  });

  $(document).on('mouseenter', '.tran-note', (e) => {
    $(e.currentTarget).addClass('ui-state-hover');
  }).on('mouseleave', '.tran-note', (e) => {
    $(e.currentTarget).removeClass('ui-state-hover');
  });

  $('#text').on('keypress', (event) => {
    if (event.key === 'Enter') {
      $('#userinput_ok').trigger('click');
      return false;
    }
  });

  $(document).on('keyup', (event) => {
    if (event.key === 'Escape') {
      $('#userinput_cancel').trigger('click');
      return false;
    }
  });

  // To prevent selection when the mouse leaves the transcript HTML element
  $('#ted_embedded').on('mousedown', () => false);

  $('.transcript-container').on('mouseleave', () => {
    deleteSelection();
  });

  if (!readOnly) initializeJQueryContextMenu();

  // initializeResizableDiv();
  $('#selectable').selectable({
    stop() {
      $('.ui-selected', this).each((i, el) => {
        const text = $(el).text();
        $(`[data-title="${text}"]`).addClass('ui-state-hover').tooltip('show');
      });
    },
    unselected(event, ui) {
      const text = $(ui.unselected).text();
      $(`[data-title="${text}"]`).removeClass('ui-state-hover');
    },
  });

  updateTagList();
  /* $(window).on('beforeunload',function(){
      saveEditing();
  }); */

  $('#ted_transcript').tooltip({
    html: true,
    trigger: 'hover',
    selector: '[data-bs-toggle="tooltip"]',
    // eslint-disable-next-line object-shorthand
    title: function () {
      return $(this).attr('data-title');
    },
  }).popover({
    html: true,
    trigger: 'hover',
    selector: '[data-bs-toggle="popover"]',
    // eslint-disable-next-line object-shorthand
    title: function () {
      return $(this).attr('data-title') || '';
    },
    // eslint-disable-next-line object-shorthand
    content: function () {
      return $(this).attr('data-content');
    },
  });
});

// To reset tags list on selecting a new transcript language
function clearTagList() {
  tags = {};
  updateTagList();
}

// To dynamically create/update the tags list in the 'Show Tags' pane
function updateTagList() {
  $('#selectable li').remove();
  $('.tran-note').removeClass('ui-state-hover');

  Object.values(tags).forEach((value) => {
    $('#selectable').append(`<li class="ui-widget-content">${value}</li>`);
  });
}

function initializeJQueryContextMenu() {
  const $contextmenuItems = $(document.getElementById('contextmenu_items'));
  // check if container is selectable
  if (!$contextmenuItems) return;

  $.contextMenu({
    selector: '.tran-note',
    build() {
      const items = {};
      $('li', $contextmenuItems).each((i, el) => {
        const name = el.textContent;
        const { action } = el.dataset;
        items[name.toLowerCase()] = {
          name,
          action,
          icon: el.dataset.icon,
          className: el.dataset.class,
          disabled() {
            if (this.attr('data-title') && action === 'add-annotation') return true;
            if (!this.attr('data-title') && (action === 'edit-annotation' || action === 'delete-annotation')) return true;
            return this.attr('data-content') && action === 'add-wordnet-definition';
          },
        };
      });
      return {
        callback(itemKey, opt) {
          const item = opt.items[itemKey];
          doAction(item, this);
        },
        items,
      };
    },
  });
}

const pickrConfig = {
  theme: 'nano',
  lockOpacity: true,
  useAsButton: true,
  defaultRepresentation: 'HEX',

  swatches: [
    '#000000', '#434343', '#666666', '#b7b7b7', '#cccccc', '#efefef', '#ffffff',
    '#ff0000', '#ff9900', '#ffff00', '#00ff00', '#0000ff', '#9900ff', '#ff00ff',
    '#f4cccc', '#fce5cd', '#fff2cc', '#d9ead3', '#cfe2f3', '#d9d2e9', '#ead1dc',
    '#ea9999', '#f9cb9c', '#ffe599', '#b6d7a8', '#9fc5e8', '#b4a7d6', '#d5a6bd',
    '#e06666', '#f6b26b', '#ffd966', '#93c47d', '#6fa8dc', '#8e7cc3', '#c27ba0',
    '#cc0000', '#e69138', '#f1c232', '#6aa84f', '#3d85c6', '#674ea7', '#a64d79',
    '#990000', '#b45f06', '#bf9000', '#38761d', '#0b5394', '#351c75', '#741b47',
    '#660000', '#783f04', '#7f6000', '#274e13', '#073763', '#20124d', '#4c1130',
  ],

  components: {
    interaction: {
      input: true,
    },
  },
};

function openColorPicker(elId, el) {
  const background = el.css('background-color') || '#ffff00';
  el.css('background-color', background);

  const pickr = new Pickr({
    default: background,
    el: el[0],
    ...pickrConfig,
  }).show();

  pickr.on('change', (color) => {
    el.css('background-color', color.toHEXA().toString());
  }).on('hide', () => {
    pickr.destroyAndRemove();
  });
}

function doAction(action, item) {
  switch (action.action) {
    case 'add-annotation':
      selectedNodeId = item.attr('id');
      PF('userinput_dialog').show();
      break;
    case 'edit-annotation':
      selectedNodeId = item.attr('id');
      $('#text').val(item.attr('data-title'));
      isEditAnnotation = true;
      PF('userinput_dialog').show();
      break;
    case 'delete-annotation':
      selectedNodeId = item.attr('id');
      saveTranscriptAction(item.text(), item.attr('data-title'), 'delete annotation');
      item.removeAttr('data-title');

      if (!item.attr('data-content')) {
        item.removeAttr('data-bs-toggle');
      }
      delete tags[selectedNodeId];
      updateTagList();
      break;
    case 'add-wordnet-definition':
      selectedNodeId = item.attr('id');
      commandSetSynonyms([{ name: 'word', value: item.text() }]);
      break;
    case 'delete-selection':
      // eslint-disable-next-line no-alert
      if (window.confirm(`${deleteSelectionText}(${item.text()})?`)) {
        saveTranscriptAction(item.text(), item.attr('data-title'), 'deselection');
        delete tags[item.attr('id')];
        updateTagList();
        item.contents().unwrap();
        item.remove();
      }
      break;
    case 'colorpicker':
      selectedNodeId = item.attr('id');
      openColorPicker(selectedNodeId, item);
      break;
    default:
      console.error('Unimplemented or unsupported action: ', action);
  }
}

function disableJQueryContextMenu() {
  $('.tran-note').contextMenu(false);
}

// noinspection JSUnusedGlobalSymbols
/**
 * Triggered after user adds WordNet definition
 */
function setSynonyms(xhr, status, args) {
  let synonyms = '';
  synonyms += args.synonyms;
  const selectedNode = $(`span#${selectedNodeId}`);
  if (synonyms.toLowerCase() !== 'multiple') {
    selectedNode.attr('data-content', synonyms.replace(/&lt;br\/&gt;/g, '<br/>'));
    selectedNode.attr('data-bs-toggle', 'popover');

    saveTranscriptAction(selectedNode.text(), '', 'display definition');
  }
}

function getNextNode(node) {
  if (node.firstChild) return node.firstChild;
  while (node) {
    if (node.nextSibling) return node.nextSibling;
    node = node.parentNode;
  }
  return undefined;
}

function getNodesInRange(range) {
  const start = range.startContainer;
  const end = range.endContainer;
  const commonAncestor = range.commonAncestorContainer;
  const nodes = [];
  let node;

  // walk parent nodes from start to common ancestor
  for (node = start; node; node = node.parentNode) {
    if (node.nodeType === 1 && node.tagName.toLowerCase() === 'span') nodes.push(node);
    if (node === commonAncestor) break;
  }
  nodes.reverse();

  // walk children and siblings from start until end is found
  for (node = start; node; node = getNextNode(node)) {
    if (node.nodeType === 1 && node.tagName.toLowerCase() === 'span') nodes.push(node);
    if (node === end) break;
  }

  return nodes;
}

function getWindowSelectionDirection(sel) {
  const position = sel.anchorNode.compareDocumentPosition(sel.focusNode);
  let backward = false;
  // position == 0 if nodes are the same
  if ((!position && sel.anchorOffset > sel.focusOffset) || position === Node.DOCUMENT_POSITION_PRECEDING) backward = true;
  return backward;
}

// Event handler to highlight selected text in transcript
function noteSelectedText() {
  const selStr = window.getSelection().toString().trim();

  if (selStr !== '') {
    noteId++;

    const span = document.createElement('span');
    span.setAttribute('class', 'tran-note');
    span.setAttribute('id', `note${noteId}`);

    $(span).on('mouseenter', (e) => {
      $(e.currentTarget).addClass('ui-state-hover');
    }).on('mouseleave', (e) => {
      $(e.currentTarget).removeClass('ui-state-hover');
    });

    const sel = window.getSelection && window.getSelection();
    const transcriptElement = document.getElementById('ted_transcript');

    if (sel.rangeCount > 0) {
      const range = sel.getRangeAt(0);
      const preCaretRange = range.cloneRange();
      preCaretRange.selectNodeContents(transcriptElement);
      preCaretRange.setEnd(range.startContainer, range.startOffset);
      let start = preCaretRange.toString().length;
      preCaretRange.setEnd(range.endContainer, range.endOffset);
      const end = preCaretRange.toString().length;
      console.log(`starts at:${start}, ends at:${end}`);

      const nodesInRange = getNodesInRange(range);
      if (nodesInRange.length > 0) {
        for (let i = 0; i < nodesInRange.length; i++) {
          const nodeStart = parseInt($(nodesInRange[i]).attr('data-start'), 10);
          const nodeEnd = parseInt($(nodesInRange[i]).attr('data-end'), 10);
          if (start < nodeStart && nodeEnd < end) {
            delete tags[$(nodesInRange[i]).attr('id')];
            updateTagList();
            $(nodesInRange[i]).contents().unwrap();
          }
        }
      }
      // console.log(getNodesInRange(range));
      // console.log(sel.anchorNode.parentNode);
      const backward = getWindowSelectionDirection(sel);
      if ($(sel.anchorNode.parentNode).attr('id') !== 'ted_transcript') {
        if (!backward) {
          start = $(sel.anchorNode.parentNode).attr('data-start');
          $(sel.anchorNode.parentNode).contents().unwrap();
        } else {
          $(sel.anchorNode.parentNode).contents().unwrap();
        }
      }

      if (!($(sel.focusNode.parentNode).attr('id') === 'ted_transcript' || $(sel.focusNode.parentNode).attr('id') === 'transcript')) {
        if (backward) {
          start = $(sel.focusNode.parentNode).attr('data-start');
          $(sel.focusNode.parentNode).contents().unwrap();
        } else {
          $(sel.focusNode.parentNode).contents().unwrap();
        }
      }
      // console.log(sel.focusNode.parentNode);

      span.setAttribute('data-start', start);
      span.setAttribute('data-end', end);

      // range.surroundContents(span);
      // let selectedTextNode = document.createTextNode(range.toString());
      // span.appendChild(selectedTextNode);
      // range.deleteContents();
      span.appendChild(range.extractContents());
      for (let i = 0; i < nodesInRange.length; i++) {
        if (!$(nodesInRange[i]).text().trim().length) $(nodesInRange[i]).remove();
      }

      range.insertNode(span);
      range.selectNode(span);
      sel.removeAllRanges();
      sel.addRange(range);

      // PF('userinput_dialog').show();
    }
    saveTranscriptAction(selStr, '', 'selection');
    // commandSetSynonyms([{name:'word', value:selStr}]);
  }
  deleteSelection();
}

function deleteSelection() {
  if (window.getSelection().removeAllRanges) {
    window.getSelection().removeAllRanges();
  }
}

// noinspection JSUnusedGlobalSymbols
/**
 * Triggered when user open Edit Annotation dialog
 *
 * @param {string} buttonClicked can be `ok` or `cancel` depdends on button user clicked.
 */
function getUserText(buttonClicked) {
  const selectedNode = $(`span#${selectedNodeId}`);
  const inputField = $('#text');

  const userText = inputField.val();
  PF('userinput_dialog').hide();
  inputField.val('');

  if (buttonClicked === 'ok' && userText !== '') {
    selectedNode.attr('data-title', userText);

    if (selectedNode.attr('data-content')) {
      selectedNode.attr('data-bs-toggle', 'popover');
    } else {
      selectedNode.attr('data-bs-toggle', 'tooltip');
    }

    tags[selectedNodeId] = userText;
    updateTagList();
    saveTranscriptAction(selectedNode.text(), userText, isEditAnnotation ? 'edit annotation' : 'add annotation');
    isEditAnnotation = false;
  }
}

function saveEditing() {
  const update = document.getElementById('ted_transcript').innerHTML;
  commandSaveResource([{ name: 'transcript', value: update }]);
}

function submitTranscript() {
  disableJQueryContextMenu();
  const update = document.getElementById('ted_transcript').innerHTML;
  commandSubmitResource([{ name: 'transcript', value: update }]);
}
