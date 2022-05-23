var docEditor;

function lightbox_close_for_editor() {
	$('#lightbox').hide();
	$('#lightbox').detach();
	location.reload(true);
}

var innerAlert = function(message) {
	if (console && console.log)
		console.log(message);
};

var onReady = function() {
	innerAlert("Document editor ready");
};

var onDocumentStateChange = function(event) {
	var title = document.title.replace(/\*$/g, "");
	document.title = title + (event.data ? "*" : "");
};

var onRequestEditRights = function() {
	location.href = location.href.replace(RegExp("action=view\&?", "i"), "");
};

var onError = function(event) {
	if (event)
		innerAlert(event.data);
};

var onOutdatedVersion = function(event) {
	location.reload(true);
};

var onRequestHistoryClose = function() {
    document.location.reload();
};

var onRequestHistoryData = function(event) {
    $.post($('#ed_history_url').val() + "&version=" + event.data,function(json) {   
    	docEditor.setHistoryData(json);    
    }, "json");
};

var onRequestHistory = function() {
	$.get($('#ed_history_url').val(), function(json) {  
		docEditor.refreshHistory(json);
	});   
};

/*
jQuery.loadScript = function (url, callback) {
	jQuery.ajax({
		url: url,
		dataType: 'script',
		success: callback,
		async: true
	});
}

if (typeof someObject == 'undefined') $.loadScript('https://haydn.kbs.uni-hannover.de/web-apps/apps/api/documents/api.js', function(){
	//Stuff to do after someScript has loaded
});
*/

var connectEditor = function(id_div, type) {
	var mode = type=="edit"?"edit":"view"; 
	var canBeEdited = type=="edit"?true:false; 
	docEditor = new DocsAPI.DocEditor(id_div, {
		width : "100%",
		height : "100%",
		type : type,
		documentType : $('#ed_file_type').val(),

		document : {
			title : $('#ed_file_name').val(),
			url : $('#ed_file_url').val(),
			fileType : $('#ed_file_ext').val(),
			key : $('#ed_file_key').val(),

			permissions : {
				edit : canBeEdited,
				download : true,
				comment:true
			}
		},
		editorConfig : {
			mode : mode,
			lang : "en",

			callbackUrl : $('#ed_callback_url').val() + "&userId=" + $('#ed_user_id').val(),

			user : {
				id : $('#ed_user_id').val(),
				name : $('#ed_user_name').val()
			},

			embedded : {
				saveUrl : $('#ed_file_url').val() ,
				toolbarDocked : "top"
			}

		},
		events : {
			"onAppReady" : onReady,
			"onDocumentStateChange" : onDocumentStateChange,
			'onRequestEditRights' : onRequestEditRights,
			"onError" : onError,
			"onOutdatedVersion" : onOutdatedVersion,
			"onRequestHistoryData":onRequestHistoryData,
			"onRequestHistory":onRequestHistory,
			"onRequestHistoryClose":onRequestHistoryClose
		}
	});
	docEditor;
};