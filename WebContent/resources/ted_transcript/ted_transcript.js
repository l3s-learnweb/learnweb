var escape_key_flag = false;
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
	//Initializing rangy highlighter
    /*rangy.init();
    highlighter = rangy.createHighlighter();
    //Adding the css class highlight to the rangy highlighter object
    highlighter.addClassApplier(rangy.createCssClassApplier("highlight", {
        ignoreWhiteSpace: true,
        tagNames: ["span", "a"]
    }));*/
});

var highlighter;
var usertext = "";
var synonyms = "";
var sel_str ="";

function highlightSelectedText() {
    highlighter.highlightSelection("highlight");
}

function setSynonyms(xhr,status,args){
	synonyms = "";
	synonyms = synonyms + args.synonyms;
		
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
	}
	
	/*highlighter.addClassApplier(rangy.createCssClassApplier("note", {
        ignoreWhiteSpace: true,
        elementTagName: "span",
        elementProperties: {
        	id: noteid,
        	onclick: function() {
                var highlight = highlighter.getHighlightForElement(this);
                if (window.confirm("Delete this selection (" + $(this).text() + ")?")) {
                    highlighter.removeHighlights( [highlight] );
                	saveTranscriptLog([{name:'word', value:$(this).text()},{name:'user_annotation', value:$(this).attr("data-title")},{ name:'action',value:'deselection'}]);
                    $(this).contents().unwrap();
                }
                return false;
            },
            onmouseup:function(e){
            	e.stopPropagation();
            	return false;
            }
          }
    }));
	highlighter.highlightSelection("note");*/
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
		setSynonymsForWord([{name:'word', value:sel_str}]);
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
	if(buttonClicked == 'ok')
		usertext = $("#text").val();

	PF('userinput_dialog').hide();	
	$("#text").val('');
	if(synonyms != "multiple")
		$('#'+noteid).attr({'data-title':usertext, 'data-content':synonyms});
	else
		$('#'+noteid).attr({'data-content':usertext});
	
	//Tooltip creation
	if(synonyms && usertext == "")
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
	}
	if(escape_key_flag)
	{	saveTranscriptLog([{name:'word', value:sel_str},{name:'user_annotation', value:usertext},{ name:'action',value:'selection'}]);
		escape_key_flag = false;
	}
}

function saveEditing(){
	var update = document.getElementById("ted_transcript").innerHTML;
	saveTedResource([{name:'transcript',value:update}]);
}

/*function removeHighlightFromSelectedText() {
    highlighter.unhighlightSelection();
}*/
