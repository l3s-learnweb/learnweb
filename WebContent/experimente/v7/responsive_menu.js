// drop-down-menu

$(document).ready(function(){
	
	$(".menubutton").click(function(){

		$(".userbar").slideToggle(820, function(){
			$(".userbar").toggleClass("userbar-expanded").css('display', '');
		});
		
		$("#sub_menu_bar").slideToggle(820, function(){
			$("#sub_menu_bar").toggleClass("sub_menu_bar-expanded").css("display", "");
			$(".sub_menu_bar-expanded").css("position", "relative");
			$(".sub_menu_bar-expanded").css("top", "215px");
		});
		$("#sub_menu_bar").css("top", "50px");
	});

})  


$(document).ready(function(){
	$("#mystuff").click(function(){
		$("#stuffmenu").slideToggle(820);
	});
	$("#setting").click(function(){
		$("#settingmenu").slideToggle(820);
	});
	$("#searchhistory").click(function(){	
		$("#searchhistorymenu").slideToggle(820);
	});
	$("#explorehistory").click(function(){	
		$("#explorehistorymenu").slideToggle(820);
	});
}) 

//scroll

var lasty=0;
	
$(document).ready(function(){
	
	$("#fixedbar").show();
	$("#sub_menu_bar").show();
	$("#sub_menu_bar").css("position","relative");
    $("#sub_menu_bar").css("top", "50px");

    var hidden = false;
 $(window).bind( 'scroll', function() {

	 $("#fixedbar").show(); 
	
    var myPosY = $(window).scrollTop();
    

    if (myPosY < lasty && $(window).width()>820 ) {

		//if (hidden) {
    			//$("#sub_menu_bar").hide()
				//$("#sub_menu_bar").slideDown(750);
				$("#sub_menu_bar").css('top', (myPosY + 50) + 'px');
				$("#sub_menu_bar").show();
			//}
		hidden = false;
		}
    else
    	hidden = true;
    
   
    if(myPosY<lasty && $("sub_menu_bar-expanded").css("display", "") && $(window).width()>820){
		$(".sub_menu_bar-expanded").css("top", (myPosY+215)+"px");
    }
    
    lasty=myPosY;
    
    });
   
})


// flags

$(document).ready(function(){
	$("#german").click(function(){
		$("#germanbutton").css("display","block");
		$("#englishbutton").css("display","none");
	})
	
	$("#english").click(function(){
		$("#englishbutton").css("display","block");
		$("#germanbutton").css("display","none");
	})
})

















