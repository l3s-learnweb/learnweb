/* global view */
/* global commandLoadNextPage, commandOnResourceClick, commandGetResourceDetails */

let noMoreResults = false;
let loading = false;

function prepareResources() {
  if ($.fancybox) {
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
          commandGetResourceDetails([
            { name: 'slideIndex', value: slide.index },
            { name: 'resourceRank', value: resourceRank },
          ]);

          window.updateSlideDetails = function (xhr, status, { slideIndex, resourceRank: retRank, embeddedCode }) {
            if (retRank !== resourceRank) {
              instance.slides[slideIndex].isLoading = false;
              return false;
            }

            instance.setContent(slide, embeddedCode);

            slide.opts.caption = $('#search_item_meta').html();
            // Set caption
            if (slide.opts.caption && slide.opts.caption.length && slide.index === instance.currIndex) {
              instance.$caption = instance.$refs.caption;
              instance.$caption.children().eq(0).html(slide.opts.caption);
              instance.showControls();
            }

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
  if (view === 'float') {
    $('.search-float').justifiedGallery('norewind').on('jg.rowflush', () => {
      loading = false;
    });
  } else {
    loading = false;
  }
}

function testIfResultsFillPage() {
  if ($(window).scrollTop() + $(window).height() > $(document).height() - 100) {
    loadNextPage();
  }
}

function loadNextPage() {
  if (noMoreResults || loading) return;

  loading = true;
  $('#search_loading_more_results').show();
  commandLoadNextPage();
}

// noinspection JSUnusedGlobalSymbols
function displayNextPage(xhr, status) {
  const currentWrapper = $(PrimeFaces.escapeClientId('search_results:current'));
  const nextWrapper = $(PrimeFaces.escapeClientId('search_results:next'));

  const currentResults = $('.search-item', currentWrapper);
  const newResults = $('.search-item', nextWrapper);
  $('#search_loading_more_results').hide();

  if (newResults.length === 0 || status !== 'success') {
    if (status !== 'success') console.error('Error on requesting more resources:', status);

    if (currentResults.length > 0) {
      $('#search_no_more_results').show();
    } else {
      $('#search_nothing_found').show();
    }

    noMoreResults = true;
    return;
  }

  // copy from #next to #current
  currentWrapper.append(newResults);

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
  $(document).on('mouseup', PrimeFaces.escapeClientId('search_results:current .search-item'), (e) => {
    const tempResourceId = $(e.currentTarget).attr('id').substring(9);
    commandOnResourceClick([{ name: 'resourceId', value: tempResourceId }]);
  });
});
