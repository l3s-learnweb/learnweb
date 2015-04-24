function update(desc,editor,dialog)
{		
	$(desc).text(PF(editor).jqInput[0].value.replace(/["]/g,"'"));

	PF(dialog).hide();
	return false;
}

function update_title(desc,editor,dialog,id)
{
	$(desc).text(PF(editor).jqInput[0].value.replace(/["]/g,"'"));
	$(".slide_title",$("#"+id).parent().parent()).text(PF(editor).jqInput[0].value.replace(/["]/g,"'"));
	PF(dialog).hide();
	return false;
}

