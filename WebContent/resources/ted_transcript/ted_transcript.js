var escape_key_flag = false;
var isEditAnnotation = false;
var selectedNodeId;
var tags = {}; 

function openTagsDiv() {
	if($('.overlay').width() == 0)
    {
		$('.overlay').width("256px");
	    $('.embedded').width($('.embedded').width() - 256);
	    $('.right_bar').css("margin-right","256px");
    }
}

function closeTagsDiv() {
	$('.overlay').width("0px");	
    $('.embedded').width($('.embedded').width() + 256);
    $('.right_bar').css("margin-right","0px");
    $('.note').removeClass('hover');
    $('.ui-selected').removeClass("ui-selected");
}

$(document).ready(function(){
	//To include embedded TED video
	$(".embedded").contents().each(function(index, node) {
		if (node.nodeType == 8) {
		// node is a comment
		$(node).replaceWith(node.nodeValue);
		}
	});
    
	var selected_elements = document.getElementsByClassName("note");
	for(var i=0; i<selected_elements.length; i++){
		var selected_element = selected_elements[i];
		selected_element.onclick = function(){
	        if (window.confirm("Delete this selection (" + $(this).text() + ")?")) {
	        	saveTranscriptLog([{name:'word', value:$(this).text()},{name:'user_annotation', value:$(this).attr("data-title")},{ name:'action',value:'deselection'}]);
	        	delete tags[$(this).attr('id')];
	        	updateTagList();
	        	$(this).contents().unwrap();
            }
		};
		
		//Initializing tags list with already existing tags in the transcript
		if(selected_element.getAttribute("data-title") != undefined)
			tags[selected_element.id] = selected_element.getAttribute("data-title");
		
		//Initializing Tooltipster only on the elements which has a data-title or data-content
		if($(selected_element).data('title') || $(selected_element).data('content'))
		{
			$(selected_element).tooltipster({
				functionInit: function(origin, content) {
			        if($(this).data('title') && $(this).data('content'))
			        	return $(this).data('title') + '<hr/>' + $(this).data('content').replace(new RegExp('&lt;br/&gt;','g'),'<br/>');
			        else if($(this).data('content'))
			        	return $(this).data('content').replace(new RegExp('&lt;br/&gt;','g'),'<br/>');
			        else if($(this).data('title'))
			        	return $(this).data('title');
			    },
		    	contentAsHTML: true,
		    	maxWidth: 300,
		    	position:'left',
		    	theme:'tooltipster-custom-theme'
		    });	
		}
		
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

	if(!readOnly)
		initializeJQueryContextMenu();
	
	initializeResizableDiv();
	
	$( "#selectable" ).selectable({
	      stop: function() {
	        $( ".ui-selected", this ).each(function() {
	          var text = $(this).text();
	          $('[data-title="' + text + '"]').addClass("hover");
	        });
	      },
	      unselected: function(event,ui){
				var text = $(ui.unselected).text();
				$('[data-title="' + text + '"]').removeClass("hover");
			}
	});
	
	updateTagList();
	
	/*$(window).bind('beforeunload',function(){
		saveEditing();
	});*/
});

//To reset tags list on selecting a new transcript language
function clearTagList(){
	tags = {};
	updateTagList();
}

//To dynamically create/update the tags list in the 'Show Tags' pane
function updateTagList(){
	  //clear the existing list
	  $('#selectable li').remove();
	  $('.note').removeClass('hover');
	  var tagSet = new Set();
	  for(var k in tags)  
		  tagSet.add(tags[k]);
	  
	  tagSet.forEach(function(value){
		  $('#selectable').append('<li class="ui-widget-content">'+value+'</li>');
	  });	  
}

function initializeResizableDiv(){
	$(".embedded").resizable({
		  handles: "e"
	});
	
	$('.embedded').resize(function(){
	   $('.right_bar').width($("#ted_content").width() - $(".embedded").width() - 30); 
	});	
	
	$(window).resize(function(){
		if($('.overlay').width() > 0)
		   $('.right_bar').width($("#ted_content").width()-$(".embedded").width() - 30 - 256);
		else
		   $('.right_bar').width($("#ted_content").width()-$(".embedded").width() - 30);
		
		$('.right_bar').height($("#ted_content").height()); 
	});
}

function initializeJQueryContextMenu(){
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
            "edit_annotation": {
            	name: "Edit Annotation",
            	callback:function(key, options){
            		selectedNodeId = this.attr("id");
            		$("#text").val($(this).attr('data-title'));
            		isEditAnnotation = true;
            		PF('userinput_dialog').show();
            		return true;
            	},
            	disabled: function(key, opt) { 
                    return !this.data('annotationDisabled'); 
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
            "delete_selection": {
            	name: "Delete Selection",
            	callback:function(key, options){
            		$(this).click();
            		return true;
            	}
            }
        }
    });
}

