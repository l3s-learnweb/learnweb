function update(desc,editor,dialog)
{		
	$(desc).html(PF(editor).jqInput[0].value.replace(/["]/g,"'"));

	PF(dialog).hide();
	return false;
}

function update_title(desc,editor,dialog,id)
{
	$(desc).html(PF(editor).jqInput[0].value.replace(/["]/g,"'"));
	$(".slide_title",$("#"+id).parent().parent()).html(PF(editor).jqInput[0].value.replace(/["]/g,"'"));
	PF(dialog).hide();
	return false;
}

