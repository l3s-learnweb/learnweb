/**
 * jQuery autoComplete v1.0.7
 * Copyright (c) 2014 Simon Steinberger / Pixabay
 * GitHub: https://github.com/Pixabay/jQuery-autoComplete
 * License: http://www.opensource.org/licenses/mit-license.php
 */

(function ($) {
  $.fn.autoComplete = function (options) {
    const o = $.extend({}, $.fn.autoComplete.defaults, options);

    // public methods
    if (typeof options === 'string') {
      this.each(function () {
        const that = $(this);
        if (options === 'destroy') {
          $(window).off('resize.autocomplete', that.updateSC);
          that.off('blur.autocomplete focus.autocomplete keydown.autocomplete keyup.autocomplete');
          if (that.data('autocomplete')) that.attr('autocomplete', that.data('autocomplete'));
          else that.removeAttr('autocomplete');
          $(that.data('sc')).remove();
          that.removeData('sc').removeData('autocomplete');
        }
      });
      return this;
    }

    return this.each(function () {
      const that = $(this);
      // sc = 'suggestions container'
      that.sc = $(`<div class="autocomplete-suggestions ${o.menuClass}"></div>`);
      that.data('sc', that.sc).data('autocomplete', that.attr('autocomplete'));
      that.attr('autocomplete', 'off');
      that.cache = {};
      that.last_val = '';

      that.updateSC = function (resize, next) {
        that.sc.css({
          top: that.offset().top + that.outerHeight(),
          left: that.offset().left,
          width: that.outerWidth(),
        });
        if (!resize) {
          that.sc.show();
          if (!that.sc.maxHeight) that.sc.maxHeight = parseInt(that.sc.css('max-height'), 10);
          if (!that.sc.suggestionHeight) that.sc.suggestionHeight = $('.autocomplete-suggestion', that.sc).first().outerHeight();
          if (that.sc.suggestionHeight) {
            if (!next) that.sc.scrollTop(0);
            else {
              const scrTop = that.sc.scrollTop();
              const selTop = next.offset().top - that.sc.offset().top;
              if (selTop + that.sc.suggestionHeight - that.sc.maxHeight > 0) {
                that.sc.scrollTop(selTop + that.sc.suggestionHeight + scrTop - that.sc.maxHeight);
              } else if (selTop < 0) {
                that.sc.scrollTop(selTop + scrTop);
              }
            }
          }
        }
      };
      $(window).on('resize.autocomplete', that.updateSC);

      that.sc.appendTo('body');

      that.sc.on('mouseleave', '.autocomplete-suggestion', () => {
        $('.autocomplete-suggestion.selected').removeClass('selected');
      });

      that.sc.on('mouseenter', '.autocomplete-suggestion', function () {
        $('.autocomplete-suggestion.selected').removeClass('selected');
        $(this).addClass('selected');
      });

      that.sc.on('mousedown click', '.autocomplete-suggestion', function (e) {
        const item = $(this); const
          v = item.data('val');
        if (v || item.hasClass('autocomplete-suggestion')) { // else outside click
          that.val(v);
          o.onSelect(e, v, item);
          that.sc.hide();
        }
        return false;
      });

      that.on('blur.autocomplete', () => {
        const overSb = $('.autocomplete-suggestions:hover').length;
        if (!overSb) {
          that.last_val = that.val();
          that.sc.hide();
          setTimeout(() => { that.sc.hide(); }, 350); // hide suggestions on fast input
        } else if (!that.is(':focus')) setTimeout(() => { that.focus(); }, 20);
      });

      if (!o.minChars) that.on('focus.autocomplete', () => { that.last_val = '\n'; that.trigger('keyup.autocomplete'); });

      function suggest(data) {
        const val = that.val();
        that.cache[val] = data;
        if (data.length && val.length >= o.minChars) {
          let s = '';
          for (let i = 0; i < data.length; i++) s += o.renderItem(data[i], val);
          that.sc.html(s);
          that.updateSC(0);
        } else that.sc.hide();
      }

      that.on('keydown.autocomplete', (e) => {
        // down (40), up (38)
        if ((e.which === 40 || e.which === 38) && that.sc.html()) {
          let next;
          const sel = $('.autocomplete-suggestion.selected', that.sc);
          if (!sel.length) {
            next = (e.which === 40) ? $('.autocomplete-suggestion', that.sc).first() : $('.autocomplete-suggestion', that.sc).last();
            that.val(next.addClass('selected').data('val'));
          } else {
            next = (e.which === 40) ? sel.next('.autocomplete-suggestion') : sel.prev('.autocomplete-suggestion');
            if (next.length) {
              sel.removeClass('selected');
              that.val(next.addClass('selected').data('val'));
            } else {
              sel.removeClass('selected');
              that.val(that.last_val);
              next = 0;
            }
          }
          that.updateSC(0, next);
          return false;
        }
        // esc
        if (e.which === 27) that.val(that.last_val).sc.hide();
        // enter or tab
        else if (e.which === 13 || e.which === 9) {
          const sel = $('.autocomplete-suggestion.selected', that.sc);
          if (sel.length && that.sc.is(':visible')) { o.onSelect(e, sel.data('val'), sel); setTimeout(() => { that.sc.hide(); }, 20); }
        }
      });

      that.on('keyup.autocomplete', (e) => {
        if ($.inArray(e.which, [13, 27, 35, 36, 37, 38, 39, 40]) === -1) {
          const val = that.val();
          if (val.length >= o.minChars) {
            if (val !== that.last_val) {
              that.last_val = val;
              clearTimeout(that.timer);
              if (o.cache) {
                if (val in that.cache) { suggest(that.cache[val]); return; }
                // no requests if previous suggestions were empty
                for (let i = 1; i < val.length - o.minChars; i++) {
                  const part = val.slice(0, val.length - i);
                  if (part in that.cache && !that.cache[part].length) { suggest([]); return; }
                }
              }
              that.timer = setTimeout(() => { o.source(val, suggest); }, o.delay);
            }
          } else {
            that.last_val = val;
            that.sc.hide();
          }
        }
      });
    });
  };

  $.fn.autoComplete.defaults = {
    source: 0,
    minChars: 3,
    delay: 150,
    cache: 1,
    menuClass: '',
    renderItem(item, search) {
      // escape special characters
      search = search.replace(/[-/\\^$*+?.()|[\]{}]/g, '\\$&');
      const re = new RegExp(`(${search.split(' ').join('|')})`, 'gi');
      return `<div class="autocomplete-suggestion" data-val="${item}">${item.replace(re, '<b>$1</b>')}</div>`;
    },
    onSelect(e, term, item) {},
  };
}(jQuery));
