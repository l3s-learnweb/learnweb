var escape_key_flag = false;
$(document).ready(function(){
	
	$(".embedded").contents().each(function(index, node) {
		if (node.nodeType == 8) {
		// node is a comment
		$(node).replaceWith(node.nodeValue);
		}
	});
	
	/*var hiddeninput = document.getElementById("transcript_form:hidden_transcript");
	var ted_transcript = hiddeninput.value;
	var div = document.getElementById("ted_transcript");
	div.innerHTML = ted_transcript;
    */
	Tipped.create('.note', function() {
        return {
          title: $(this).data('title'),
          content: $(this).data('content')
        };
      },{
    	  containment:{selector:"#ted_transcript", padding:0},
    	  maxWidth:300,
    	  size:'x-small',
    	  radius:5,
    	  position:"bottom"
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
			getUserText('ok');
	});
		
	$(document).keyup(function(event){
		var keycode = (event.keyCode ? event.keyCode : event.which);
		if(keycode == '27')
			getUserText('cancel');

	});

	//Initializing rangy highlighter
    rangy.init();
    highlighter = rangy.createHighlighter();
    //Adding the css class highlight to the rangy highlighter object
    highlighter.addClassApplier(rangy.createCssClassApplier("highlight", {
        ignoreWhiteSpace: true,
        tagNames: ["span", "a"]
    }));
});

var highlighter;
var usertext = "";
var synonyms = "";
var sel_str ="";

/*function displayTranscript(){

	var hiddeninput = document.getElementById("transcript_form:hidden_transcript");
	var ted_transcript = hiddeninput.value;
	var div = document.getElementById("ted_transcript");
	div.innerHTML = ted_transcript;
}*/

function highlightSelectedText() {
    highlighter.highlightSelection("highlight");
}

function setSynonyms(xhr,status,args){
	synonyms = "";
	synonyms = synonyms + args.synonyms;
		
	noteid++;
	highlighter.addClassApplier(rangy.createCssClassApplier("note", {
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
	highlighter.highlightSelection("note");
	PF('userinput_dialog').show();
}

function noteSelectedText() {
	usertext = "";
	escape_key_flag = true;
	
	sel = rangy.getSelection();
	sel_str = sel.toString();
	if(sel_str != "")
		setSynonymsForWord([{name:'word', value:sel_str}]);
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
	Tipped.create('#'+noteid,function() {
      return {
          title: $(this).data('title'),
          content: $(this).data('content')
        	 };
      	},{
    	  containment:{selector:"#ted_transcript", padding:0},
    	  maxWidth:300,
    	  size:'x-small',
    	  radius:5,
    	  position:"bottom"
    	}
     );
	if(escape_key_flag)
	{	saveTranscriptLog([{name:'word', value:sel_str},{name:'user_annotation', value:usertext},{ name:'action',value:'selection'}]);
		escape_key_flag = false;
	}
}

function saveEditing(){
	var update = document.getElementById("ted_transcript").innerHTML;
	saveTedResource([{name:'transcript',value:update}]);
}

function removeHighlightFromSelectedText() {
    highlighter.unhighlightSelection();
}
