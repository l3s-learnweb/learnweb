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


function footer_toggle(){
	$('.footer').slideToggle();
}

function footer_close(){
	$('.footer').hide();
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