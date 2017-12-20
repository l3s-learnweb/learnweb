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

var history_info = {};  


var onRequestHistoryData = function(event) {
	debugger;
	var url = "/Learnweb-Tomcat/history";
    $.post(url+"?version=" + event.data+"&resourceId=" + $('#ed_resource_id').val(),JSON.stringify(history_info), function(json) {   
    	docEditor.setHistoryData(json);    
    }, "json"); 
   
};



var onRequestHistory = function() {
	var url = "/Learnweb-Tomcat/history?resourceId=" + $('#ed_resource_id').val()
        $.get(url, function(json) {  
        	history_info = json;
        	docEditor.refreshHistory(json);
        });   
    };



var сonnectEditor = function(id_div, type) {
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
			}
		},
		editorConfig : {
			mode : mode,

			lang : "en",

			callbackUrl : $('#ed_callback_url').val() + "&userId=" + $('#ed_user_id').val(),

			user : {
				id : $('#ed_user_id').val(),
				name : $('#ed_user_name').val(),
			},

			embedded : {
				saveUrl : $('#ed_file_url').val() ,
				toolbarDocked : "top",
			},

		},
		events : {
			"onReady" : onReady,
			"onDocumentStateChange" : onDocumentStateChange,
			'onRequestEditRights' : onRequestEditRights,
			"onError" : onError,
			"onOutdatedVersion" : onOutdatedVersion,
			"onRequestHistoryData":onRequestHistoryData,
			"onRequestHistory":onRequestHistory,
			"onRequestHistoryClose":onRequestHistoryClose,
		}
	});
	docEditor;
};
