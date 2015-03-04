// drop-down-menu

$(document).ready(function(){
	
	$(".menubutton").click(function(){

		$(".userbar").slideToggle(1000, function(){
			$(".userbar").toggleClass("userbar-expanded").css('display', '');
		});
		
	});

})  


// flags

$(document).ready(function(){
	$("#german").click(function(){
		$("#germanbutton").removeClass("non-active").toggleClass("active");
		$("#englishbutton").removeClass("active").toggleClass("non-active");
	})
	
	$("#english").click(function(){
		$("#englishbutton").removeClass("non-active").toggleClass("active");
		$("#germanbutton").removeClass("active").toggleClass("non-active");
	})
})


// logs

$(document).ready(function(){
	
	$("#loginbutton").click(function(){
		$("#logoutbutton").css("display","block");
		$("#loginbutton").css("display","none");
		
		window.onresize=function(){
			var _width=$(window).width();	
			if($("#logoutbutton").css("display","block") && _width<625){
				$("#logoutbutton").css("display","none");
				$("#logout").css("display","block");
				$("#login").css("display","none");
			}
			if(_width>=625){
				$("#logoutbutton").css("display","block");
				$("#logout").css("display","none");
			}
		}
		
	})
	
	$("#logoutbutton").click(function(){
		$("#loginbutton").css("display","block");
		$("#logoutbutton").css("display","none");
		
		window.onresize=function(){
			var _width=$(window).width();
			if($("#loginbutton").css("display","block") && _width<625){
				$("#loginbutton").css("display","none");
				$("#login").css("display","block");
				$("#logout").css("display","none");
			}
			if(_width>=625){
				$("#loginbutton").css("display","block");
				$("#login").css("display","none");
			}
		}
	})
	
})


$(document).ready(function(){
	
	$("#logout").click(function(){
		$("#logout").css("display","none");
		$("#login").css("display","block");
		
		window.onresize=function(){
			var _width=$(window).width();
			if( _width>=625){
				$("#login").css("display","none");
				$("#loginbutton").css("display","block");
			}
			if(_width<625){
				$("#login").css("display","block");
				$("#loginbutton").css("display","none");
			}
		}
	})
	
	$("#login").click(function(){
		$("#login").css("display","none");
		$("#logout").css("display","block");
		$("#loginbutton").css("display","none");
			
		window.onresize=function(){
			var _width=$(window).width();
			if( _width>=625){
				$("#logout").css("display","none");
				$("#logoutbutton").css("display","block");
			}
			if(_width<625){
				$("#logout").css("display","block");
				$("#logoutbutton").css("display","none");
			}
		}
	})
	
})

/*$(document).ready(function(){
	window.onresize=function(){
		var _width=$(window).width();
			if($("#logoutbutton").css("display","block") && _width<=880){
				$(".language ul li ul").css("position","absolute");
				$(".language ul li ul").css("right","3.5em");
			}
			
		}
})*/

/*$(document).ready(function(){
	$("#loginbutton").click(function(){
		$("#logoutbutton").css("display","block");
		$(".userbar ul li ul").css("right","4.5em");
	})
})*/






