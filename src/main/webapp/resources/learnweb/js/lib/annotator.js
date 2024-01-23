/* global bootstrap, Pickr */
/* global commandSaveLog, commandSubmitResource, commandSaveResource, commandGetDefinition */

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

const defaultOptions = {
  readOnly: false,
  lastNoteId: 0,
  defaultBackground: '#ffff00',

  editNoteFormSelector: '#update_note_form',
  tabsOverlaySelector: undefined,
  noteSelector: '.tran-note',
  noteTitleAttribute: 'data-title',
  noteContentAttribute: 'data-content',

  noteContextMenu: [
    { action: 'add-annotation', icon: 'fa-plus', name: 'Add annotation' },
    { action: 'edit-annotation', icon: 'fa-pencil-alt', name: 'Edit annotation' },
    { action: 'delete-annotation', icon: 'fa-minus', name: 'Delete annotation' },
    { action: 'add-wordnet-definition', icon: 'fa-language', name: 'Add Wordnet Definition' },
    { action: 'delete-selection', icon: 'fa-trash', name: 'Delete selection' },
    { action: 'colorpicker', icon: 'fa-folder', name: 'Color' },
  ],

  locale: {
    deleteSelection: 'Are you sure yo want to delete this selection?',
  },
};

class Annotator {
  constructor(elSelector, options) {
    this.options = { ...defaultOptions, ...options };
    console.trace('Annotator initialized with options', this.options);

    // store nodeIds
    this.lastNoteId = this.options.lastNoteId; // AUTO_INCREASE like, unique increment IDs for new nodes
    this.selectedNoteId = undefined;
    this.containerEl = document.querySelector(elSelector);

    this.isEditAnnotation = false; // stores whether current input is for new node or edit existing one
    this.tags = {}; // stores all the texts entered for nodes

    this.initTooltips();
    this.initTagsOverlay();

    if (!this.options.readOnly) {
      this.initJQueryContextMenu();
      this.initSelectable();
    }
  }

  initSelectable() {
    this.containerEl.addEventListener('mouseup', () => {
      const selection = window.getSelection();
      const selectionString = selection.toString().trim();

      if (selectionString !== '' && selection.rangeCount > 0) {
        this.lastNoteId++;

        const span = document.createElement('span');
        span.setAttribute('class', this.options.noteSelector.substring(1));
        span.setAttribute('id', String(this.lastNoteId));

        const range = selection.getRangeAt(0);
        const preCaretRange = range.cloneRange();
        preCaretRange.selectNodeContents(this.containerEl);
        preCaretRange.setEnd(range.startContainer, range.startOffset);
        let start = preCaretRange.toString().length;
        preCaretRange.setEnd(range.endContainer, range.endOffset);
        const end = preCaretRange.toString().length;
        console.trace(`Selection starts at:${start}, ends at:${end}`);

        const nodesInRange = getNodesInRange(range);
        nodesInRange.forEach((nodeInRange) => {
          const nodeStart = parseInt(nodeInRange.getAttribute('data-start'), 10);
          const nodeEnd = parseInt(nodeInRange.getAttribute('data-end'), 10);

          // remove node
          if (start < nodeStart && nodeEnd < end) {
            delete this.tags[nodeInRange.id];
            this.updateTagList();
            nodeInRange.replaceWith(...nodeInRange.childNodes); // removes <span> wrapping
            nodeInRange.remove();
          }
        });

        const backward = getWindowSelectionDirection(selection);
        if (selection.anchorNode.parentNode.id !== this.containerEl.id) {
          if (!backward) {
            start = selection.anchorNode.parentNode.getAttribute('data-start');
          }
          selection.anchorNode.parentNode.replaceWith(...selection.anchorNode.parentNode.childNodes);
        } else if (selection.focusNode.id !== this.containerEl.id && selection.focusNode.parentNode.id !== this.containerEl.id) {
          if (backward) {
            start = selection.focusNode.parentNode.getAttribute('data-start');
          }
          selection.focusNode.parentNode.replaceWith(...selection.focusNode.parentNode.childNodes);
        }

        span.setAttribute('data-start', start);
        span.setAttribute('data-end', end);
        span.appendChild(range.extractContents());

        range.insertNode(span);
        range.selectNode(span);
        selection.removeAllRanges();
        selection.addRange(range);

        this.logAction(selectionString, '', 'selection');
      }
      window.getSelection().removeAllRanges();
    }, false);

    this.containerEl.addEventListener('mouseleave', () => window.getSelection().removeAllRanges());
  }

