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
	$('#footer').slideToggle();
	return false;
}

function footerClose()
{
	$('#footer').hide();
	return false;
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
	
	})

})  