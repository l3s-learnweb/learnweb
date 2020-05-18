/* global saveTranscriptLog, submitTedResource, saveTedResource, setSynonymsForWord */
/* global noteId:writable, readOnly */

// let escape_key_flag = false;
let isEditAnnotation = false;
let selectedNodeId;
let tags = {};
const deleteSelectionText = document.getElementById('delete_selection').innerHTML;

function saveTranscriptAction(word, annotation, action) {
  saveTranscriptLog([
    { name: 'word', value: word },
    { name: 'user_annotation', value: annotation },
    { name: 'action', value: action },
  ]);
}

/* function openTab(evt, divName) {
    let i, tabcontent, tablinks;
    tabcontent = document.getElementsByClassName('overlay-content');
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = 'none';
    }
    tablinks = document.getElementsByClassName('tablinks');
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace('ui-state-active', "");
    }
    document.getElementById(divName).style.display = 'block';
    $(evt.currentTarget).addClass('ui-state-active');

} */

/*
function selectTab(divName) {
  let i, tabcontent, tablinks;
    tabcontent = document.getElementsByClassName('overlay-content');
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = 'none';
    }
    tablinks = document.getElementsByClassName('tablinks');
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace('ui-state-active', "");
    }
    document.getElementById(divName).style.display = 'block';
  document.getElementById('test123:' + divName + '_button').classList.add('ui-state-active');
} */

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
  const selectedElements = document.getElementsByClassName('tran-note');
  if (readOnly) {
    $(document).on('click', '.tran-note', (e) => {
      const $this = $(e.currentTarget);
      // eslint-disable-next-line no-alert
      if (window.confirm(`${deleteSelectionText}(${this.text()})?`)) {
        saveTranscriptAction($this.text(), $this.data('title'), 'deselection');
        delete tags[$this.attr('id')];
        updateTagList();
        $this.contents().unwrap();
      }
    });
  }
  for (let i = 0; i < selectedElements.length; i++) {
    const selectedElement = selectedElements[i];
    // Initializing tags list with already existing tags in the transcript
    if (selectedElement.getAttribute('data-title')) tags[selectedElement.id] = selectedElement.getAttribute('data-title');

    // Initializing Tooltipster only on the elements which has a data-title or data-content
    if ($(selectedElement).data('title') || $(selectedElement).data('content')) {
      $(selectedElement).tooltipster({
        functionInit() {
          const $this = $(this);
          if ($this.data('title') && $this.data('content')) {
            return `${$this.data('title')}<hr/>${$this.data('content').replace(new RegExp('&lt;br/&gt;', 'g'), '<br/>')}`;
          }
          if ($this.data('content')) {
            return $this.data('content').replace(new RegExp('&lt;br/&gt;', 'g'), '<br/>');
          }
          if ($this.data('title')) {
            return $this.data('title');
          }
        },
        contentAsHTML: true,
        maxWidth: 300,
        position: 'left',
        theme: 'tooltipster-transcript',
      });
    }

    $(selectedElement).on('mouseenter', (e) => {
      $(e.currentTarget).addClass('ui-state-hover');
    });

    $(selectedElement).on('mouseleave', (e) => {
      $(e.currentTarget).removeClass('ui-state-hover');
    });
  }

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
  $('.embed-responsive').on('mousedown', () => false);

  $('.transcript-container').on('mouseleave', () => {
    deleteSelection();
  });

  if (!readOnly) initializeJQueryContextMenu();

  // initializeResizableDiv();
  $('#selectable').selectable({
    stop() {
      $('.ui-selected', this).each((i, el) => {
        const text = $(el).text();
        $(`[data-title="${text}"]`).addClass('ui-state-hover').tooltipster('open');
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
            if (this.data('annotationdisabled') && action === 'add-annotation') return true;
            return !!(this.data('definitiondisabled') && action === 'add-wordnet-definition');
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


function doAction(action, item) {
  switch (action.action) {
    case 'add-annotation':
      selectedNodeId = item.attr('id');
      PF('userinput_dialog').show();
      item.data('annotationdisabled', true);
      break;
    case 'edit-annotation':
      selectedNodeId = item.attr('id');
      $('#text').val(item.attr('data-title'));
      isEditAnnotation = true;
      PF('userinput_dialog').show();
      break;
    case 'delete-annotation':
      selectedNodeId = item.attr('id');
      saveTranscriptAction(item.text(), item.data('title'), 'delete annotation');
      item.removeAttr('data-title');
      if (item.data('content')) {
        item.tooltipster('content', item.data('content').replace(new RegExp('&lt;br/&gt;', 'g'), '<br/>'));
      } else {
        item.tooltipster('destroy');
        item.removeAttr('title');
      }
      delete tags[selectedNodeId];
      updateTagList();
      item.data('annotationdisabled', false);
      break;
    case 'add-wordnet-definition':
      selectedNodeId = item.attr('id');
      setSynonymsForWord([{ name: 'word', value: item.text() }]);
      item.data('definitiondisabled', item.data('definitiondisabled') !== true);
      break;
    case 'delete-selection':
      // eslint-disable-next-line no-alert
      if (window.confirm(`${deleteSelectionText}(${item.text()})?`)) {
        saveTranscriptAction(item.text(), item.data('title'), 'deselection');
        delete tags[item.attr('id')];
        updateTagList();
        item.contents().unwrap();
        item.remove();
      }
      break;
    case 'colorpicker':
      selectedNodeId = item.attr('id');
      $('.color-picker-field').spectrum({
        showPaletteOnly: true,
        togglePaletteOnly: true,
        togglePaletteMoreText: 'more',
        togglePaletteLessText: 'less',
        hideAfterPaletteSelect: true,
        color: 'blanchedalmond',
        palette: [
          ['#000', '#444', '#666', '#999', '#ccc', '#eee', '#f3f3f3', '#fff'],
          ['#f00', '#f90', '#ff0', '#0f0', '#0ff', '#00f', '#90f', '#f0f'],
          ['#f4cccc', '#fce5cd', '#fff2cc', '#d9ead3', '#d0e0e3', '#cfe2f3', '#d9d2e9', '#ead1dc'],
          ['#ea9999', '#f9cb9c', '#ffe599', '#b6d7a8', '#a2c4c9', '#9fc5e8', '#b4a7d6', '#d5a6bd'],
          ['#e06666', '#f6b26b', '#ffd966', '#93c47d', '#76a5af', '#6fa8dc', '#8e7cc3', '#c27ba0'],
          ['#c00', '#e69138', '#f1c232', '#6aa84f', '#45818e', '#3d85c6', '#674ea7', '#a64d79'],
          ['#900', '#b45f06', '#bf9000', '#38761d', '#134f5c', '#0b5394', '#351c75', '#741b47'],
          ['#600', '#783f04', '#7f6000', '#274e13', '#0c343d', '#073763', '#20124d', '#4c1130'],
        ],
        change(color) {
          $(`#${selectedNodeId}`).css('background-color', color.toHexString());
        },
      });
      break;
    default:
      console.error('Unimplemented or unsupported action: ', action);
  }
}

function disableJQueryContextMenu() {
  $('.tran-note').contextMenu(false);
}

let usertext = '';
let selStr = '';

// noinspection JSUnusedGlobalSymbols
function setSynonyms(xhr, status, args) {
  let synonyms = '';
  synonyms += args.synonyms;
  const selectedNode = $(`#${selectedNodeId}`);
  if (synonyms !== 'multiple') {
    selectedNode.attr('data-content', synonyms);
    if (!selectedNode.hasClass('tooltipstered')) {
      selectedNode.tooltipster({
        functionInit() {
          return $(this).data('content').replace(new RegExp('&lt;br/&gt;', 'g'), '<br/>');
        },
        contentAsHTML: true,
        maxWidth: 300,
        position: 'right',
        theme: 'tooltipster-transcript',
      });
    } else {
      selectedNode.tooltipster('content', `${selectedNode.data('title')}<hr/>${selectedNode.data('content')
        .replace(new RegExp('&lt;br/&gt;', 'g'), '<br/>')}`);
    }
    selectedNode.trigger('mouseenter');
    saveTranscriptAction(selectedNode.text(), '', 'display definition');
  }

  /* noteId++;

  let span = document.createElement('span');
  span.setAttribute('class', 'tran-note');
  span.setAttribute('id', noteId);
  $(span).on('click',function(){
      if (window.confirm('Delete this selection (' + $(this).text() + ')?')) {
          saveTranscriptAction($(this).text(), $(this).data('title'), 'deselection');
          $(this).contents().unwrap();
          $(this).remove();
      }
  });
  let sel = window.getSelection && window.getSelection();
  if(sel.rangeCount > 0)
  {
      let range = sel.getRangeAt(0);
      span.appendChild(range.extractContents());
      range.insertNode(span);
      PF('userinput_dialog').show();
  } */
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
  usertext = '';
  // escape_key_flag = true;

  selStr = window.getSelection().toString();
  selStr = selStr.trim();

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
          const nodeStart = parseInt($(nodesInRange[i]).data('start'), 10);
          const nodeEnd = parseInt($(nodesInRange[i]).data('end'), 10);
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
          start = $(sel.anchorNode.parentNode).data('start');
          $(sel.anchorNode.parentNode).contents().unwrap();
        } else {
          $(sel.anchorNode.parentNode).contents().unwrap();
        }
      }

      if (!($(sel.focusNode.parentNode).attr('id') === 'ted_transcript' || $(sel.focusNode.parentNode).attr('id') === 'transcript')) {
        if (backward) {
          start = $(sel.focusNode.parentNode).data('start');
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
    // setSynonymsForWord([{name:'word', value:selStr}]);
  }
  deleteSelection();
}

function deleteSelection() {
  if (window.getSelection().removeAllRanges) {
    window.getSelection().removeAllRanges();
  }
}

function getUserText(buttonClicked) {
  const selectedNode = $(`#${selectedNodeId}`);
  if (buttonClicked === 'ok') {
    usertext = $('#text').val();
  } else {
    selectedNode.data('annotationdisabled', selectedNode.data('annotationdisabled') !== true);
  }

  PF('userinput_dialog').hide();
  $('#text').val('');

  /* if(synonyms != 'multiple')
      $('#'+noteId).attr({'data-title':usertext, 'data-content':synonyms});
  else
      $('#'+noteId).attr({'data-content':usertext}); */

  if (usertext !== '') {
    selectedNode.attr({ 'data-title': usertext });
    if (!selectedNode.hasClass('tooltipstered')) {
      selectedNode.tooltipster({
        functionInit() {
          return $(this).data('title');
        },
        contentAsHTML: true,
        maxWidth: 300,
        position: 'right',
        theme: 'tooltipster-transcript',
      });
    } else if (selectedNode.data('content')) {
      selectedNode.tooltipster('content', `${selectedNode.attr('data-title')}<hr/>${selectedNode.attr('data-content')
        .replace(new RegExp('&lt;br/&gt;', 'g'), '<br/>')}`);
    } else {
      selectedNode.tooltipster('content', selectedNode.attr('data-title'));
    }

    selectedNode.trigger('mouseenter');
    tags[selectedNodeId] = usertext;
    updateTagList();
    saveTranscriptAction(selectedNode.text(), usertext, isEditAnnotation ? 'edit annotation' : 'add annotation');

    isEditAnnotation = false;
  }
  // Tooltip creation
  /* if(synonyms && usertext == "")
      {}
  else
  {
      $('#'+noteId).tooltipster({
          functionInit: function() {
              if($(this).data('title'))
                  return $(this).data('title') + '<hr/>' + $(this).data('content').replace(new RegExp('&lt;br/&gt;','g'),'<br/>');
              else
                  return $(this).data('content').replace(new RegExp('&lt;br/&gt;','g'),'<br/>');
          },
          contentAsHTML: true,
          maxWidth: 300,
          position:'right',
          theme:'tooltipster-transcript'
      });
  } */
  /* if(escape_key_flag)
  {  saveTranscriptAction(selStr, usertext, 'selection');
      escape_key_flag = false;
  } */
}

function saveEditing() {
  const update = document.getElementById('ted_transcript').innerHTML;
  saveTedResource([{ name: 'transcript', value: update }]);
}

function submitTranscript() {
  disableJQueryContextMenu();
  const update = document.getElementById('ted_transcript').innerHTML;
  submitTedResource([{ name: 'transcript', value: update }]);
}
