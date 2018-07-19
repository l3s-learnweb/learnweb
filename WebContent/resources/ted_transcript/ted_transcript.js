//var escape_key_flag = false;
var isEditAnnotation = false;
var selectedNodeId;
var tags = {}; 

function openTab(evt, divName) {
    var i, tabcontent, tablinks;
    tabcontent = document.getElementsByClassName("overlay-content");
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = "none";
    }
    tablinks = document.getElementsByClassName("tablinks");
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace(" active", "");
    }
    document.getElementById(divName).style.display = "block";
    $(evt.currentTarget).addClass("active");
    
}

function selectTab(divName) {
	var i, tabcontent, tablinks;
    tabcontent = document.getElementsByClassName("overlay-content");
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = "none";
    }
    tablinks = document.getElementsByClassName("tablinks");
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace(" active", "");
    }
    document.getElementById(divName).style.display = "block";
    $('#' + divName + "_button").addClass("active");
}

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
	for(var i=0; i<selected_elements.length; i++) 
	{
		var selected_element = selected_elements[i];
		if(!readOnly)
		{
			selected_element.onclick = function(){
	        if (window.confirm("Delete this selection (" + $(this).text() + ")?")) {
	        	saveTranscriptLog([{name:'word', value:$(this).text()},{name:'user_annotation', value:$(this).attr("data-title")},{ name:'action',value:'deselection'}]);
	        	delete tags[$(this).attr('id')];
	        	updateTagList();
	        	$(this).contents().unwrap();
            }
			};
		}
		
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
		
		$(selected_element).on('mouseenter',function(){
			$(this).addClass('hover');
		});
		$(selected_element).on('mouseleave',function(){
			$(this).removeClass('hover');
		});	
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
	
	//To prevent selection when the mouse leaves the transcript HTML element
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
	
	//colorpicker initialization
	$(".basic").spectrum({
	    showPaletteOnly: true,
	    togglePaletteOnly: true,
	    togglePaletteMoreText: 'more',
	    togglePaletteLessText: 'less',
	    hideAfterPaletteSelect:true,
	    color: 'blanchedalmond',
	    palette: [
	        ["#000","#444","#666","#999","#ccc","#eee","#f3f3f3","#fff"],
	        ["#f00","#f90","#ff0","#0f0","#0ff","#00f","#90f","#f0f"],
	        ["#f4cccc","#fce5cd","#fff2cc","#d9ead3","#d0e0e3","#cfe2f3","#d9d2e9","#ead1dc"],
	        ["#ea9999","#f9cb9c","#ffe599","#b6d7a8","#a2c4c9","#9fc5e8","#b4a7d6","#d5a6bd"],
	        ["#e06666","#f6b26b","#ffd966","#93c47d","#76a5af","#6fa8dc","#8e7cc3","#c27ba0"],
	        ["#c00","#e69138","#f1c232","#6aa84f","#45818e","#3d85c6","#674ea7","#a64d79"],
	        ["#900","#b45f06","#bf9000","#38761d","#134f5c","#0b5394","#351c75","#741b47"],
	        ["#600","#783f04","#7f6000","#274e13","#0c343d","#073763","#20124d","#4c1130"]
	    ],
	    change: function(color) {
	        $('#'+selectedNodeId).css("background-color",color.toHexString());
	        $('.colorpicker').css("background",color.toHexString());
	    }
	});
	    
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
    $.contextMenu.types.label = function(item, opt, root) {
    	$('<div class="basic">Color: <span class="colorpicker"></span></div>')
    	.appendTo(this)
        .on('click', function() {
            	selectedNodeId = root.$trigger.attr("id");
            });
    };
    
    $.contextMenu({
    	selector: '.note',
    	events: {
    		show: function() {
    			$(this).trigger("mouseleave");
    			$('.colorpicker').css('background-color',$(this).css("background-color"));
    			$(".basic").spectrum("set", $(this).css("background-color"));
    		}
    	},
    	items: {
    		"add_annotation": {
    			name: "Add Annotation",
    			callback: function(key, options) {
    				selectedNodeId = this.attr("id");
    				PF('userinput_dialog').show();
    				this.attr('data-annotationdisabled',!(this.attr('data-annotationdisabled') == 'true'));
    				return true;
    			},
    			disabled: function(key, opt) { 
    				return this.attr('data-annotationdisabled') == 'true';
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
    				return !(this.attr('data-annotationdisabled') == 'true');
    			}
    		},
    		"delete_annotation": {
    			name: "Delete Annotation",
    			callback:function(key, options){
    				selectedNodeId = this.attr("id");
    				saveTranscriptLog([{name:'word', value:$(this).text()},{name:'user_annotation', value:$(this).attr("data-title")},{ name:'action',value:'delete annotation'}]);
    				this.removeAttr('data-title');
    				if(this.data('content'))
    					this.tooltipster('content', this.attr('data-content').replace(new RegExp('&lt;br/&gt;','g'),'<br/>'));
    				else
    				{
    					this.tooltipster('destroy');
    					this.removeAttr('title');
    				}
    				delete tags[selectedNodeId];
    				updateTagList();
    				this.attr('data-annotationdisabled',!(this.attr('data-annotationdisabled') == 'true'));
    				return true;
    			},
    			disabled: function(key, opt) { 
    				return !(this.attr('data-annotationdisabled') == 'true');
    			}
    		},
    		"display_def": {
    			name: "Add WordNet Definition",
    			callback:function(key, options){
    				selectedNodeId = this.attr("id");
    				setSynonymsForWord([{name:'word', value:this.text()}]);
    				this.attr('data-definitiondisabled',!(this.attr('data-definitiondisabled') == 'true'));
    				return true;
    			},
    			disabled: function(key, opt) {
    				return this.attr('data-definitiondisabled') == 'true'; 
    			}
    		},
    		"delete_selection": {
    			name: "Delete Selection",
    			callback:function(key, options){
    				$(this).click();
    				return true;
    			}
    		},
    		label: {type: "label", customName: "Label"}
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
	
	/*noteId++;
	
	var span = document.createElement("span");
	span.setAttribute("class", "note");
	span.setAttribute("id", noteId);
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

function getNextNode(node)
{
    if (node.firstChild)
        return node.firstChild;
    while (node)
    {
        if (node.nextSibling)
            return node.nextSibling;
        node = node.parentNode;
    }
}

function getNodesInRange(range)
{
    var start = range.startContainer;
    var end = range.endContainer;
    var commonAncestor = range.commonAncestorContainer;
    var nodes = [];
    var node;

    // walk parent nodes from start to common ancestor
    for (node = start; node; node = node.parentNode)
    {
    	if(node.nodeType == 1 && node.tagName.toLowerCase() == 'span')
    		nodes.push(node);
        if (node == commonAncestor)
            break;
    }
    nodes.reverse();

    // walk children and siblings from start until end is found
    for (node = start; node; node = getNextNode(node))
    {
    	if(node.nodeType == 1 && node.tagName.toLowerCase() == 'span')
    		nodes.push(node);
        if (node == end)
            break;
    }

    return nodes;
}

function getWindowSelectionDirection(sel){
	var position = sel.anchorNode.compareDocumentPosition(sel.focusNode);
	var backward = false;
	// position == 0 if nodes are the same
	if (!position && sel.anchorOffset > sel.focusOffset || 
	  position === Node.DOCUMENT_POSITION_PRECEDING)
	  backward = true; 
	return backward;
}

//Event handler to highlight selected text in transcript
function noteSelectedText() {
	usertext = "";
	//escape_key_flag = true;
	
	if(window.getSelection)
		sel_str = window.getSelection().toString();
	else if(document.selection && document.selection.type != "Control") //support IE browsers
		sel_str = document.selection.createRange().text;
	
	sel_str = sel_str.trim();
	if(sel_str != "")
	{
		noteId++;
		var span = document.createElement("span");
		span.setAttribute("class", "note");
		span.setAttribute("id", noteId);
		$(span).on('click',function(){
			if (window.confirm("Delete this selection (" + $(this).text() + ")?")) {
				saveTranscriptLog([{name:'word', value:$(this).text()},{name:'user_annotation', value:$(this).attr("data-title")},{ name:'action',value:'deselection'}]);
				delete tags[$(this).attr('id')];
				updateTagList();
				$(this).contents().unwrap();
				$(this).remove();
			}
		});
		$(span).on('mouseenter',function(){
			$(this).addClass('hover');
		});
		$(span).on('mouseleave',function(){
			$(this).removeClass('hover');
		});
		var sel = window.getSelection && window.getSelection();
		var transcriptElement = document.getElementById("ted_transcript");
		
		if(document.selection && !sel)
		{
			var range = document.selection.createRange();
			
			var preCaretTextRange = document.body.createTextRange();
	        preCaretTextRange.moveToElementText(transcriptElement);
	        preCaretTextRange.setEndPoint("EndToStart", range);
	        start = preCaretTextRange.text.length;
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
				
				var nodesInRange = getNodesInRange(range);
				if(nodesInRange.length>0)
				{
					for(var i =0; i< nodesInRange.length; i++)
					{
						var nodeStart = parseInt($(nodesInRange[i]).attr("data-start"));
						var nodeEnd = parseInt($(nodesInRange[i]).attr("data-end"));
						if(start < nodeStart && nodeEnd < end)
						{
							delete tags[$(nodesInRange[i]).attr('id')];
							updateTagList();
							$(nodesInRange[i]).contents().unwrap();
						}
					}
				}
				//console.log(getNodesInRange(range));
				//console.log(sel.anchorNode.parentNode);
				var backward = getWindowSelectionDirection(sel);
				if($(sel.anchorNode.parentNode).attr("id") != 'ted_transcript')
				{
					if(!backward)
					{
						start = $(sel.anchorNode.parentNode).attr("data-start");
						$(sel.anchorNode.parentNode).contents().unwrap();
					}
					else
						$(sel.anchorNode.parentNode).contents().unwrap();
				}

				if(!($(sel.focusNode.parentNode).attr("id") == 'ted_transcript'||$(sel.focusNode.parentNode).attr("id") == 'transcript'))
				{
					if(backward)
					{
						start = $(sel.focusNode.parentNode).attr("data-start");
						$(sel.focusNode.parentNode).contents().unwrap();
					}
					else
						$(sel.focusNode.parentNode).contents().unwrap();
					
				}
				//console.log(sel.focusNode.parentNode);
				
				span.setAttribute("data-start", start);
				span.setAttribute("data-end", end);

				//range.surroundContents(span);
				//var selectedTextNode = document.createTextNode(range.toString());
				//span.appendChild(selectedTextNode);
				//range.deleteContents();
				span.appendChild(range.extractContents());
				for(var i =0; i< nodesInRange.length; i++)
				{
					if(!$(nodesInRange[i]).text().trim().length)
						$(nodesInRange[i]).remove();
				}
				
				range.insertNode(span);
				range.selectNode(span);
				sel.removeAllRanges();
				sel.addRange(range);
				
				//PF('userinput_dialog').show();
			}
		}
		saveTranscriptLog([{name:'word', value:sel_str},{name:'user_annotation', value:''},{ name:'action',value:'selection'}]);
		//setSynonymsForWord([{name:'word', value:sel_str}]);
	}
	deleteSelection();
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
		selectedNode.attr('data-annotationdisabled',!(selectedNode.attr('data-annotationdisabled') == 'true'));
	
	PF('userinput_dialog').hide();	
	$("#text").val('');
	
	/*if(synonyms != "multiple")
		$('#'+noteId).attr({'data-title':usertext, 'data-content':synonyms});
	else
		$('#'+noteId).attr({'data-content':usertext});*/
	
	if(usertext != "")
	{   selectedNode.attr({'data-title':usertext});
		if(!selectedNode.hasClass('tooltipstered'))
		{
			selectedNode.tooltipster({
				functionInit: function(origin, content) {
			        	return $(this).attr('data-title');
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
		$('#'+noteId).tooltipster({
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
