/** @external saveTranscriptLog */
/** @external submitTedResource */
/** @external saveTedResource */
/** @external setSynonymsForWord */

//var escape_key_flag = false;
var isEditAnnotation = false;
var selectedNodeId;
var tags = {};

function saveTranscriptAction(word, annotation, action) {
    saveTranscriptLog([
        {name: 'word', value: word},
        {name: 'user_annotation', value: annotation},
        {name: 'action', value: action}
    ]);
}

/*function openTab(evt, divName) {
    var i, tabcontent, tablinks;
    tabcontent = document.getElementsByClassName('overlay-content');
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = 'none';
    }
    tablinks = document.getElementsByClassName('tablinks');
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace('ui-state-active', "");
    }
    document.getElementById(divName).style.display = 'block';
    $(evt.currentTarget).addClass('ui-state-active');
    
}*/
/*
function selectTab(divName) {
	var i, tabcontent, tablinks;
    tabcontent = document.getElementsByClassName('overlay-content');
    for (i = 0; i < tabcontent.length; i++) {
        tabcontent[i].style.display = 'none';
    }
    tablinks = document.getElementsByClassName('tablinks');
    for (i = 0; i < tablinks.length; i++) {
        tablinks[i].className = tablinks[i].className.replace('ui-state-active', "");
    }
    document.getElementById(divName).style.display = 'block';
	document.getElementById('test123:' + divName + '_button').classList.add('ui-state-active');
}*/

/*function openTagsDiv() {
	if($('.overlay').width() === 0)
    {
		$('.overlay').width('256px');
/!*	    $('.embedded').width($('.embedded').width() - 256);
	    $('.right_bar').css('margin-right','256px');*!/
    }
}

function closeTagsDiv() {
	$('.overlay').width('0px');
/!*    $('.embedded').width($('.embedded').width() + 256);
    $('.right_bar').css('margin-right','0px');*!/
    $('.note').removeClass('ui-state-hover');
    $('.ui-selected').removeClass('ui-selected');
}*/

