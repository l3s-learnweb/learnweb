// drop-down-menu

$(document).ready(function(){
	
	$(".menu").click(function(){
		
		$(".userbar ul li ul").css("visibility","visible");
		
	});
	
	
	$(document).mousedown(function(){
		
		$(".userbar ul li ul").css("visibility","hidden");
	
	});
	
$(".userbar ul li ul").mousedown(function(){
		
		$(".userbar ul li ul").css("visibility","visible");
	
	})

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

// resourcetyp
$(document).ready(function(){
	$("#images").click(function(){
		if($("#images").hasClass("non-activeResource")==true){
			$("#images").removeClass("non-activeResource").toggleClass("activeResource");
		}
		if($("#web").hasClass("activeResource")==true){
			$("#web").removeClass("activeResource").toggleClass("non-activeResource");
		}
		if($("#more_option").hasClass("activeResource")==true){
			$("#more_option").removeClass("activeResource").toggleClass("non-activeResource");
		}
		if($("#videos").hasClass("activeResource")==true){
			$("#videos").removeClass("activeResource").toggleClass("non-activeResource")
		}
	});
	
	$("#web").click(function(){
		if($("#web").hasClass("non-activeResource")==true){
			$("#web").removeClass("non-activeResource").toggleClass("activeResource");
		}
		if($("#images").hasClass("activeResource")==true){
			$("#images").removeClass("activeResource").toggleClass("non-activeResource");
		}
		if($("#more_option").hasClass("activeResource")==true){
			$("#more_option").removeClass("activeResource").toggleClass("non-activeResource");
		}
		if($("#videos").hasClass("activeResource")==true){
			$("#videos").removeClass("activeResource").toggleClass("non-activeResource")
		}
	});
	
	$("#videos").click(function(){
		if($("#videos").hasClass("non-activeResource")==true){
			$("#videos").removeClass("non-activeResource").toggleClass("activeResource");
		}
		if($("#images").hasClass("activeResource")==true){
			$("#images").removeClass("activeResource").toggleClass("non-activeResource");
		}
		if($("#more_option").hasClass("activeResource")==true){
			$("#more_option").removeClass("activeResource").toggleClass("non-activeResource");
		}
		if($("#web").hasClass("activeResource")==true){
			$("#web").removeClass("activeResource").toggleClass("non-activeResource")
		}
	});
	$("#more_option").click(function(){
		if($("#more_option").hasClass("non-activeResource")==true){
			$("#more_option").removeClass("non-activeResource").toggleClass("activeResource");
		}
		if($("#images").hasClass("activeResource")==true){
			$("#images").removeClass("activeResource").toggleClass("non-activeResource");
		}
		if($("#videos").hasClass("activeResource")==true){
			$("#videos").removeClass("activeResource").toggleClass("non-activeResource");
		}
		if($("#web").hasClass("activeResource")==true){
			$("#web").removeClass("activeResource").toggleClass("non-activeResource")
		}
	});
})

// logs
/*
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






