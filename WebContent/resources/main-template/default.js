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
	footer.slideToggle();
	
	if(footer.is(":visible"))
		setPreference('helptext_hide', 'false');
	else
		setPreference('helptext_hide', 'true');

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

$(document).ready(function(){
	
	$(".menu").click(function(){		
		$(".userbar ul li ul").css("visibility","visible");		
	});
	
	
	$(document).mouseup(function(){		
		$(".userbar ul li ul").css("visibility","hidden");	
	});
	
	$(".userbar ul li ul").mouseup(function(){		
		$(".userbar ul li ul").css("visibility","visible");
	
	});
	
	$("#group_menu > .ui-panelmenu-panel").each(function( index ) 
	{
		var group = $(this);
		var links = group.children('div');
		
		group.find('h3').click(function()
		{
			links.toggle();
			return false;
		});
		
		
	});
	

})  