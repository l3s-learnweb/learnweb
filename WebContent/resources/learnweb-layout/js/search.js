/* global view */
/* global ajaxLoadNextPage, logResourceOpenedCommand, getResourceDetailsCommand */

function prepareResources() {
  if (view !== 'list') {
    $('.search-float').justifiedGallery({
      rowHeight: '160',
      maxRowHeight: '180',
      margins: 8,
      border: 8,
      captionsShowAlways: true,
      captionsAnimation: true,
    });

    $().fancybox({
      selector: '.search-item-lightbox',
      baseClass: 'fancybox-search-layout',
      type: 'inline',
      smallBtn: false,
      infobar: false,
      toolbar: true,
      buttons: ['close'],
      animationEffect: 'fade',
      transitionEffect: 'fade',
      preventCaptionOverlap: false,
      idleTime: false,
      gutter: 0,
      onInit(instance) {
        instance.$refs.inner.wrap('<div class="fancybox-outer"></div>');

        instance.loadSlide = function (slide) {
          if (slide.isLoading || slide.isLoaded) {
            return;
          }

          instance.showLoading(slide);

          slide.isLoading = true;
          if (instance.trigger('beforeLoad', slide) === false) {
            slide.isLoading = false;
            return false;
          }

          slide.$slide.off('refresh').trigger('onReset').addClass(slide.opts.slideClass);

          const resourceRank = parseInt(slide.opts.$orig[0].id.replace('resource_', ''), 10);
          getResourceDetailsCommand([
            { name: 'slideIndex', value: slide.index },
            { name: 'resourceRank', value: resourceRank },
          ]);

          window.updateSlideDetails = function (xhr, status, { resourceRank: retRank, embeddedCode }) {
            if (retRank !== resourceRank) return;

            slide.isComplete = true;

            slide.opts.caption = $('#search_item_meta').html();
            // Set caption
            if (slide.opts.caption && slide.opts.caption.length) {
              instance.$caption = instance.$refs.caption;
              instance.$caption.children().eq(0).html(slide.opts.caption);
            }

            instance.setContent(slide, embeddedCode);
            instance.showControls();

            slide.$slide.one('onReset', function () {
              // Pause all html5 video/audio
              $(this).find('video,audio').trigger('pause');
            });
          };

          return true;
        };
      },
    });
  }
}

function updateSlideDetails() {
  // just a placeholder to avoid errors
}

function prepareNewResources() {
  if (view !== 'list') {
    $('.search-float').justifiedGallery('norewind');
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
function displayNextPage(xhr, status) {
  const totalResults = $('#searchResults .search-item');
  const newResults = $('#searchResultsNext .search-item');
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
  $('#searchResultsNext > *').appendTo('#searchResults');

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
  $(document).on('mouseup', '.search-item-web a.res-link', (e) => {
    const tempResourceId = $(e.currentTarget).closest('.resource').attr('id').substring(9);
    logResourceOpenedCommand([{ name: 'resource_id', value: tempResourceId }]);
  });
});
