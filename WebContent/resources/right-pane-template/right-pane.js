/**
 * Method required to open archive versions in a new tab 
 */
function archive_url_select(){
	if(PF('archive_versions_menu').getSelectedLabel() != "Choose an archived version")
		window.open(PF('archive_versions_menu').getSelectedValue(),'_blank');
}