$(function () {
    //To include embedded TED video
    $('.embed-responsive').contents().each(function (index, node) {
        if (node.nodeType === 8) {
            // node is a comment
            $(node).replaceWith(node.nodeValue);
        }
    });

    var selected_elements = document.getElementsByClassName('note');
    for (var i = 0; i < selected_elements.length; i++) {
        var selected_element = selected_elements[i];
        if (!readOnly) {
            selected_element.onclick = function () {
                if (window.confirm('Delete this selection (' + $(this).text() + ')?')) {
                    saveTranscriptAction($(this).text(), $(this).data('title'), 'deselection');
                    delete tags[$(this).attr('id')];
                    updateTagList();
                    $(this).contents().unwrap();
                }
            };
        }

        //Initializing tags list with already existing tags in the transcript
        if (selected_element.getAttribute('data-title'))
            tags[selected_element.id] = selected_element.getAttribute('data-title');

        //Initializing Tooltipster only on the elements which has a data-title or data-content
        if ($(selected_element).data('title') || $(selected_element).data('content')) {
            $(selected_element).tooltipster({
                functionInit: function (origin, content) {
                    var $this = $(this);
                    if ($this.data('title') && $this.data('content'))
                        return $this.data('title') + '<hr/>' + $this.data('content').replace(new RegExp('&lt;br/&gt;', 'g'), '<br/>');
                    else if ($this.data('content'))
                        return $this.data('content').replace(new RegExp('&lt;br/&gt;', 'g'), '<br/>');
                    else if ($this.data('title'))
                        return $this.data('title');
                },
                contentAsHTML: true,
                maxWidth: 300,
                position: 'left',
                theme: 'tooltipster-custom-theme'
            });
        }

        $(selected_element).on('mouseenter', function () {
            $(this).addClass('ui-state-hover');
        });
        $(selected_element).on('mouseleave', function () {
            $(this).removeClass('ui-state-hover');
        });
    }

    $('#text').on('keypress', function (event) {
        var keyCode = (event.keyCode ? event.keyCode : event.which);
        if (keyCode === 13) {
            $('#userinput_ok').trigger('click');
            return false;
        }
    });

    $(document).on('keyup', function (event) {
        var keyCode = (event.keyCode ? event.keyCode : event.which);
        if (keyCode === 27) {
            $('#userinput_cancel').trigger('click');
            return false;
        }
    });

    // To prevent selection when the mouse leaves the transcript HTML element
    $('.embed-responsive').on('mousedown', function () {
        return false;
    });

    $('.transcript-container').on('mouseleave', function () {
        deleteSelection();
    });

    if (!readOnly)
        initializeJQueryContextMenu();

    //initializeResizableDiv();
    $('#selectable').selectable({
        stop: function () {
            $('.ui-selected', this).each(function () {
                var text = $(this).text();
                $('[data-title="' + text + '"]').addClass('ui-state-hover');
            });
        },
        unselected: function (event, ui) {
            var text = $(ui.unselected).text();
            $('[data-title="' + text + '"]').removeClass('ui-state-hover');
        }
    });

    updateTagList();

    // color picker initialization
    $('.color-picker-field').spectrum({
        showPaletteOnly: true,
        togglePaletteOnly: true,
        togglePaletteMoreText: 'more',
        togglePaletteLessText: 'less',
        hideAfterPaletteSelect: true,
        color: 'blanchedalmond',
        palette: [
            ['#000', '#444', '#666', '#999', '#ccc', '#eee', '#f3f3f3', '#fff'],
            ['#f00', '#f90', '#ff0', '#0f0', '#0ff', '#00f', '#90f', '#f0f'],
            ['#f4cccc', '#fce5cd', '#fff2cc', '#d9ead3', '#d0e0e3', '#cfe2f3', '#d9d2e9', '#ead1dc'],
            ['#ea9999', '#f9cb9c', '#ffe599', '#b6d7a8', '#a2c4c9', '#9fc5e8', '#b4a7d6', '#d5a6bd'],
            ['#e06666', '#f6b26b', '#ffd966', '#93c47d', '#76a5af', '#6fa8dc', '#8e7cc3', '#c27ba0'],
            ['#c00', '#e69138', '#f1c232', '#6aa84f', '#45818e', '#3d85c6', '#674ea7', '#a64d79'],
            ['#900', '#b45f06', '#bf9000', '#38761d', '#134f5c', '#0b5394', '#351c75', '#741b47'],
            ['#600', '#783f04', '#7f6000', '#274e13', '#0c343d', '#073763', '#20124d', '#4c1130']
        ],
        change: function (color) {
            $('#' + selectedNodeId).css('background-color', color.toHexString());
            $('.color-picker').css('background', color.toHexString());
        }
    });

    /*$(window).on('beforeunload',function(){
        saveEditing();
    });*/
});

//To reset tags list on selecting a new transcript language
function clearTagList() {
    tags = {};
    updateTagList();
}

//To dynamically create/update the tags list in the 'Show Tags' pane
function updateTagList() {
    $('#selectable li').remove();
    $('.note').removeClass('ui-state-hover');
    var tagSet = new Set();
    for (var k in tags)
        tagSet.add(tags[k]);

    tagSet.forEach(function (value) {
        $('#selectable').append('<li class="ui-widget-content">' + value + '</li>');
    });
}

function initializeResizableDiv() {
    var $embedded = $('.embedded');
    var $rightBar = $('.right_bar');
    var $tedContent = $('#ted_content');

    $embedded.resizable({
        handles: 'e'
    });

    $embedded.on('resize', function () {
        $rightBar.width($tedContent.width() - $embedded.width() - 30);
    });

    $(window).on('resize', function () {
        $rightBar.width($tedContent.width() - $embedded.width() - 30 - ($('.overlay').width() > 0 ? 256 : 0));
        $rightBar.height($tedContent.height());
    });
}

