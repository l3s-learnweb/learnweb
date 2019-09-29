/*
 *  Gridder - v1.4.2
 *  A jQuery plugin that displays a thumbnail grid expanding preview similar to the effect seen on Google Images.
 *  http://www.oriongunning.com/
 *
 *  Made by Orion Gunning
 *  Under MIT License
 */
;(function () {

  /* Custom Easing */
  $.fn.extend($.easing, {
    def: "easeInOutExpo", easeInOutExpo: function (e, f, a, h, g) {
      if (f === 0) {
        return a;
      }
      if (f === g) {
        return a + h;
      }
      if ((f /= g / 2) < 1) {
        return h / 2 * Math.pow(2, 10 * (f - 1)) + a;
      }
      return h / 2 * (-Math.pow(2, -10 * --f) + 2) + a;
    }
  });

  /* KEYPRESS LEFT & RIGHT ARROW */
  /* This will work only if a current gridder is opened. */
  $(document).on('keydown', function (e) {
    var keyCode = e.keyCode;
    var $current_gridder = $(".currentGridder");
    var $current_target = $current_gridder.find(".gridder-show");
    if ($current_gridder.length) {
      if (keyCode === 37) {
        //console.log("Pressed Left Arrow");
        $current_target.prev().prev().trigger("click");
        e.preventDefault();
      }
      if (keyCode === 39) {
        //console.log("Pressed Right Arrow");
        $current_target.next().trigger("click");
        e.preventDefault();
      }
    } else {
      //console.log("No active gridder.");
    }
  });

  $.fn.gridderExpander = function (options) {

    /* GET DEFAULT OPTIONS OR USE THE ONE PASSED IN THE FUNCTION  */
    var settings = $.extend({}, $.fn.gridderExpander.defaults, options);

    return this.each(function () {

      var myBloc;
      var _this = $(this);
      var visible = false;

      // START CALLBACK
      settings.onStart(_this);

      // CLOSE FUNCTION
      function closeExpander(base) {

        // SCROLL TO CORRECT POSITION FIRST
        if (settings.scroll) {
          $("html, body").animate({
            scrollTop: base.find(".gridder-active").offset().top - settings.scrollOffset
          }, {
            duration: 200,
            easing: settings.animationEasing
          });
        }

        _this.removeClass("hasSelectedItem");

        // REMOVES GRIDDER EXPAND AREA
        visible = false;
        base.find(".gridder-active").removeClass("gridder-active");

        base.find(".gridder-show").slideUp(settings.animationSpeed, settings.animationEasing, function () {
          base.find(".gridder-show").remove();
          settings.onClosed(base);
        });

        /* REMOVE CURRENT ACTIVE GRIDDER */
        $(".currentGridder").removeClass("currentGridder");
      }

      // OPEN EXPANDER
      function openExpander(myself) {

        /* CURRENT ACTIVE GRIDDER */
        $(".currentGridder").removeClass("currentGridder");
        _this.addClass("currentGridder");

        /* ENSURES THE CORRECT BLOC IS ACTIVE */
        if (!myself.hasClass("gridder-active")) {
          _this.find(".gridder-active").removeClass("gridder-active");
          myself.addClass("gridder-active");
        } else {
          // THE SAME IS ALREADY OPEN, LET"S CLOSE IT
          closeExpander(_this, settings);
          return;
        }

        /* REMOVES PREVIOUS BLOC */
        _this.find(".gridder-show").remove();


        /* ADD CLASS TO THE GRIDDER CONTAINER
         * So you can apply global style when item selected.
         */
        if (!_this.hasClass("hasSelectedItem")) {
          _this.addClass("hasSelectedItem");
        }

        /* ADD LOADING BLOC */
        var $htmlContent = $("<div class=\"gridder-show loading\"></div>");
        myBloc = $htmlContent.insertAfter(myself);

        /* GET CONTENT VIA AJAX OR #ID*/
        var gridderContent = myself.data("griddercontent");
        var theContent = $(gridderContent).html();
        processContent(myself, theContent);
      }

      // PROCESS CONTENT
      function processContent(myself, theContent) {

        /* FORMAT OUTPUT */
        var htmlContent = "<div class=\"gridder-padding\">";

        if (settings.showNav) {

          /* CHECK IF PREV AND NEXT BUTTON HAVE ITEMS */
          var activeItem = $(".gridder-active");
          var prevItem = (activeItem.prev());
          var nextItem = (activeItem.next().next());

          htmlContent += "<div class=\"gridder-navigation\">";
          htmlContent += "<a href=\"#\" class=\"gridder-close\">" + settings.closeText + "</a>";
          htmlContent += "<a href=\"#\" class=\"gridder-nav prev " + (!prevItem.length ? "disabled" : "") + "\">" + settings.prevText + "</a>";
          htmlContent += "<a href=\"#\" class=\"gridder-nav next " + (!nextItem.length ? "disabled" : "") + "\">" + settings.nextText + "</a>";
          htmlContent += "</div>";
        }

        htmlContent += "<div class=\"gridder-expanded-content\">";
        htmlContent += theContent;
        htmlContent += "</div>";
        htmlContent += "</div>";

        // IF EXPANDER IS ALREADY EXPANDED
        if (!visible) {
          myBloc.hide().append(htmlContent).slideDown(settings.animationSpeed, settings.animationEasing, function () {
            visible = true;
            /* AFTER EXPAND CALLBACK */
            if (typeof settings.onContent === "function") {
              settings.onContent(myBloc);
            }
          });
        } else {
          myBloc.html(htmlContent);
          myBloc.find(".gridder-padding").fadeIn(settings.animationSpeed, settings.animationEasing, function () {
            visible = true;
            /* CHANGED CALLBACK */
            if (typeof settings.onContent === "function") {
              settings.onContent(myBloc);
            }
          });
        }

        /* SCROLL TO CORRECT POSITION AFTER */
        if (settings.scroll) {
          var offset = (settings.scrollTo === "panel" ? myself.offset().top + myself.height() - settings.scrollOffset : myself.offset().top - settings.scrollOffset);
          $("html, body").animate({
            scrollTop: offset
          }, {
            duration: settings.animationSpeed,
            easing: settings.animationEasing
          });
        }

        /* REMOVE LOADING CLASS */
        myBloc.removeClass("loading");
      }

      /* CLICK EVENT */
      _this.on("click", ".gridder-list", function (e) {
        e.preventDefault();
        var myself = $(this);
        openExpander(myself);
      });

      /* NEXT BUTTON */
      _this.on("click", ".gridder-nav.next", function (e) {
        e.preventDefault();
        $(this).parents(".gridder-show").next().trigger("click");
      });

      /* PREVIOUS BUTTON */
      _this.on("click", ".gridder-nav.prev", function (e) {
        e.preventDefault();
        $(this).parents(".gridder-show").prev().prev().trigger("click");
      });

      /* CLOSE BUTTON */
      _this.on("click", ".gridder-close", function (e) {
        e.preventDefault();
        closeExpander(_this);
      });
    });
  };

  // Default Options
  $.fn.gridderExpander.defaults = {
    scroll: true,
    scrollOffset: 30,
    scrollTo: "panel", // panel or listitem
    animationSpeed: 400,
    animationEasing: "easeInOutExpo",
    showNav: true,
    nextText: "Next",
    prevText: "Previous",
    closeText: "Close",
    onStart: function () {
    },
    onContent: function () {
    },
    onClosed: function () {
    }
  };

})();
