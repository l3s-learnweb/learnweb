
var docEditor;

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

			info : {
				author : "Me",
				created : new Date(),
			},

			permissions : {
				edit : canBeEdited,
				download : true,
			}
		},
		editorConfig : {
			mode : mode,

			lang : "en",

			callbackUrl : $('#ed_callback_url').val(),

			user : {
				id : null,
				name : "John Smith",
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
		}
	});
	docEditor;
};
