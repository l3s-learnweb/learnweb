/* global logResourceOpened, ajaxLoadNextPage */
/* global view */

let noMoreResults = false;

// jquery extension: uncomment function
(function ($) {
  $.fn.uncomment = function () {
    $(this).contents().each(function () {
      if (this.nodeType === 8) {
        $(this).replaceWith(this.nodeValue);
        /*
        // Need to "evaluate" the HTML content,
        // otherwise simple text won't replace
        var e = $('<span>' + this.nodeValue + '</span>');
        $(this).replaceWith(e.contents());
        */
      }
      /*
      else if ( this.hasChildNodes() ) {
          $(this).uncomment(recurse);
      } */
    });
  };
}(jQuery));

function prepareResources() {
  if (view !== 'list') {
    $('#gallery').justifiedGallery({
      rowHeight: '160',
      maxRowHeight: '180',
      margins: 10,
      border:0,
      captionsShowAlways: true,
      captionsAnimation: true,
    });
    $('[data-fancybox="search-gallery"]').fancybox({
      baseClass: 'fancybox-search-layout',
      infobar: false,
      touch: {
        vertical: false,
      },
      buttons: ['close'],
      animationEffect: 'fade',
      transitionEffect: 'fade',
      preventCaptionOverlap: false,
      idleTime: false,
      gutter: 0,
      caption(instance, current) {
        const $caption = $(current.opts.captionid);
        return $caption.html();
      },
      onInit(instance) {
        instance.$refs.inner.wrap('<div class="fancybox-outer"></div>');
      },
    });
  }
}


let loading = false;

function loadNextPage() {
  if (noMoreResults || loading) { return; } // nothing found || not searched
  loading = true;
  $('#search_loading_more_results').show();
  ajaxLoadNextPage();
}

function displayNextPage(xhr, status) {
  const results = $('#new_results > div > div > div'); // this can include additional html like the "Page x:" on text search
  const resources = results.filter('.resource');
  $('#search_loading_more_results').hide();
  if (resources.length === 0 || status !== 'success') {
    if (status !== 'success') console.log('fehler', status);
    if (results.length > 0) $('#search_no_more_results').show();
    else $('#search_nothing_found').show();
    noMoreResults = true;
    return;
  }
  prepareResources();
  $('#results > div').append(results);
  loading = false;
  if (view === 'list') createGroupTooltips();
  testIfResultsFillPage();
}

function testIfResultsFillPage() {
  if ($(window).scrollTop() + $(window).height() > $(document).height() - 100) {
    loadNextPage();
  }
}

window.onload = testIfResultsFillPage;

function createGroupTooltips() {
  $('.tooltip').tooltipster({
    contentAsHTML: true,
    maxWidth: 400,
    position: 'right',
    interactive: true,
    multiple: true,
    interactiveTolerance: 150,
    theme: 'tooltipster-custom-theme',
  });
}

$(() => {
  prepareResources();

  $(document).on('scroll', () => {
    testIfResultsFillPage();
  });

  // To keep track of resource click in the web search or resources_list view
  $('.resourceWebLink, .resource > div a').on('mouseup', (e) => {
    const tempResourceId = $(e.currentTarget).closest('div.resource').attr('id').substring(9);
    logResourceOpened([{ name: 'resource_id', value: tempResourceId }]);
    return true;
  });

  createGroupTooltips();
});
