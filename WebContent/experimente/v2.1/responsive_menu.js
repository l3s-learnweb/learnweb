// drop-down-menu

$(document).ready(function(){
	
	$(".menubutton").click(function(){

		$(".userbar").slideToggle(820, function(){
			$(".userbar").toggleClass("userbar-expanded").css('display', '');
		});
		
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

// logs

$(document).ready(function(){
	$("#loginbutton").click(function(){
		$("#logoutbutton").css("display","block");
		$("#loginbutton").css("display","none");
	})
	$("#logoutbutton").click(function(){
		$("#loginbutton").css("display","block");
		$("#logoutbutton").css("display","none");
	})
})