  initTooltips() {
    const self = this;

    // add hover effect for all notes inside the container
    this.containerEl.addEventListener('mouseenter', (e) => {
      if (e.target.matches(this.options.noteSelector)) {
        e.target.classList.add('ui-state-hover');
      }
    }, true);
    this.containerEl.addEventListener('mouseleave', (e) => {
      if (e.target.matches(this.options.noteSelector)) {
        e.target.classList.remove('ui-state-hover');
      }
    }, true);

    // Initializing toggle only on the elements, which has a data-title or data-content
    document.querySelectorAll(this.options.noteSelector).forEach((selectedElement) => {
      if (selectedElement.hasAttribute(this.options.noteContentAttribute)) {
        const content = selectedElement.getAttribute(this.options.noteContentAttribute).replace(/&lt;br\/&gt;/g, '<br/>');
        selectedElement.setAttribute(this.options.noteContentAttribute, content);
        selectedElement.setAttribute('data-bs-toggle', 'popover');
      } else if (selectedElement.hasAttribute(this.options.noteTitleAttribute)) {
        selectedElement.setAttribute('data-bs-toggle', 'tooltip');
      }
    });

    const tooltip = new bootstrap.Tooltip(this.containerEl, {
      html: true,
      trigger: 'hover',
      selector: '[data-bs-toggle="tooltip"]',
      title: (el) => el.getAttribute(self.options.noteTitleAttribute),
    });

    const popover = new bootstrap.Popover(this.containerEl, {
      html: true,
      trigger: 'hover',
      selector: '[data-bs-toggle="popover"]',
      title: (el) => el.getAttribute(self.options.noteTitleAttribute) || '',
      content: (el) => el.getAttribute(self.options.noteContentAttribute),
    });
  }

  initTagsOverlay() {
    const self = this;

    // Initializing tag list with already existing tags
    document.querySelectorAll(this.options.noteSelector).forEach((selectedElement) => {
      if (selectedElement.hasAttribute(this.options.noteTitleAttribute)) {
        this.tags[selectedElement.id] = selectedElement.getAttribute(this.options.noteTitleAttribute);
      }
    });

    // noinspection JSUnusedGlobalSymbols
    $('#selectable').selectable({
      stop() {
        $('.ui-selected', this).each((i, el) => {
          const text = $(el).text();
          $(`[${self.options.noteTitleAttribute}="${text}"]`).addClass('ui-state-hover').tooltip('show');
        });
      },
      unselected(event, ui) {
        const text = $(ui.unselected).text();
        $(`[${self.options.noteTitleAttribute}="${text}"]`).removeClass('ui-state-hover').tooltip('hide');
      },
    });

    this.updateTagList();
  }

  initJQueryContextMenu() {
    const self = this;
    $.contextMenu({
      selector: this.options.noteSelector,
      build() {
        return {
          callback(itemKey, opt) {
            const item = opt.items[itemKey];

            self.selectedNoteId = this.attr('id');
            const prevTitle = this.attr(self.options.noteTitleAttribute);
            switch (item.action) {
              case 'add-annotation':
                PF('updateNote').show();
                break;
              case 'edit-annotation':
                $(`${self.options.editNoteFormSelector} input`).val(prevTitle);
                self.isEditAnnotation = true;
                PF('updateNote').show();
                break;
              case 'delete-annotation':
                self.logAction(this.text(), prevTitle, 'delete annotation');
                this.removeAttr(self.options.noteTitleAttribute);
                if (!this.attr(self.options.noteContentAttribute)) {
                  this.removeAttr('data-bs-toggle');
                }
                delete self.tags[self.selectedNoteId];
                self.updateTagList();
                break;
              case 'add-wordnet-definition':
                commandGetDefinition([{ name: 'term', value: this.text() }]);
                break;
              case 'delete-selection':
                // eslint-disable-next-line no-alert
                if (window.confirm(`${self.options.locale.deleteSelection}\n\n${this.text()}`)) {
                  self.logAction(this.text(), prevTitle, 'deselection');
                  delete self.tags[self.selectedNoteId];
                  self.updateTagList();
                  this.contents().unwrap();
                  this.remove();
                  self.selectedNoteId = undefined;
                }
                break;
              case 'colorpicker':
                self.openColorPicker(self.selectedNoteId, this);
                break;
              default:
                console.error('Unimplemented or unsupported action: ', item);
            }
          },
          items: self.options.noteContextMenu.map((row) => ({
            name: row.name,
            action: row.action,
            icon: row.icon,
            className: row.class,
            disabled() {
              const hasAnnotation = this.attr(self.options.noteTitleAttribute);
              const hasDefinition = this.attr(self.options.noteContentAttribute);

              if (row.action === 'add-annotation' && hasAnnotation) return true;
              if ((row.action === 'edit-annotation' || row.action === 'delete-annotation') && !hasAnnotation) return true;
              return row.action === 'add-wordnet-definition' && hasDefinition;
            },
          })),
        };
      },
    });
  }

  // To dynamically create/update the tags list in the 'Show Tags' pane
  updateTagList() {
    document.querySelectorAll('#selectable li').forEach((e) => e.remove());
    document.querySelectorAll(`${this.options.noteSelector}.ui-state-hover`).forEach((e) => e.classList.remove('ui-state-hover'));

    Object.values(this.tags).forEach((value) => {
      const listNode = document.createElement('li');
      listNode.classList.add('ui-widget-content');
      listNode.innerHTML = String(value);
      document.getElementById('selectable').appendChild(listNode);
    });
  }