function initializeJQueryContextMenu() {
    $.contextMenu.types.label = function (item, opt, root) {
        $('<div class="color-picker-field">Color: <span class="color-picker"></span></div>')
            .appendTo(this)
            .on('click', function () {
                selectedNodeId = root.$trigger.attr('id');
            });
    };

    $.contextMenu({
        selector: '.note',
        events: {
            show: function (e) {
                e.$trigger.removeClass('ui-state-hover');
                var backgroundColor = e.$trigger.css('background-color');

                $('.color-picker').css('background-color', backgroundColor);
                $('.color-picker-field').spectrum('set', backgroundColor);
            }
        },
        items: {
            'add_annotation': {
                name: 'Add Annotation',
                callback: function () {
                    selectedNodeId = this.attr('id');
                    PF('userinput_dialog').show();
                    this.data('annotationdisabled', true);
                    return true;
                },
                disabled: function () {
                    return this.data('annotationdisabled') === true;
                }
            },
            'edit_annotation': {
                name: 'Edit Annotation',
                callback: function () {
                    selectedNodeId = this.attr('id');
                    $('#text').val($(this).data('title'));
                    isEditAnnotation = true;
                    PF('userinput_dialog').show();
                    return true;
                },
                disabled: function () {
                    return this.data('annotationdisabled') !== true;
                }
            },
            'delete_annotation': {
                name: 'Delete Annotation',
                callback: function () {
                    selectedNodeId = this.attr('id');
                    saveTranscriptAction($(this).text(), $(this).data('title'), 'delete annotation');
                    this.removeAttr('data-title');
                    if (this.data('content'))
                        this.tooltipster('content', this.data('content').replace(new RegExp('&lt;br/&gt;', 'g'), '<br/>'));
                    else {
                        this.tooltipster('destroy');
                        this.removeAttr('title');
                    }
                    delete tags[selectedNodeId];
                    updateTagList();
                    this.data('annotationdisabled', false);
                    return true;
                },
                disabled: function () {
                    return this.data('annotationdisabled') !== true;
                }
            },
            'display_def': {
                name: 'Add WordNet Definition',
                callback: function () {
                    selectedNodeId = this.attr('id');
                    setSynonymsForWord([{name: 'word', value: this.text()}]);
                    this.data('definitiondisabled', this.data('definitiondisabled') !== true);
                    return true;
                },
                disabled: function () {
                    return this.data('definitiondisabled') === true;
                }
            },
            'delete_selection': {
                name: 'Delete Selection',
                callback: function () {
                    $(this).trigger('click');
                    return true;
                }
            },
            label: {type: 'label', customName: 'Label'}
        }
    });
}

function disableJQueryContextMenu() {
    $('.note').contextMenu(false);
}

var usertext = "";
//var synonyms = "";
var sel_str = "";

function setSynonyms(xhr, status, args) {
    var synonyms = "";
    synonyms = synonyms + args.synonyms;
    var selectedNode = $('#' + selectedNodeId);
    if (synonyms !== 'multiple') {
        selectedNode.data('content', synonyms);
        if (!selectedNode.hasClass('tooltipstered')) {
            selectedNode.tooltipster({
                functionInit: function (origin, content) {
                    return $(this).data('content').replace(new RegExp('&lt;br/&gt;', 'g'), '<br/>');
                },
                contentAsHTML: true,
                maxWidth: 300,
                position: 'right',
                theme: 'tooltipster-custom-theme'
            });
        } else {
            selectedNode.tooltipster('content', selectedNode.data('title') + '<hr/>' + selectedNode.data('content').replace(new RegExp('&lt;br/&gt;', 'g'), '<br/>'));
        }
        selectedNode.trigger('mouseenter');
        saveTranscriptAction(selectedNode.text(), '', 'display definition');
    }

    /*noteId++;

    var span = document.createElement('span');
    span.setAttribute('class', 'note');
    span.setAttribute('id', noteId);
    $(span).on('click',function(){
        if (window.confirm('Delete this selection (' + $(this).text() + ')?')) {
            saveTranscriptAction($(this).text(), $(this).data('title'), 'deselection');
            $(this).contents().unwrap();
            $(this).remove();
        }
    });
    var sel = window.getSelection && window.getSelection();
    if(sel.rangeCount > 0)
    {
        var range = sel.getRangeAt(0);
        span.appendChild(range.extractContents());
        range.insertNode(span);
        PF('userinput_dialog').show();
    }*/

}

