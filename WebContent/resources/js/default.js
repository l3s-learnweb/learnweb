/****************************************************************
This file will be loaded on every page. 
Include only methods which are required on every page.
****************************************************************/

/**
 * Method required for the search field
 */
function removeViewstate()
{
	$('#search_form input[type=hidden]').remove();
	return true;
}

function storeWindowSize() {
	document.getElementById('search_form:screenHeight').value = $(window).height();
	document.getElementById('search_form:screenWidth').value = $(window).width();
}		

$(document).ready(function() {
	storeWindowSize();
	//Shadowbox.init();
});

function footer_toggle(){
	$('.footer').slideToggle();
}

function footer_close(){
	$('.footer').hide();
}
/*
 * Copied from new_index.xhtml
 */

$(document).ready(function(){
    $('#header_form').live('click', function(event) {        
         $('#courselist').toggle('show');
         $('#headerbar').toggle('hide');
    });
    
    $('#backtomain').live('click', function(event) {        
         $('#courselist').toggle('hide');
         $('#headerbar').toggle('show');
    });
});	