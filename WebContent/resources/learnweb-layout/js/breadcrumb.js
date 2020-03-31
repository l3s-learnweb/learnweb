$(() => {
  function hideBreadcrumbMenu(e) {
    if (e && $(e.target).parents('.breadcrumb-menu').length) return; // prevent auto-closing when clicked on toggle
    const $bcPageMenu = $('.breadcrumb .breadcrumb-menu.ui-state-active');
    $bcPageMenu.children('ul').slideUp('fast', 'easeInOutCirc');
    $bcPageMenu.removeClass('ui-state-active');
  }

  function showBreadcrumbMenu($bcPageMenu) {
    $bcPageMenu.addClass('ui-state-active');
    $bcPageMenu.children('ul').slideDown('fast', 'easeInOutCirc');
    $(document).one('mousedown', hideBreadcrumbMenu); // add event to hide menu when clicked outside
  }

  // set menu width to at least width of parent element
  $('.breadcrumb .breadcrumb-menu ul').css('min-width', function () {
    return $(this).prev().width();
  });

  $('.breadcrumb .breadcrumb-toggle').on('click', (e) => {
    const $bcMenu = $(e.currentTarget).closest('.breadcrumb-menu');
    const isActive = $bcMenu.hasClass('ui-state-active');
    hideBreadcrumbMenu(); // close all existing menu
    if (!isActive) showBreadcrumbMenu($bcMenu); // open new menu if it was not open before
    return false;
  });
});