function getNextNode(node) {
    if (node.firstChild)
        return node.firstChild;
    while (node) {
        if (node.nextSibling)
            return node.nextSibling;
        node = node.parentNode;
    }
}

function getNodesInRange(range) {
    var start = range.startContainer;
    var end = range.endContainer;
    var commonAncestor = range.commonAncestorContainer;
    var nodes = [];
    var node;

    // walk parent nodes from start to common ancestor
    for (node = start; node; node = node.parentNode) {
        if (node.nodeType === 1 && node.tagName.toLowerCase() === 'span')
            nodes.push(node);
        if (node === commonAncestor)
            break;
    }
    nodes.reverse();

    // walk children and siblings from start until end is found
    for (node = start; node; node = getNextNode(node)) {
        if (node.nodeType === 1 && node.tagName.toLowerCase() === 'span')
            nodes.push(node);
        if (node === end)
            break;
    }

    return nodes;
}

function getWindowSelectionDirection(sel) {
    var position = sel.anchorNode.compareDocumentPosition(sel.focusNode);
    var backward = false;
    // position == 0 if nodes are the same
    if (!position && sel.anchorOffset > sel.focusOffset ||
        position === Node.DOCUMENT_POSITION_PRECEDING)
        backward = true;
    return backward;
}

// Event handler to highlight selected text in transcript
function noteSelectedText() {
    usertext = "";
    //escape_key_flag = true;

    sel_str = window.getSelection().toString();
    sel_str = sel_str.trim();

    if (sel_str !== "") {
        noteId++;

        var span = document.createElement('span');
        span.setAttribute('class', 'note');
        span.setAttribute('id', 'note' + noteId);

        $(span).on('click', function () {
            var $this = $(this);
            if (window.confirm('Delete this selection (' + $this.text() + ')?')) {
                saveTranscriptAction($this.text(), $this.data('title'), 'deselection');
                delete tags[$this.attr('id')];
                updateTagList();
                $this.contents().unwrap();
                $this.remove();
            }
        }).on('mouseenter', function () {
            $(this).addClass('ui-state-hover');
        }).on('mouseleave', function () {
            $(this).removeClass('ui-state-hover');
        });

        var sel = window.getSelection && window.getSelection();
        var transcriptElement = document.getElementById('ted_transcript');

        if (sel.rangeCount > 0) {
            var range = sel.getRangeAt(0);
            var preCaretRange = range.cloneRange();
            preCaretRange.selectNodeContents(transcriptElement);
            preCaretRange.setEnd(range.startContainer, range.startOffset);
            var start = preCaretRange.toString().length;
            preCaretRange.setEnd(range.endContainer, range.endOffset);
            var end = preCaretRange.toString().length;
            console.log('starts at:' + start + ', ends at:' + end);

            var nodesInRange = getNodesInRange(range);
            if (nodesInRange.length > 0) {
                for (var i = 0; i < nodesInRange.length; i++) {
                    var nodeStart = parseInt($(nodesInRange[i]).data('start'));
                    var nodeEnd = parseInt($(nodesInRange[i]).data('end'));
                    if (start < nodeStart && nodeEnd < end) {
                        delete tags[$(nodesInRange[i]).attr('id')];
                        updateTagList();
                        $(nodesInRange[i]).contents().unwrap();
                    }
                }
            }
            //console.log(getNodesInRange(range));
            //console.log(sel.anchorNode.parentNode);
            var backward = getWindowSelectionDirection(sel);
            if ($(sel.anchorNode.parentNode).attr('id') !== 'ted_transcript') {
                if (!backward) {
                    start = $(sel.anchorNode.parentNode).data('start');
                    $(sel.anchorNode.parentNode).contents().unwrap();
                } else
                    $(sel.anchorNode.parentNode).contents().unwrap();
            }

            if (!($(sel.focusNode.parentNode).attr('id') === 'ted_transcript' || $(sel.focusNode.parentNode).attr('id') === 'transcript')) {
                if (backward) {
                    start = $(sel.focusNode.parentNode).data('start');
                    $(sel.focusNode.parentNode).contents().unwrap();
                } else
                    $(sel.focusNode.parentNode).contents().unwrap();

            }
            //console.log(sel.focusNode.parentNode);

            span.setAttribute('data-start', start);
            span.setAttribute('data-end', end);

            //range.surroundContents(span);
            //var selectedTextNode = document.createTextNode(range.toString());
            //span.appendChild(selectedTextNode);
            //range.deleteContents();
            span.appendChild(range.extractContents());
            for (var i = 0; i < nodesInRange.length; i++) {
                if (!$(nodesInRange[i]).text().trim().length)
                    $(nodesInRange[i]).remove();
            }

            range.insertNode(span);
            range.selectNode(span);
            sel.removeAllRanges();
            sel.addRange(range);

            //PF('userinput_dialog').show();
        }
        saveTranscriptAction(sel_str, '', 'selection')
        //setSynonymsForWord([{name:'word', value:sel_str}]);
    }
    deleteSelection();
}

