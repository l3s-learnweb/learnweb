/*!
 * Gridder - v1.4.2
 * A jQuery plugin that displays a thumbnail grid expanding preview similar to the effect seen on Google Images.
 * http://www.oriongunning.com/
 *
 * Made by Orion Gunning
 * Under MIT License
 */
(() => {
  /* Custom Easing */
  $.fn.extend($.easing, {
    // eslint-disable-next-line
    def:"easeInOutExpo", easeInOutExpo:function(e,f,a,h,g){if(f===0){return a;}if(f===g){return a+h;}if((f/=g/2)<1){return h/2*Math.pow(2,10*(f-1))+a;}return h/2*(-Math.pow(2,-10*--f)+2)+a;}
  });

  /* KEYPRESS LEFT & RIGHT ARROW */
  /* This will work only if a current gridder is opened. */
  $(document).on('keydown', (e) => {
    const $currentGridder = $('.currentGridder');
    const $currentTarget = $currentGridder.find('.gridder-show');
    if ($currentGridder.length) {
      if (e.key === 'ArrowLeft') {
        // console.log("Pressed Left Arrow");
        $currentTarget.prev().prev().trigger('click');
        e.preventDefault();
      }
      if (e.key === 'ArrowRight') {
        // console.log("Pressed Right Arrow");
        $currentTarget.next().trigger('click');
        e.preventDefault();
      }
    } else {
      // console.log("No active gridder.");
    }
  });

  $.fn.gridderExpander = function (options) {
    /* GET DEFAULT OPTIONS OR USE THE ONE PASSED IN THE FUNCTION  */
    const settings = $.extend({}, $.fn.gridderExpander.defaults, options);

    return this.each((index, element) => {
      let myBloc;
      const $this = $(element);
      let visible = false;

      // START CALLBACK
      settings.onStart($this);

      // CLOSE FUNCTION
      function closeExpander(base) {
        // SCROLL TO CORRECT POSITION FIRST
        if (settings.scroll) {
          $('html, body').animate({
            scrollTop: base.find('.gridder-active').offset().top - settings.scrollOffset,
          }, {
            duration: 200,
            easing: settings.animationEasing,
          });
        }

        $this.removeClass('hasSelectedItem');

        // REMOVES GRIDDER EXPAND AREA
        visible = false;
        base.find('.gridder-active').removeClass('gridder-active');

        base.find('.gridder-show').slideUp(settings.animationSpeed, settings.animationEasing, () => {
          base.find('.gridder-show').remove();
          settings.onClosed(base);
        });

        /* REMOVE CURRENT ACTIVE GRIDDER */
        $('.currentGridder').removeClass('currentGridder');
      }

      // OPEN EXPANDER
      function openExpander(myself) {
        /* CURRENT ACTIVE GRIDDER */
        $('.currentGridder').removeClass('currentGridder');
        $this.addClass('currentGridder');

        /* ENSURES THE CORRECT BLOC IS ACTIVE */
        if (!myself.hasClass('gridder-active')) {
          $this.find('.gridder-active').removeClass('gridder-active');
          myself.addClass('gridder-active');
        } else {
          // THE SAME IS ALREADY OPEN, LET"S CLOSE IT
          closeExpander($this, settings);
          return;
        }

        /* REMOVES PREVIOUS BLOC */
        $this.find('.gridder-show').remove();

        /* ADD CLASS TO THE GRIDDER CONTAINER
         * So you can apply global style when item selected.
         */
        if (!$this.hasClass('hasSelectedItem')) {
          $this.addClass('hasSelectedItem');
        }

        /* ADD LOADING BLOC */
        const $htmlContent = $('<div class="gridder-show loading"></div>');
        myBloc = $htmlContent.insertAfter(myself);

        /* GET CONTENT VIA AJAX OR #ID */
        const gridderContent = myself.data('griddercontent');
        const theContent = $(gridderContent).html();
        processContent(myself, theContent);
      }

      // PROCESS CONTENT
      function processContent(myself, theContent) {
        /* FORMAT OUTPUT */
        let htmlContent = '<div class="gridder-padding">';

        if (settings.showNav) {
          /* CHECK IF PREV AND NEXT BUTTON HAVE ITEMS */
          const activeItem = $('.gridder-active');
          const prevItem = (activeItem.prev());
          const nextItem = (activeItem.next().next());

          htmlContent += '<div class="gridder-navigation">';
          htmlContent += `<a href="#" class="gridder-close">${settings.closeText}</a>`;
          htmlContent += `<a href="#" class="gridder-nav prev ${prevItem.length ? '' : 'disabled'}">${settings.prevText}</a>`;
          htmlContent += `<a href="#" class="gridder-nav next ${nextItem.length ? '' : 'disabled'}">${settings.nextText}</a>`;
          htmlContent += '</div>';
        }

        htmlContent += '<div class="gridder-expanded-content">';
        htmlContent += theContent;
        htmlContent += '</div>';
        htmlContent += '</div>';

        // IF EXPANDER IS ALREADY EXPANDED
        if (!visible) {
          myBloc.hide().append(htmlContent).slideDown(settings.animationSpeed, settings.animationEasing, () => {
            visible = true;
            /* AFTER EXPAND CALLBACK */
            if (typeof settings.onContent === 'function') {
              settings.onContent(myBloc);
            }
          });
        } else {
          myBloc.html(htmlContent);
          myBloc.find('.gridder-padding').fadeIn(settings.animationSpeed, settings.animationEasing, () => {
            visible = true;
            /* CHANGED CALLBACK */
            if (typeof settings.onContent === 'function') {
              settings.onContent(myBloc);
            }
          });
        }

        /* SCROLL TO CORRECT POSITION AFTER */
        if (settings.scroll) {
          const offset = (settings.scrollTo === 'panel'
            ? myself.offset().top + myself.height() - settings.scrollOffset
            : myself.offset().top - settings.scrollOffset);

          $('html, body').animate({
            scrollTop: offset,
          }, {
            duration: settings.animationSpeed,
            easing: settings.animationEasing,
          });
        }

        /* REMOVE LOADING CLASS */
        myBloc.removeClass('loading');
      }

      /* CLICK EVENT */
      $this.on('click', '.gridder-list', (e) => {
        e.preventDefault();
        const myself = $(e.currentTarget);
        openExpander(myself);
      });

      /* NEXT BUTTON */
      $this.on('click', '.gridder-nav.next', (e) => {
        e.preventDefault();
        $(e.currentTarget).parents('.gridder-show').next().trigger('click');
      });

      /* PREVIOUS BUTTON */
      $this.on('click', '.gridder-nav.prev', (e) => {
        e.preventDefault();
        $(e.currentTarget).parents('.gridder-show').prev().prev()
          .trigger('click');
      });

      /* CLOSE BUTTON */
      $this.on('click', '.gridder-close', (e) => {
        e.preventDefault();
        closeExpander($this);
      });
    });
  };

  // Default Options
  $.fn.gridderExpander.defaults = {
    scroll: true,
    scrollOffset: 30,
    scrollTo: 'panel', // panel or listitem
    animationSpeed: 400,
    animationEasing: 'easeInOutExpo',
    showNav: true,
    nextText: 'Next',
    prevText: 'Previous',
    closeText: 'Close',
    onStart() {},
    onContent() {},
    onClosed() {},
  };
})();
