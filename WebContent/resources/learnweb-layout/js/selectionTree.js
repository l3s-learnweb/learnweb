$(document).ready(function() {
	$('.checker').each(function(index) {
		$(this).on('click', function() {
			var checked = this.checked;
			$(this).parent().find('input:checkbox').attr('checked', checked);
		});
	});
	
	$('.klapper_zu').each(function(index) {
		$(this).on('click', function() {
			$(this).toggleClass('klapper_auf');
			$(this).parent().find('ul').first().slideToggle();
		});
	});
});