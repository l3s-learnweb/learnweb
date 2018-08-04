$(document).on('ready', function() {
	$(".added_res_slider").slick({
		dots : true,
		infinite : true,
		arrows : true,
		slidesToShow : 3,
		slidesToScroll : 1,
		focusOnSelect : true

	});

	$(".added_res_slider").on('afterChange', function(event, slick, currentSlide) {
		displayResource(currentSlide, 'added');
	});

	$(".updated_res_slider").slick({
		dots : true,
		infinite : true,
		arrows : true,
		slidesToShow : 3,
		slidesToScroll : 1,
		focusOnSelect : true

	});

	$(".updated_res_slider").on('afterChange', function(event, slick, currentSlide) {
		displayResource(currentSlide, 'updated');
	});
	initPanel();
});

function displayResource(index, type) {
	onAfterChange([ {
		name : 'index',
		value : index
	}, {
		name : 'type',
		value : type
	} ]);
}

$(".dropdown-btn")
		.each(
				function() {
					$(this)
							.click(
									function() {
										$(this).find(".fa-caret-down")
												.toggleClass("open");
										var nextElement = $(this).next();
										if (nextElement.hasClass('open')) {
											$(this).removeClass('active');
											nextElement.removeClass('open');
											nextElement.height(0);
										} else {
											if ($(this).attr('id') === 'added_res_tab_new_changes') {
												var currentIndex = nextElement
														.find('.slick-current')
														.attr(
																'data-slick-index');
												displayResource(currentIndex,
														'added');
												closeTheOtherSlider($('#added_res_tab'));
											}
											if ($(this).attr('id') === 'added_res_tab') {
												var currentIndex = nextElement
														.find('.slick-current')
														.attr(
																'data-slick-index');
												displayResource(currentIndex,
														'updated');
												closeTheOtherSlider($('#added_res_tab_new_changes'))
											}

											$(this).addClass('active');
											$(this).next().addClass('open');
											if (nextElement.height() === 0) {
												nextElement
														.height(nextElement
																.find(
																		'div:first-child')
																.outerHeight(
																		true));
											}
										}
									})
				})

function initPanel() {
	$(".dropdown-btn").each(
			function() {
				$(this).find(".fa-caret-down").toggleClass("open");
				var nextElement = $(this).next();
				if (nextElement.hasClass('open')) {
					$(this).removeClass('active');
					nextElement.removeClass('open');
					nextElement.height(0);
				} else {
					if ($(this).attr('id') === 'added_res_tab_new_changes') {
						var currentIndex = nextElement.find('.slick-current')
								.attr('data-slick-index');
						displayResource(currentIndex, 'added');
					}
					if ($(this).attr('id') === 'added_res_tab') {
						var currentIndex = nextElement.find('.slick-current')
								.attr('data-slick-index');
						displayResource(currentIndex, 'updated');
					}
					$(this).addClass('active');
					$(this).next().addClass('open');
					if (nextElement.height() === 0) {
						nextElement.height(nextElement.find('div:first-child')
								.outerHeight(true));
					}
				}
			})

}

function closeTheOtherSlider(tabName) {
	if (tabName.next().hasClass('open')) {
		tabName.removeClass('active');
		tabName.find(".fa-caret-down").toggleClass("open", true);
		tabName.next().removeClass('open');
		tabName.next().height(0);
	}
}

$('#show_activities').click(function(){
	if($(this).hasClass('closed_activities')){
		$(this).removeClass('closed_activities');
		$('#all_activities').removeClass('hidden').addClass('blocked');
		$(this).find('i').addClass('fa-angle-double-down').removeClass('fa-angle-double-up');
		$(this).find('span').text("Hide activities");
	}else{
		$(this).addClass('closed_activities');
		$('#all_activities').addClass('hidden').removeClass('blocked');
		$(this).find('i').addClass('fa-angle-double-up').removeClass('fa-angle-double-down');
		$(this).find('span').text("Show activities");
	}
});

