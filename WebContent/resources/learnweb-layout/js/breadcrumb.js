$(() => {
  function hideBreadcrumbMenu(e) {
    if (e && $(e.target).parents('.breadcrumb-toggle').length) return; // prevent auto-closing when clicked on toggle
    $('.breadcrumb .ul-active').slideUp('fast', 'easeInOutCirc').removeClass('ul-active');
  }

  function showBreadcrumbMenu($bcPageMenu) {
    $bcPageMenu.addClass('ul-active').slideDown('fast', 'easeInOutCirc');
    $(document).one('mousedown', hideBreadcrumbMenu); // add event to hide menu when clicked outside
  }

  $('.breadcrumb .page-menu ul').each(function () {
    $(this).css({
      'min-width': $(this).prev('.breadcrumb-item').width(),
      display: 'none', // this better to do in css
    });
  });

  $('.breadcrumb .breadcrumb-toggle').on('click', function (e) {
    const submenu = $(this).closest('.page-menu').children('ul');
    const isActive = submenu.hasClass('ul-active');
    hideBreadcrumbMenu(); // close all existing menu
    if (!isActive) showBreadcrumbMenu(submenu); // open new menu if it was not open before
    return false;
  });
});
