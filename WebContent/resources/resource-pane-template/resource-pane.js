/**
 * Method required to open archive versions in a new tab 

function archive_url_select(){
	if(PF('archive_versions_menu').getSelectedValue() != 0)
		window.open(PF('archive_versions_menu').getSelectedValue(),'_blank');
} */

function open_timeline_view(){
	$('#timeline_view').slideToggle("slow",function(){scroller();});
	return false;
}

function open_list_view(){
	$('#archive_list_view').slideToggle("slow",function(){scroller();});
	return false;
}