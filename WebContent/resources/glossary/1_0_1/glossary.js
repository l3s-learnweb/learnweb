function openGlossForm(){
	$('#glossform').css('display', 'block'); 
	document.getElementById('glossform').scrollIntoView();
}

function scrollToAnchor() 
{
    jQuery('html, body').animate({
        scrollBottom: jQuery("#glossform").offset().top
    }, 2000);
}

var groupRow = function() 
{
	var columnsToMerge = 5; // number of first columns to merge
	var glossaryEntryColumn = 2; // index of column that contains the glossaryEntryId
	
	var rows = $('#glossary_table').find('tr');
	var groupStartIndex = null, rowGroupCount = 1, rowGroupGlossaryEntryId = null, groupCounter = 1;

	for(var i = 0; i < rows.length; i++) 
	{
		var row = rows.eq(i);				
		var glossaryEntryId = row.children('td').eq(glossaryEntryColumn).find("span").first().data("itemid");

		if(rowGroupGlossaryEntryId !== glossaryEntryId) 
		{
			groupCounter++;
			groupStartIndex = i;
			rowGroupGlossaryEntryId = glossaryEntryId;
			rowGroupCount = 1;
		}
		else 
		{	
			// remove the first 5 columns
			for(var k = 0; k < columnsToMerge; k++)
				row.children('td').eq(0).remove();

			rowGroupCount++;
		}

		if(groupStartIndex != null && rowGroupCount > 1)
		{
			for(var l = 0; l < columnsToMerge; l++)
			{
				var cell = rows.eq(groupStartIndex).children('td').eq(l);
				cell.attr('rowspan', rowGroupCount);
				cell.removeClass(); // remove all classes
				cell.addClass(groupCounter % 2 === 0? 'ui-datatable-even' : 'ui-datatable-odd');
			}
		}
	}
};

$(document).ready(function() {
	groupRow();
});

function setPasteStatus(element, field)
{
	var id = $(element).attr('id');
	id = id.substring(0, id.lastIndexOf(':'));
	id = id +":paste_status_"+ field;
	id = "#"+ id.replace(/:/g, '\\:'); //  need to escape : for jquery
	$(id).val('true');
}