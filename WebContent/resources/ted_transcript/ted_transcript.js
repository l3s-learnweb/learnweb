var escape_key_flag = false;
var selectedNodeId;

$(document).ready(function(){
	
	$(".embedded").contents().each(function(index, node) {
		if (node.nodeType == 8) {
		// node is a comment
		$(node).replaceWith(node.nodeValue);
		}
	});
    
	$('.note').tooltipster({
		functionInit: function(origin, content) {
	        if($(this).data('title'))
	        	return $(this).data('title') + '<hr/>' + $(this).data('content').replace(new RegExp('&lt;br/&gt;','g'),'<br/>');
	        else
	        	return $(this).data('content').replace(new RegExp('&lt;br/&gt;','g'),'<br/>');
	    },
    	contentAsHTML: true,
    	maxWidth: 300,
    	position:'right',
    	theme:'tooltipster-custom-theme'
    });
	
	var selected_words = document.getElementsByClassName("note");
	for(var i=0; i<selected_words.length; i++){
		var selected_word = selected_words[i];
		selected_word.onclick = function(){
	        if (window.confirm("Delete this selection (" + $(this).text() + ")?")) {
	        	saveTranscriptLog([{name:'word', value:$(this).text()},{name:'user_annotation', value:$(this).attr("data-title")},{ name:'action',value:'deselection'}]);
	        	$(this).contents().unwrap();
            }
		};
	}

	$('#text').keypress(function(event){
		var keycode = (event.keyCode ? event.keyCode : event.which);
		if(keycode == '13')
		{
			$('#userinput_ok').click();
			return false;
		}
	});
		
	$(document).keyup(function(event){
		var keycode = (event.keyCode ? event.keyCode : event.which);
		if(keycode == '27')
		{
			$('#userinput_cancel').click();
			return false;
		}

	});
	
	$('.embedded').mousedown(function() {return false;}); 
	$('.tedTranscript').mouseleave(function(){
		deleteSelection();
	});

	//Initializing JQuery contextmenu
	$.contextMenu({
        selector: '.note', 
        items: {
            "add_annotation": {
                name: "Add Annotation",
                callback: function(key, options) {
                    selectedNodeId = this.attr("id");
                    PF('userinput_dialog').show();
                    this.data('annotationDisabled',!this.data('annotationDisabled'));
                    return true;
                },
                disabled: function(key, opt) { 
                    return this.data('annotationDisabled'); 
                }
            },
            "display_def": {
            	name: "Add Definition",
            	callback:function(key, options){
            		selectedNodeId = this.attr("id");
            		setSynonymsForWord([{name:'word', value:this.text()}]);
            		this.data('definitionDisabled',!this.data('definitionDisabled'));
            		return true;
            	},
            	disabled: function(key, opt) { 
                    return this.data('definitionDisabled'); 
                }
            },
        }
    });
});

var usertext = "";
//var synonyms = "";
var sel_str ="";

function setSynonyms(xhr,status,args){
	var synonyms = "";
	synonyms = synonyms + args.synonyms;
	var selectedNode = $('#' + selectedNodeId);	
	if(synonyms != "multiple")
	{
		selectedNode.attr({'data-content':synonyms});
		if(!selectedNode.hasClass('tooltipstered'))
		{
			selectedNode.tooltipster({
				functionInit: function(origin, content) {
			        	return $(this).data('content').replace(new RegExp('&lt;br/&gt;','g'),'<br/>');
			    },
		    	contentAsHTML: true,
		    	maxWidth: 300,
		    	position:'right',
		    	theme:'tooltipster-custom-theme'
		    });
		}
		else
		{
			selectedNode.tooltipster('content',selectedNode.data('title') + '<hr/>' + selectedNode.data('content').replace(new RegExp('&lt;br/&gt;','g'),'<br/>'));
		}
		selectedNode.trigger("mouseenter");
	}
	
	/*noteid++;
	
	var span = document.createElement("span");
	span.setAttribute("class", "note");
	span.setAttribute("id", noteid);
	$(span).on('click',function(){
		if (window.confirm("Delete this selection (" + $(this).text() + ")?")) {
			saveTranscriptLog([{name:'word', value:$(this).text()},{name:'user_annotation', value:$(this).attr("data-title")},{ name:'action',value:'deselection'}]);
			$(this).contents().unwrap();
			$(this).remove();
		}
	});
	var sel = window.getSelection && window.getSelection();
	if(document.selection && !sel)
	{
		var range = document.selection.createRange();
		span.appendChild(range.htmlText);
		range.pasteHTML(span.outerHTML);
		PF('userinput_dialog').show();
	}
	else
	{
		if(sel.rangeCount > 0)
		{
			var range = sel.getRangeAt(0);    
			span.appendChild(range.extractContents());
			range.insertNode(span);
			PF('userinput_dialog').show();
		}
	}*/
	
}