function deleteSelection() {
    if (window.getSelection().removeAllRanges) {
        window.getSelection().removeAllRanges();
    }
}

function getUserText(buttonClicked) {
    var selectedNode = $('#' + selectedNodeId);
    if (buttonClicked === 'ok')
        usertext = $('#text').val();
    else
        selectedNode.data('annotationdisabled', selectedNode.data('annotationdisabled') !== true);

    PF('userinput_dialog').hide();
    $('#text').val('');

    /*if(synonyms != 'multiple')
        $('#'+noteId).attr({'data-title':usertext, 'data-content':synonyms});
    else
        $('#'+noteId).attr({'data-content':usertext});*/

    if (usertext !== "") {
        selectedNode.data('title', usertext);
        if (!selectedNode.hasClass('tooltipstered')) {
            selectedNode.tooltipster({
                functionInit: function (origin, content) {
                    return $(this).data('title');
                },
                contentAsHTML: true,
                maxWidth: 300,
                position: 'right',
                theme: 'tooltipster-custom-theme'
            });
        } else if (selectedNode.data('content')) {
            selectedNode.tooltipster('content', selectedNode.data('title') + '<hr/>' + selectedNode.data('content').replace(new RegExp('&lt;br/&gt;', 'g'), '<br/>'));
        } else
            selectedNode.tooltipster('content', selectedNode.data('title'));

        selectedNode.trigger('mouseenter');
        tags[selectedNodeId] = usertext;
        updateTagList();
        saveTranscriptAction(selectedNode.text(), usertext, isEditAnnotation ? 'edit annotation' : 'add annotation');

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
    {	saveTranscriptAction(sel_str, usertext, 'selection');
        escape_key_flag = false;
    }*/
}

function saveEditing() {
    var update = document.getElementById('ted_transcript').innerHTML;
    saveTedResource([{name: 'transcript', value: update}]);
}

function submitTranscript() {
    disableJQueryContextMenu();
    var update = document.getElementById('ted_transcript').innerHTML;
    submitTedResource([{name: 'transcript', value: update}]);
}
