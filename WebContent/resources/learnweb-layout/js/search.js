/* global view */
/* global logResourceOpened, ajaxLoadNextPage */

function prepareResources() {
  if (view !== 'list') {
    $('#gallery').justifiedGallery({
      rowHeight: '160',
      maxRowHeight: '180',
      margins: 10,
      border: 0,
      captionsShowAlways: true,
      captionsAnimation: true,
    });

    $().fancybox({
      selector: '[data-fancybox="search-gallery"]',
      baseClass: 'fancybox-search-layout',
      infobar: false,
      toolbar: true,
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
        return $(current.opts.captionid).html();
      },
      onInit(instance) {
        instance.$refs.inner.wrap('<div class="fancybox-outer"></div>');
      },
    });
  }
}

function prepareNewResources() {
  if (view !== 'list') {
    $('#gallery').justifiedGallery('norewind');
  }
}

function testIfResultsFillPage() {
  if ($(window).scrollTop() + $(window).height() > $(document).height() - 100) {
    loadNextPage();
  }
}

let noMoreResults = false;
let loading = false;

function loadNextPage() {
  if (noMoreResults || loading) return;

  loading = true;
  $('#search_loading_more_results').show();
  ajaxLoadNextPage();
}

// noinspection JSUnusedGlobalSymbols
function displayNextPage(xhr, status, args) {
  const totalResults = $('#searchResults > div > .resource');
  const newResults = $('#searchResultsNext > div > .resource');
  $('#search_loading_more_results').hide();

  if (newResults.length === 0 || status !== 'success') {
    if (status !== 'success') console.error('Error on requesting more resources:', status);

    if (totalResults.length > 0) {
      $('#search_no_more_results').show();
    } else {
      $('#search_nothing_found').show();
    }

    noMoreResults = true;
    return;
  }

  // copy from #searchResultsNext to #searchResults
  $('#searchResultsNext > div > *').appendTo('#searchResults > div');

  loading = false;
  prepareNewResources();
  testIfResultsFillPage();
}

$(() => {
  prepareResources();

  testIfResultsFillPage();
  $(document).on('scroll', () => {
    testIfResultsFillPage();
  });

  // To keep track of resource click in the web search or resources_list view
  $(document).on('mouseup', '.resource a.resource-web-link', (e) => {
    const tempResourceId = $(e.currentTarget).closest('.resource').attr('id').substring(9);
    logResourceOpened([{ name: 'resource_id', value: tempResourceId }]);
  });
});
