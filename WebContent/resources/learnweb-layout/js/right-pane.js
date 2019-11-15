/* global updateThumbnailCommand, editorConfigValues, DocsAPI */

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

function updateThumbnail() {
  const archiveUrl = $('#archive_iframe').attr('src');
  updateThumbnailCommand([{ name: 'archive_url', value: archiveUrl }]);
}

$(document).on('click', '.archive-snapshot-list button', (e) => {
  $(e.currentTarget).toggleClass('outline-btn');
  $(e.currentTarget).next().slideToggle();
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
    loadEditorScript(editorConfigValues.clientUrl);
    editorFrames.push(['iframe_editor', 'embedded', editorConfigValues]);
  }
}

function loadModalEditor() {
  loadEditorScript(editorConfigValues.clientUrl);
  attachEditor('modal_editor', 'desktop', editorConfigValues);

  $.fancybox.open({
    src: '#modal_editor_wrapper',
    type: 'inline',
  }, {
    baseClass: 'fancybox-iframe-inline',
    closeExisting: true,
    smallBtn: false,
  });
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

function loadEditorScript(clientUrl) {
  if (!editorFrames.loaded) {
    loadScript(`${clientUrl}/apps/api/documents/api.js`, () => {
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
  let historyInfo = {};
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
      lang: 'en',
      callbackUrl: `${configValues.callbackUrl}&userId=${configValues.user.id}`,
      user: {
        id: configValues.user.id,
        name: configValues.user.name,
      },
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
        $.post(`${configValues.historyUrl}?version=${event.data}&resourceId=${configValues.document.resourceId}`, JSON.stringify(historyInfo), (json) => {
          docEditor.setHistoryData(json);
        }, 'json');
      },
      onRequestHistory() {
        // noinspection JSIgnoredPromiseFromCall
        $.get(`${configValues.historyUrl}?resourceId=${configValues.document.resourceId}`, (json) => {
          historyInfo = json;
          docEditor.refreshHistory(json);
        });
      },
      onRequestHistoryClose() {
        document.location.reload();
      },
    },
  });

  window.docEditor = docEditor;
}