function disableJQueryContextMenu()
{
	$('.note').contextMenu(false);
}

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
		saveTranscriptLog([{name:'word', value:selectedNode.text()},{name:'user_annotation', value:''},{ name:'action',value:'display definition'}]);
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
				delete tags[$(this).attr('id')];
				updateTagList();
				$(this).contents().unwrap();
				$(this).remove();
			}
		});
		
		var sel = window.getSelection && window.getSelection();
		var transcriptElement = document.getElementById("ted_transcript");
		
		if(document.selection && !sel)
		{
			var range = document.selection.createRange();
			var preCaretTextRange = document.body.createTextRange();
	        preCaretTextRange.moveToElementText(transcriptElement);
	        preCaretTextRange.setEndPoint("EndToStart", range);
	        end = preCaretTextRange.text.length;
	        preCaretTextRange.setEndPoint("EndToEnd", range);
	        end = preCaretTextRange.text.length;
			console.log("start at:" + start + ", ends at:" + end);
			span.setAttribute("data-start", start);
			span.setAttribute("data-end", end);
			span.appendChild(range.htmlText);
			range.pasteHTML(span.outerHTML);
			//PF('userinput_dialog').show();	
		}
		else
		{
			if(sel.rangeCount > 0)
			{
				var range = sel.getRangeAt(0);
				var preCaretRange = range.cloneRange();
	            preCaretRange.selectNodeContents(transcriptElement);
	            preCaretRange.setEnd(range.startContainer, range.startOffset);
	            start = preCaretRange.toString().length;
	            preCaretRange.setEnd(range.endContainer, range.endOffset);
	            end = preCaretRange.toString().length;
				console.log("starts at:" + start + ", ends at:" + end);
				span.setAttribute("data-start", start);
				span.setAttribute("data-end", end);
				span.appendChild(range.extractContents());
				range.insertNode(span);
				//PF('userinput_dialog').show();
			}
		}
		saveTranscriptLog([{name:'word', value:sel_str},{name:'user_annotation', value:''},{ name:'action',value:'selection'}]);
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
		else if(selectedNode.data('content'))
		{
			selectedNode.tooltipster('content',selectedNode.attr('data-title') + '<hr/>' + selectedNode.attr('data-content').replace(new RegExp('&lt;br/&gt;','g'),'<br/>'));
		}
		else
			selectedNode.tooltipster('content', selectedNode.attr('data-title'));
		
		selectedNode.trigger("mouseenter");
		tags[selectedNodeId] = usertext;
		updateTagList();
		if(!isEditAnnotation)
			saveTranscriptLog([{name:'word', value:selectedNode.text()},{name:'user_annotation', value:usertext},{ name:'action',value:'add annotation'}]);
		else
			saveTranscriptLog([{name:'word', value:selectedNode.text()},{name:'user_annotation', value:usertext},{ name:'action',value:'edit annotation'}]);
		
		isEditAnnotation = false;
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

function submitTranscript(){
	disableJQueryContextMenu();
	var update = document.getElementById("ted_transcript").innerHTML;
	submitTedResource([{name:'transcript',value:update}]);
}
