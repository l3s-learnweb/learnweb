let activeUl;

function clearActiveUl() {
  activeUl = $('.ul-active');
  activeUl.slideUp('fast', 'easeInOutCirc');
  activeUl.removeClass('ul-active');
}

$(document).ready(() => {
  let submenu;

  $('.page-menu ul').each(function () {
    $(this).css({
      'min-width': $(this).parent().children('.breadcrumb-item').width(),
      display: 'none',
    });

    console.log($(this).parent().children('.breadcrumb-item').width());
  });

  $('a.breadcrumb-toggle').click(function (e) {
    e.preventDefault();
    e.stopPropagation();
    submenu = $(this).parent().parent().children('ul');
    if (!submenu.hasClass('ul-active')) {
      clearActiveUl();
      console.log(submenu.html());
      submenu.addClass('ul-active');
      submenu.slideDown('fast', 'easeInOutCirc');
    } else clearActiveUl();
  });

  $(window).click(() => {
    clearActiveUl();
  });
});
