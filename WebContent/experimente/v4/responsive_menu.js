// drop-down-menu

$(document).ready(function(){
	
	$(".menubutton").click(function(){

		$(".userbar").slideToggle(820, function(){
			$(".userbar").toggleClass("userbar-expanded").css('display', '');
		});
		
		$("#secondbar").slideToggle(820, function(){
			$("#secondbar").toggleClass("secondbar-expanded").css("display", "");
			$(".secondbar-expanded").css("position", "relative");
			$(".secondbar-expanded").css("top", "215px");
		});
		$("#secondbar").css("top", "50px");
	});

})  

	

//scroll

var lasty=0;
	
$(document).ready(function(){
	
	$("#fixedbar").show();
	$("#secondbar").show();
	$("#secondbar").css("position","relative");
    $("#secondbar").css("top", "50px");

    var hidden = false;
 $(window).bind( 'scroll', function() {

	 $("#fixedbar").show(); 
	
    var myPosY = $(window).scrollTop();
    

    if (myPosY < lasty && $(window).width()>820 ) {

		//if (hidden) {
    		//$("#secondbar").hide()
			//	$("#secondbar").slideDown(750);
				$("#secondbar").css('top', (myPosY + 50) + 'px');
				$("#secondbar").show();
			//}
		hidden = false;
		}
    else
    	hidden = true;
    
   
    if(myPosY<lasty && $("secondbar-expanded").css("display", "") && $(window).width()>820){
		$(".secondbar-expanded").css("top", (myPosY+215)+"px");
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

















