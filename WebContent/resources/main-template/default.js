/****************************************************************
This file will be loaded on every page. 
Include only methods which are required on every page.
****************************************************************/

/**
 * Method required for the search field
 */
function removeViewstate(searchForm)
{
	$(searchForm).find("[name='javax.faces.ViewState']").remove();
}

function footerToggle()
{
	var footer = $('#footer');
	
	if(footer.css('display') == 'none')
		setPreference('helptext_hide', 'false');
	else
		setPreference('helptext_hide', 'true');
	
	footer.slideToggle();

	return false;
}

function footerClose()
{
	$('#footer').hide();
	setPreference('helptext_hide', 'true');
	return false;
}

function setPreference(prefKey, prefValue)
{
	setPreferenceRemote([{name:'key', value:prefKey}, {name:'value', value:prefValue}]);
}

$(document).ready(function()
{	
	$("#group_menu > .panelmenu").each(function( index ) 
	{
		var group = $(this);
		var links = group.children('div');
		
		group.find('h3 .iconarrow').click(function()
		{
			links.slideToggle();
			group.toggleClass('active');
			return true;
		});		
	});
})  