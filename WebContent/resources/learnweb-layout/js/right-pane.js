/** @external updateThumbnailCommand */
/** @external editorConfigValues */

/* Show timeline/list of snapshots in resource_view */

function archive_open_timeline() {
    var $archiveListBtn = $('#archive_list_btn');
    var $archiveTimelineBtn = $('#archive_timeline_btn');
    var $archiveListView = $('#archive_list_view');
    var $archiveTimelineView = $('#archive_timeline_view');

    if ($archiveListView.is(':visible')) {
        $archiveListView.slideToggle('slow');
        $archiveListBtn.removeClass('ui-state-active');
    }

    $archiveTimelineView.slideToggle('slow', function () {
        $archiveTimelineBtn.addClass('ui-state-active');
        if ($archiveTimelineView.is(':visible')) {
            var $container = $('#archive_timeline_container');
            $container.show().width($archiveTimelineView.width());
            chart.setSize($archiveTimelineView.width(), $container.height());
            chart.reflow();
        }
    });
}

function archive_open_list() {
    var $archiveListBtn = $('#archive_list_btn');
    var $archiveTimelineBtn = $('#archive_timeline_btn');
    var $archiveListView = $('#archive_list_view');
    var $archiveTimelineView = $('#archive_timeline_view');

    if ($archiveTimelineView.is(':visible')) {
        $archiveTimelineView.slideToggle('slow');
        $archiveTimelineBtn.removeClass('ui-state-active');
    }

    $archiveListView.slideToggle('slow', function () {
        $archiveListBtn.addClass('ui-state-active');
    });
}



/* Modal to see archive versions */

function loadArchiveUrlsModal() {
    $.fancybox.open({
        src: '#modal_archive_urls',
        type: 'inline'
    }, {
        baseClass: 'fancybox-html-archive-urls',
        closeExisting: true,
        smallBtn : false
    });

    $('#archive_iframe').attr('src', function () {
        return $(this).data('src');
    });
}

function updateThumbnail() {
    var archive_url = $('#archive_iframe').attr('src');
    updateThumbnailCommand([{name: 'archive_url', value: archive_url}]);
}

$(document).on('click', '.archive-snapshot-list button', function (e) {
    $(this).toggleClass('outline-btn');
    $(this).next().slideToggle();
});

$(document).on('click', '.archive-snapshot-list a', function (e) {
    var $this = $(this);
    // make button active and reset the previous active element
    $('.archive-snapshot-list span:not(.outline-btn) a').parent().addClass('outline-btn');
    $this.parent().removeClass('outline-btn');

    // update iframe
    $('#archive_timestamp').text($this.text());
    $('#archive_iframe').attr('src', $this.attr('href'));

    e.preventDefault();
});



/* OnlyOffice editor embedded & modal */

var editorFrames = [];
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
        type: 'inline'
    }, {
        baseClass: 'fancybox-iframe-inline',
        closeExisting: true,
        smallBtn : false
    });
}

function loadScript(scriptUrl, callback) {
    var s = document.createElement('script');
    s.type = 'text/javascript';s.src = scriptUrl;s.async = true;s.onload = callback;
    var t = document.getElementsByTagName('script')[0];
    t.parentNode.insertBefore(s, t);
}

function loadEditorScript(clientUrl) {
    if (!editorFrames.loaded) {
        loadScript(clientUrl + '/apps/api/documents/api.js', function () {
            editorFrames.loaded = true;

            editorFrames.push = function (args) {
                attachEditor.apply(null, args);
            };

            for (var i = 0, l = editorFrames.length; i < l; ++i) {
                attachEditor.apply(null, editorFrames[i]);
            }
        });
    }
}

function attachEditor(elementId, editorType, configValues) {
    var history_info = {};
    var docEditor = new DocsAPI.DocEditor(elementId, {
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
                comment: true
            }
        },
        editorConfig: {
            mode: configValues.document.canBeEdited === 'true' ? 'edit' : 'view',
            lang: 'en',
            callbackUrl: configValues.callbackUrl + '&userId=' + configValues.user.id,
            user: {
                id: configValues.user.id,
                name: configValues.user.name
            },
            embedded: {
                saveUrl: configValues.document.url,
                toolbarDocked: 'top'
            }
        },
        events: {
            'onAppReady': function () {
                console.log('Document editor ready');
            },
            'onDocumentStateChange': function (event) {
                var title = document.title.replace(/\*$/g, "");
                document.title = title + (event.data ? '*' : "");
            },
            'onRequestEditRights': function () {
                location.href = location.href.replace(RegExp('action=view\&?', 'i'), "");
            },
            'onError': function (err) {
                console.error(err);
            },
            'onOutdatedVersion': function (event) {
                location.reload(true);
            },
            'onRequestHistoryData': function (event) {
                $.post(configValues.historyUrl + '?version=' + event.data + '&resourceId=' + configValues.document.resourceId, JSON.stringify(history_info), function (json) {
                    docEditor.setHistoryData(json);
                }, 'json');
            },
            'onRequestHistory': function () {
                $.get(configValues.historyUrl + '?resourceId=' + configValues.document.resourceId, function (json) {
                    history_info = json;
                    docEditor.refreshHistory(json);
                });
            },
            'onRequestHistoryClose': function () {
                document.location.reload();
            }
        }
    });

    window.docEditor = docEditor;
};
