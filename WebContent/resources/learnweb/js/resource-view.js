/* global getWidgetVarById, emoji */
/* global editorConfigValues, DocsAPI */

/* Modal to see archive versions */
function loadArchiveUrlsModal() {
  $.fancybox.open({
    src: '#modal_archive_urls',
    type: 'inline',
  }, {
    baseClass: 'fancybox-html-archive-urls',
    closeExisting: true,
    smallBtn: false,
  });

  $('#archive_iframe').attr('src', function () {
    return $(this).data('src');
  });
}

$(document).on('click', '.archive-snapshot-list button', (e) => {
  $(e.currentTarget).toggleClass('outline-btn');
  $(e.currentTarget).nextAll('div').slideToggle();
});

$(document).on('click', '.archive-snapshot-list a.set-thumbnail', (e) => {
  // make button active and reset the previous active element
  $('.archive-snapshot-list span:not(.outline-btn) a').parent().addClass('outline-btn');
  $(e.currentTarget).parent().removeClass('outline-btn');

  // update iframe
  $('#archive_timestamp').text($(e.currentTarget).text());
  $('#archive_iframe').attr('src', $(e.currentTarget).attr('href'));

  e.preventDefault();
});

/* OnlyOffice editor embedded & modal */

const editorFrames = [];

function loadPreviewEditor() {
  if (document.getElementById('iframe_editor')) {
    loadEditorScript(editorConfigValues.officeServerUrl);
    editorFrames.push(['iframe_editor', 'desktop', editorConfigValues]);
  }
}

function loadScript(scriptUrl, callback) {
  const s = document.createElement('script');
  s.type = 'text/javascript';
  s.src = scriptUrl;
  s.async = true;
  s.onload = callback;
  const t = document.getElementsByTagName('script')[0];
  t.parentNode.insertBefore(s, t);
}

function loadEditorScript(officeServerUrl) {
  if (!editorFrames.loaded) {
    loadScript(`${officeServerUrl}/web-apps/apps/api/documents/api.js`, () => {
      editorFrames.loaded = true;

      editorFrames.push = (args) => {
        attachEditor(...args);
      };

      for (let i = 0, l = editorFrames.length; i < l; ++i) {
        attachEditor(...editorFrames[i]);
      }
    });
  }
}

function attachEditor(elementId, editorType, configValues) {
  const docEditor = new DocsAPI.DocEditor(elementId, {
    width: '100%',
    height: '100%',
    type: editorType,
    documentType: configValues.documentType,
    document: {
      title: configValues.document.title,
      url: configValues.document.url,
      fileType: configValues.document.fileType,
      key: configValues.document.key,
      permissions: {
        edit: configValues.document.canBeEdited === 'true',
        download: true,
        comment: true,
      },
    },
    editorConfig: {
      mode: configValues.document.canBeEdited === 'true' ? 'edit' : 'view',
      lang: configValues.lang,
      callbackUrl: configValues.callbackUrl,
      user: configValues.user,
      embedded: {
        saveUrl: configValues.document.url,
        toolbarDocked: 'top',
      },
    },
    events: {
      onAppReady() {
        console.log('Document editor ready');
      },
      onDocumentStateChange(event) {
        const title = document.title.replace(/\*$/g, '');
        document.title = title + (event.data ? '*' : '');
      },
      onRequestEditRights() {
        window.location.href = window.location.href.replace(RegExp('action=view&?', 'i'), '');
      },
      onError(err) {
        console.error(err);
      },
      onOutdatedVersion() {
        window.location.reload();
      },
      onRequestHistoryData(event) {
        // noinspection JSIgnoredPromiseFromCall
        $.post(`${configValues.historyUrl}&version=${event.data}`, (json) => docEditor.setHistoryData(json));
      },
      onRequestHistory() {
        // noinspection JSIgnoredPromiseFromCall
        $.get(configValues.historyUrl, (json) => docEditor.refreshHistory(json));
      },
      onRequestHistoryClose() {
        document.location.reload();
      },
    },
  });

  window.docEditor = docEditor;
}

$(() => {
  $(document).on('keydown', '#tag_text_input', (e) => {
    if (e.key === 'Enter') {
      $('#tag_text_btn').trigger('click');
      return false;
    }
  });

  $(document).on('keydown', '#comment_text_input', (e) => {
    if (e.key === 'Enter') {
      $('#comment_text_btn').trigger('click');
      return false;
    }
  });

  $('.preview-wrapper > img').on('click', (e) => {
    $(e.currentTarget).toggleClass('amplified');
  });

  if (window.self !== window.top) {
    $('.ui-button.navbar-back').on('click', (e) => {
      // eslint-disable-next-line
      parent.$.fancybox.close();
      e.preventDefault();
    });
  }
});

// Edit comments shortcuts
function startEditComment(el) {
  const commentDetails = $(el).closest('.comment-details');
  commentDetails.children('[id$="editCommentControls"]').hide();

  // show editable field
  const editable = commentDetails.children('[id$="editCommentInplace"]').get(0);
  getWidgetVarById(editable.id).show();

  // init emoji button
  if (typeof emoji !== 'undefined') emoji.init();
}

function showEditCommentControls(source) {
  const commentDetails = $(`#${source.replaceAll(':', '\\:')}`).closest('.comment-details');
  commentDetails.children('[id$="editCommentControls"]').show();
}