function noteSelectedText() {
	usertext = "";
	escape_key_flag = true;

	if(window.getSelection)
		sel_str = window.getSelection().toString();
	else if(document.selection && document.selection.type != "Control") //support IE browsers
		sel_str = document.selection.createRange().text;
	sel_str = sel_str.trim();
	if(sel_str != "")
	{
		noteid++;
		var span = document.createElement("span");
		span.setAttribute("class", "note");
		span.setAttribute("id", noteid);
		$(span).on('click',function(){
			if (window.confirm("Delete this selection (" + $(this).text() + ")?")) {
				saveTranscriptLog([{name:'word', value:$(this).text()},{name:'user_annotation', value:$(this).attr("data-title")},{ name:'action',value:'deselection'}]);
				$(this).contents().unwrap();
				$(this).remove();
			}
		});
		var sel = window.getSelection && window.getSelection();
		if(document.selection && !sel)
		{
			var range = document.selection.createRange();
			span.appendChild(range.htmlText);
			range.pasteHTML(span.outerHTML);
			//PF('userinput_dialog').show();
		}
		else
		{
			if(sel.rangeCount > 0)
			{
				var range = sel.getRangeAt(0);    
				span.appendChild(range.extractContents());
				range.insertNode(span);
				//PF('userinput_dialog').show();
			}
		}
		//setSynonymsForWord([{name:'word', value:sel_str}]);
	}
}

function deleteSelection() {
	if (window.getSelection) {
		if (window.getSelection().empty) {  // Chrome
			window.getSelection().empty();
		} else if (window.getSelection().removeAllRanges) {  // Firefox
			window.getSelection().removeAllRanges();
		}
	} else if (document.selection) {  // IE
		document.selection.empty();
	}
}

function getUserText(buttonClicked){
	var selectedNode = $('#' + selectedNodeId);
	if(buttonClicked == 'ok')
		usertext = $("#text").val();
	else
		selectedNode.data('annotationDisabled',!selectedNode.data('annotationDisabled'));
	
	PF('userinput_dialog').hide();	
	$("#text").val('');
	
	/*if(synonyms != "multiple")
		$('#'+noteid).attr({'data-title':usertext, 'data-content':synonyms});
	else
		$('#'+noteid).attr({'data-content':usertext});*/
	
	if(usertext != "")
	{   selectedNode.attr({'data-title':usertext});
		if(!selectedNode.hasClass('tooltipstered'))
		{
			selectedNode.tooltipster({
				functionInit: function(origin, content) {
			        	return $(this).data('title');
			    },
		    	contentAsHTML: true,
		    	maxWidth: 300,
		    	position:'right',
		    	theme:'tooltipster-custom-theme'
		    });
		}
		else
		{
			selectedNode.tooltipster('content',selectedNode.data('title') + '<hr/>' + selectedNode.data('content').replace(new RegExp('&lt;br/&gt;','g'),'<br/>'));
		}
		selectedNode.trigger("mouseenter");
	}
	//Tooltip creation
	/*if(synonyms && usertext == "")
		{}
	else
	{
		$('#'+noteid).tooltipster({
			functionInit: function(origin, content) {
		        if($(this).data('title'))
		        	return $(this).data('title') + '<hr/>' + $(this).data('content').replace(new RegExp('&lt;br/&gt;','g'),'<br/>');
		        else
		        	return $(this).data('content').replace(new RegExp('&lt;br/&gt;','g'),'<br/>');
		    },
	    	contentAsHTML: true,
	    	maxWidth: 300,
	    	position:'right',
	    	theme:'tooltipster-custom-theme'
	    });
	}*/
	/*if(escape_key_flag)
	{	saveTranscriptLog([{name:'word', value:sel_str},{name:'user_annotation', value:usertext},{ name:'action',value:'selection'}]);
		escape_key_flag = false;
	}*/
}

function saveEditing(){
	var update = document.getElementById("ted_transcript").innerHTML;
	saveTedResource([{name:'transcript',value:update}]);
}