  // To reset a tag list on selecting a new language
  // noinspection JSUnusedGlobalSymbols
  clearTagList() {
    this.tags = {};
    this.updateTagList();
  }

  openTagsDiv(tabId = 0) {
    if (this.options.tabsOverlaySelector) {
      const width = parseFloat(getComputedStyle(document.querySelector(this.options.tabsOverlaySelector), null).width.replace('px', ''));
      if (width === 0) {
        document.querySelector(this.options.tabsOverlaySelector).style.width = '256px';
        document.querySelector('#transcript_column').style.marginRight = '256px';
        PF('transcriptTabView').select(tabId);
      }
    }
  }

  closeTagsDiv() {
    if (this.options.tabsOverlaySelector) {
      document.querySelector(this.options.tabsOverlaySelector).style.width = '0px';
      document.querySelector('#transcript_column').style.marginRight = '0px';
      document.querySelectorAll(`${this.options.noteSelector}.ui-state-hover`).forEach((e) => e.classList.remove('ui-state-hover'));
      document.querySelectorAll('.ui-selected').forEach((e) => e.classList.remove('ui-selected'));
    }
  }

  /**
   * @param elId
   * @param {jQuery} el
   */
  openColorPicker(elId, el) {
    const background = el.css('background-color') || this.options.defaultBackground;
    el.css('background-color', background);

    const pickr = new Pickr({ default: background, el: el[0], ...pickrConfig }).show();
    pickr.on('change', (color) => {
      el.css('background-color', color.toHEXA().toString());
    }).on('hide', () => {
      pickr.destroyAndRemove();
    });
  }

  disableJQueryContextMenu() {
    $.contextMenu('destroy', this.options.noteSelector);
  }

  // noinspection JSUnusedGlobalSymbols
  /**
   * Triggered after user adds WordNet definition
   */
  saveDefinition(xhr, status, args) {
    const { synonyms } = args;
    if (synonyms) {
      const selectedNote = this.containerEl.querySelector(`span#${CSS.escape(this.selectedNoteId)}`);
      selectedNote.setAttribute(this.options.noteContentAttribute, synonyms.replace(/&lt;br\/&gt;/g, '<br/>'));
      selectedNote.setAttribute('data-bs-toggle', 'popover');

      this.logAction(selectedNote.textContent, '', 'display definition');
    }
  }

  // noinspection JSUnusedGlobalSymbols
  /**
   * Triggered when user open Edit Annotation dialog
   */
  saveAnnotation() {
    const selectedNote = this.containerEl.querySelector(`span#${CSS.escape(this.selectedNoteId)}`);
    const userText = document.querySelector(`${this.options.editNoteFormSelector} input`).value;

    if (userText !== '') {
      selectedNote.setAttribute(this.options.noteTitleAttribute, userText);

      if (selectedNote.hasAttribute(this.options.noteContentAttribute)) {
        selectedNote.setAttribute('data-bs-toggle', 'popover');
      } else {
        selectedNote.setAttribute('data-bs-toggle', 'tooltip');
      }

      this.tags[this.selectedNoteId] = userText;
      this.updateTagList();
      this.logAction(selectedNote.textContent, userText, this.isEditAnnotation ? 'edit annotation' : 'add annotation');
      this.isEditAnnotation = false;
    }

    this.cancelAnnotation();
  }

  // eslint-disable-next-line class-methods-use-this
  cancelAnnotation() {
    document.querySelector(`${this.options.editNoteFormSelector} input`).value = '';
    PF('updateNote').hide();
  }

  // eslint-disable-next-line class-methods-use-this
  logAction(word, annotation, action) {
    commandSaveLog([
      { name: 'action', value: action },
      { name: 'selection', value: word },
      { name: 'annotation', value: annotation },
    ]);
  }

  saveAnnotatedText() {
    const update = this.containerEl.innerHTML;
    commandSaveResource([{ name: 'annotatedText', value: update }]);
  }

  submitTranscript() {
    this.disableJQueryContextMenu();
    const update = this.containerEl.innerHTML;
    commandSubmitResource([{ name: 'transcript', value: update }]);
  }
}

// Private static functions
/**
 * @param {Range} range
 * @returns {Node[]}
 */
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

/**
 * @param {Node} node
 * @returns {undefined|Node}
 */
function getNextNode(node) {
  if (node.firstChild) return node.firstChild;
  while (node) {
    if (node.nextSibling) return node.nextSibling;
    node = node.parentNode;
  }
  return undefined;
}

/**
 * @param {Selection} sel
 * @returns {boolean}
 */
function getWindowSelectionDirection(sel) {
  const position = sel.anchorNode.compareDocumentPosition(sel.focusNode);
  let backward = false;
  // position == 0 if nodes are the same
  if ((!position && sel.anchorOffset > sel.focusOffset) || position === Node.DOCUMENT_POSITION_PRECEDING) backward = true;
  return backward;
}
