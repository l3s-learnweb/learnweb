/*!
 * justifiedGallery - v3.8.1
 * http://miromannino.github.io/Justified-Gallery/
 * Copyright (c) 2023 Miro Mannino
 * Licensed under the MIT license.
 */
(() => {
  /**
   * Justified Gallery controller constructor
   *
   * @param $gallery the gallery to build
   * @param settings the settings (the defaults are in JustifiedGallery.defaults)
   * @constructor
   */
  const JustifiedGallery = function ($gallery, settings) {
    this.settings = settings;
    this.checkSettings();

    this.imgAnalyzerTimeout = null;
    this.entries = null;
    this.buildingRow = {
      entriesBuff: [],
      width: 0,
      height: 0,
      aspectRatio: 0,
    };
    this.lastFetchedEntry = null;
    this.lastAnalyzedIndex = -1;
    this.yield = {
      every: 2, // do a flush every n flushes (must be greater than 1)
      flushed: 0, // flushed rows without a yield
    };
    this.border = settings.border >= 0 ? settings.border : settings.margins;
    this.maxRowHeight = this.retrieveMaxRowHeight();
    this.suffixRanges = this.retrieveSuffixRanges();
    this.offY = this.border;
    this.rows = 0;
    this.spinner = {
      phase: 0,
      timeSlot: 150,
      $el: $('<div class="jg-spinner"><span></span><span></span><span></span></div>'),
      intervalId: null,
    };
    this.scrollBarOn = false;
    this.checkWidthIntervalId = null;
    this.galleryWidth = $gallery.width();
    this.$gallery = $gallery;

    this.galleryHeightToSet = 0;
  };

  /** @returns {String} the best suffix given the width and the height */
  JustifiedGallery.prototype.getSuffix = function (width, height) {
    let i;
    const longestSide = (width > height) ? width : height;
    for (i = 0; i < this.suffixRanges.length; i++) {
      if (longestSide <= this.suffixRanges[i]) {
        return this.settings.sizeRangeSuffixes[this.suffixRanges[i]];
      }
    }
    return this.settings.sizeRangeSuffixes[this.suffixRanges[i - 1]];
  };

  /**
   * Remove the suffix from the string
   *
   * @returns {string} a new string without the suffix
   */
  JustifiedGallery.prototype.removeSuffix = function (str, suffix) {
    return str.substring(0, str.length - suffix.length);
  };

  /**
   * @returns {boolean} a boolean to say if the suffix is contained in the str or not
   */
  JustifiedGallery.prototype.endsWith = function (str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
  };

  /**
   * Get the used suffix of a particular url
   *
   * @param str
   * @returns {String} return the used suffix
   */
  JustifiedGallery.prototype.getUsedSuffix = function (str) {
    // eslint-disable-next-line no-restricted-syntax
    for (const suffix of Object.values(this.settings.sizeRangeSuffixes)) {
      if (suffix.length !== 0 && this.endsWith(str, suffix)) return suffix;
    }
    return '';
  };

  /**
   * Given an image src, with the width and the height, returns the new image src with the
   * best suffix to show the best quality thumbnail.
   *
   * @returns {String} the suffix to use
   */
  JustifiedGallery.prototype.newSrc = function (imageSrc, imgWidth, imgHeight, image) {
    let newImageSrc;

    if (this.settings.thumbnailPath) {
      newImageSrc = this.settings.thumbnailPath(imageSrc, imgWidth, imgHeight, image);
    } else {
      const matchRes = imageSrc.match(this.settings.extension);
      const ext = (matchRes === null) ? '' : matchRes[0];
      newImageSrc = imageSrc.replace(this.settings.extension, '');
      newImageSrc = this.removeSuffix(newImageSrc, this.getUsedSuffix(newImageSrc));
      newImageSrc += this.getSuffix(imgWidth, imgHeight) + ext;
    }

    return newImageSrc;
  };

  /**
   * Shows the images that is in the given entry
   *
   * @param $entry the entry
   * @param callback the callback that is called when the show animation is finished
   */
  JustifiedGallery.prototype.showImg = function ($entry, callback) {
    if (this.settings.cssAnimation) {
      $entry.addClass('jg-entry-visible');
      if (callback) callback();
    } else {
      $entry.stop().fadeTo(this.settings.imagesAnimationDuration, 1.0, callback);
      $entry.find(this.settings.imgSelector).stop().fadeTo(this.settings.imagesAnimationDuration, 1.0, callback);
    }
  };

  /**
   * Extract the image src form the image, looking from the 'safe-src', and if it can't be found, from the
   * 'src' attribute. It saves in the image data the 'jg.originalSrc' field, with the extracted src.
   *
   * @param $image the image to analyze
   * @returns {String} the extracted src
   */
  JustifiedGallery.prototype.extractImgSrcFromImage = function ($image) {
    let imageSrc = $image.data('safe-src');
    let imageSrcLoc = 'data-safe-src';
    if (typeof imageSrc === 'undefined') {
      imageSrc = $image.attr('src');
      imageSrcLoc = 'src';
    }
    $image.data('jg.originalSrc', imageSrc); // this is saved for the destroy method
    $image.data('jg.src', imageSrc); // this will change overtime
    $image.data('jg.originalSrcLoc', imageSrcLoc); // this is saved for the destroy method
    return imageSrc;
  };

  /** @returns {jQuery} the image in the given entry */
  JustifiedGallery.prototype.imgFromEntry = function ($entry) {
    const $img = $entry.find(this.settings.imgSelector);
    return $img.length === 0 ? null : $img;
  };

  /** @returns {jQuery} the caption in the given entry */
  JustifiedGallery.prototype.captionFromEntry = function ($entry) {
    const $caption = $entry.find('> .jg-caption');
    return $caption.length === 0 ? null : $caption;
  };

  /**
   * Display the entry
   *
   * @param {jQuery} $entry the entry to display
   * @param {int} x the x position where the entry must be positioned
   * @param y the y position where the entry must be positioned
   * @param imgWidth the image width
   * @param imgHeight the image height
   * @param rowHeight the row height of the row that owns the entry
   */
  JustifiedGallery.prototype.displayEntry = function ($entry, x, y, imgWidth, imgHeight, rowHeight) {
    $entry.width(imgWidth);
    $entry.height(rowHeight);
    $entry.css('top', y);
    $entry.css('left', x);

    const $image = this.imgFromEntry($entry);
    if ($image !== null) {
      $image.css('width', imgWidth);
      $image.css('height', imgHeight);
      $image.css('margin-left', -imgWidth / 2);
      $image.css('margin-top', -imgHeight / 2);

      // Image reloading for a high quality of thumbnails
      let imageSrc = $image.data('jg.src');
      if (imageSrc) {
        imageSrc = this.newSrc(imageSrc, imgWidth, imgHeight, $image[0]);

        const self = this;
        $image.one('error', () => {
          self.resetImgSrc($image); // revert to the original thumbnail
        });

        const loadNewImage = function () {
          // if (imageSrc !== newImageSrc) {
          $image.attr('src', imageSrc);
          // }
        };

        if ($entry.data('jg.loaded') === 'skipped' && imageSrc) {
          this.onImageEvent(imageSrc, (() => {
            this.showImg($entry, loadNewImage); // load the new image after the fadeIn
            $entry.data('jg.loaded', true);
          }));
        } else {
          this.showImg($entry, loadNewImage); // load the new image after the fadeIn
        }
      }
    } else {
      this.showImg($entry);
    }

    this.displayEntryCaption($entry);
  };

  /**
   * Display the entry caption. If the caption element doesn't exists, it creates the caption using the 'alt'
   * or the 'title' attributes.
   *
   * @param {jQuery} $entry the entry to process
   */
  JustifiedGallery.prototype.displayEntryCaption = function ($entry) {
    const $image = this.imgFromEntry($entry);
    if ($image !== null && this.settings.captions) {
      let $imgCaption = this.captionFromEntry($entry);

      // Create it if it doesn't exist
      if ($imgCaption === null) {
        let caption = $image.attr('alt');
        if (!this.isValidCaption(caption)) caption = $entry.attr('title');
        if (this.isValidCaption(caption)) { // Create only we found something
          $imgCaption = $('<div class="jg-caption"></div>').text(caption);
          $entry.append($imgCaption);
          $entry.data('jg.createdCaption', true);
        }
      }

      // Create events (we check again the $imgCaption because it can be still inexistent)
      if ($imgCaption !== null) {
        if (!this.settings.cssAnimation) $imgCaption.stop().fadeTo(0, this.settings.captionSettings.nonVisibleOpacity);
        this.addCaptionEventsHandlers($entry);
      }
    } else {
      this.removeCaptionEventsHandlers($entry);
    }
  };

  /**
   * Validates the caption
   *
   * @param caption The caption that should be validated
   * @return {boolean} Validation result
   */
  JustifiedGallery.prototype.isValidCaption = function (caption) {
    return (typeof caption !== 'undefined' && caption.length > 0);
  };

  /**
   * The callback for the event 'mouseenter'. It assumes that the event currentTarget is an entry.
   * It shows the caption using jQuery (or using CSS if it is configured so)
   *
   * @param {Event} eventObject the event object
   */
  JustifiedGallery.prototype.onEntryMouseEnterForCaption = function (eventObject) {
    const $caption = this.captionFromEntry($(eventObject.currentTarget));
    if (this.settings.cssAnimation) {
      $caption.addClass('jg-caption-visible').removeClass('jg-caption-hidden');
    } else {
      $caption.stop().fadeTo(
        this.settings.captionSettings.animationDuration,
        this.settings.captionSettings.visibleOpacity,
      );
    }
  };

  /**
   * The callback for the event 'mouseleave'. It assumes that the event currentTarget is an entry.
   * It hides the caption using jQuery (or using CSS if it is configured so)
   *
   * @param {Event} eventObject the event object
   */
  JustifiedGallery.prototype.onEntryMouseLeaveForCaption = function (eventObject) {
    const $caption = this.captionFromEntry($(eventObject.currentTarget));
    if (this.settings.cssAnimation) {
      $caption.removeClass('jg-caption-visible').removeClass('jg-caption-hidden');
    } else {
      $caption.stop().fadeTo(
        this.settings.captionSettings.animationDuration,
        this.settings.captionSettings.nonVisibleOpacity,
      );
    }
  };

  /**
   * Add the handlers of the entry for the caption
   *
   * @param $entry the entry to modify
   */
  JustifiedGallery.prototype.addCaptionEventsHandlers = function ($entry) {
    let captionMouseEvents = $entry.data('jg.captionMouseEvents');
    if (typeof captionMouseEvents === 'undefined') {
      captionMouseEvents = {
        mouseenter: $.proxy(this.onEntryMouseEnterForCaption, this),
        mouseleave: $.proxy(this.onEntryMouseLeaveForCaption, this),
      };
      $entry.on('mouseenter', undefined, undefined, captionMouseEvents.mouseenter);
      $entry.on('mouseleave', undefined, undefined, captionMouseEvents.mouseleave);
      $entry.data('jg.captionMouseEvents', captionMouseEvents);
    }
  };

  /**
   * Remove the handlers of the entry for the caption
   *
   * @param $entry the entry to modify
   */
  JustifiedGallery.prototype.removeCaptionEventsHandlers = function ($entry) {
    const captionMouseEvents = $entry.data('jg.captionMouseEvents');
    if (typeof captionMouseEvents !== 'undefined') {
      $entry.off('mouseenter', undefined, captionMouseEvents.mouseenter);
      $entry.off('mouseleave', undefined, captionMouseEvents.mouseleave);
      $entry.removeData('jg.captionMouseEvents');
    }
  };

  /**
   * Clear the building row data to be used for a new row
   */
  JustifiedGallery.prototype.clearBuildingRow = function () {
    this.buildingRow.entriesBuff = [];
    this.buildingRow.aspectRatio = 0;
    this.buildingRow.width = 0;
  };

  /**
   * Justify the building row, preparing it to
   *
   * @param isLastRow
   * @param hiddenRow undefined or false for normal behavior. hiddenRow = true to hide the row.
   * @returns a boolean to know if the row has been justified or not
   */
  JustifiedGallery.prototype.prepareBuildingRow = function (isLastRow, hiddenRow) {
    let i;
    let $entry;
    let imgAspectRatio;
    let newImgW;
    let newImgH;
    let justify = true;
    let minHeight = 0;
    let availableWidth = this.galleryWidth - 2 * this.border - (
      (this.buildingRow.entriesBuff.length - 1) * this.settings.margins);
    const rowHeight = availableWidth / this.buildingRow.aspectRatio;
    let defaultRowHeight = this.settings.rowHeight;
    const justifiable = this.buildingRow.width / availableWidth > this.settings.justifyThreshold;

    // Skip the last row if we can't justify it and the lastRow == 'hide'
    if (hiddenRow || (isLastRow && this.settings.lastRow === 'hide' && !justifiable)) {
      for (i = 0; i < this.buildingRow.entriesBuff.length; i++) {
        $entry = this.buildingRow.entriesBuff[i];
        if (this.settings.cssAnimation) {
          $entry.removeClass('jg-entry-visible');
        } else {
          $entry.stop().fadeTo(0, 0.1);
          $entry.find('> img, > a > img').fadeTo(0, 0);
        }
      }
      return -1;
    }

    // With lastRow = nojustify, justify if is justificable (the images will not become too big)
    if (isLastRow && !justifiable && this.settings.lastRow !== 'justify' && this.settings.lastRow !== 'hide') {
      justify = false;

      if (this.rows > 0) {
        defaultRowHeight = (this.offY - this.border - this.settings.margins * this.rows) / this.rows;
        justify = (defaultRowHeight * this.buildingRow.aspectRatio) / availableWidth > this.settings.justifyThreshold;
      }
    }

    for (i = 0; i < this.buildingRow.entriesBuff.length; i++) {
      $entry = this.buildingRow.entriesBuff[i];
      imgAspectRatio = $entry.data('jg.width') / $entry.data('jg.height');

      if (justify) {
        newImgW = (i === this.buildingRow.entriesBuff.length - 1) ? availableWidth : rowHeight * imgAspectRatio;
        newImgH = rowHeight;
      } else {
        newImgW = defaultRowHeight * imgAspectRatio;
        newImgH = defaultRowHeight;
      }

      availableWidth -= Math.round(newImgW);
      $entry.data('jg.jwidth', Math.round(newImgW));
      $entry.data('jg.jheight', Math.ceil(newImgH));
      if (i === 0 || minHeight > newImgH) minHeight = newImgH;
    }

    this.buildingRow.height = minHeight;
    return justify;
  };

  /**
   * Flush a row: justify it, modify the gallery height accordingly to the row height
   *
   * @param isLastRow
   * @param hiddenRow undefined or false for normal behavior. hiddenRow = true to hide the row.
   */
  JustifiedGallery.prototype.flushRow = function (isLastRow, hiddenRow) {
    const { settings } = this;
    let $entry;
    let offX = this.border;

    const buildingRowRes = this.prepareBuildingRow(isLastRow, hiddenRow);
    if (hiddenRow || (isLastRow && settings.lastRow === 'hide' && buildingRowRes === -1)) {
      this.clearBuildingRow();
      return;
    }

    if (this.maxRowHeight) {
      if (this.maxRowHeight < this.buildingRow.height) this.buildingRow.height = this.maxRowHeight;
    }

    // Align last (unjustified) row
    if (isLastRow && (settings.lastRow === 'center' || settings.lastRow === 'right')) {
      let availableWidth = this.galleryWidth - 2 * this.border - (this.buildingRow.entriesBuff.length - 1) * settings.margins;

      for (let i = 0; i < this.buildingRow.entriesBuff.length; i++) {
        $entry = this.buildingRow.entriesBuff[i];
        availableWidth -= $entry.data('jg.jwidth');
      }

      if (settings.lastRow === 'center') {
        offX += Math.round(availableWidth / 2);
      } else if (settings.lastRow === 'right') {
        offX += availableWidth;
      }
    }

    const lastEntryIdx = this.buildingRow.entriesBuff.length - 1;
    for (let i = 0; i <= lastEntryIdx; i++) {
      $entry = this.buildingRow.entriesBuff[this.settings.rtl ? lastEntryIdx - i : i];
      this.displayEntry($entry, offX, this.offY, $entry.data('jg.jwidth'), $entry.data('jg.jheight'), this.buildingRow.height);
      offX += $entry.data('jg.jwidth') + settings.margins;
    }

    // Gallery Height
    this.galleryHeightToSet = this.offY + this.buildingRow.height + this.border;
    this.setGalleryTempHeight(this.galleryHeightToSet + this.getSpinnerHeight());

    if (!isLastRow || (this.buildingRow.height <= settings.rowHeight && buildingRowRes)) {
      // Ready for a new row
      this.offY += this.buildingRow.height + settings.margins;
      this.rows += 1;
      this.clearBuildingRow();
      this.settings.triggerEvent.call(this, 'jg.rowflush');
    }
  };

  // Scroll position not restoring: https://github.com/miromannino/Justified-Gallery/issues/221
  let galleryPrevStaticHeight = 0;

  JustifiedGallery.prototype.rememberGalleryHeight = function () {
    galleryPrevStaticHeight = this.$gallery.height();
    this.$gallery.height(galleryPrevStaticHeight);
  };

  // grow only
  JustifiedGallery.prototype.setGalleryTempHeight = function (height) {
    galleryPrevStaticHeight = Math.max(height, galleryPrevStaticHeight);
    this.$gallery.height(galleryPrevStaticHeight);
  };

  JustifiedGallery.prototype.setGalleryFinalHeight = function (height) {
    galleryPrevStaticHeight = height;
    this.$gallery.height(height);
  };

  /**
   * Checks the width of the gallery container, to know if a new justification is needed
   */
  JustifiedGallery.prototype.checkWidth = function () {
    this.checkWidthIntervalId = setInterval($.proxy(function () {
      // if the gallery is not currently visible, abort.
      if (!this.$gallery.is(':visible')) return;

      const galleryWidth = parseFloat(this.$gallery.width());
      if (Math.abs(galleryWidth - this.galleryWidth) > this.settings.refreshSensitivity) {
        this.galleryWidth = galleryWidth;
        this.rewind();

        this.rememberGalleryHeight();

        // Restart to analyze
        this.startImgAnalyzer(true);
      }
    }, this), this.settings.refreshTime);
  };

  /**
   * @returns {boolean} a boolean saying if the spinner is active or not
   */
  JustifiedGallery.prototype.isSpinnerActive = function () {
    return this.spinner.intervalId !== null;
  };

  /**
   * @returns {int} the spinner height
   */
  JustifiedGallery.prototype.getSpinnerHeight = function () {
    return this.spinner.$el.innerHeight();
  };

  /**
   * Stops the spinner animation and modify the gallery height to exclude the spinner
   */
  JustifiedGallery.prototype.stopLoadingSpinnerAnimation = function () {
    clearInterval(this.spinner.intervalId);
    this.spinner.intervalId = null;
    this.setGalleryTempHeight(this.$gallery.height() - this.getSpinnerHeight());
    this.spinner.$el.detach();
  };

  /**
   * Starts the spinner animation
   */
  JustifiedGallery.prototype.startLoadingSpinnerAnimation = function () {
    const spinnerContext = this.spinner;
    const $spinnerPoints = spinnerContext.$el.find('span');
    clearInterval(spinnerContext.intervalId);
    this.$gallery.append(spinnerContext.$el);
    this.setGalleryTempHeight(this.offY + this.buildingRow.height + this.getSpinnerHeight());
    spinnerContext.intervalId = setInterval(() => {
      if (spinnerContext.phase < $spinnerPoints.length) {
        $spinnerPoints.eq(spinnerContext.phase).fadeTo(spinnerContext.timeSlot, 1);
      } else {
        $spinnerPoints.eq(spinnerContext.phase - $spinnerPoints.length).fadeTo(spinnerContext.timeSlot, 0);
      }
      spinnerContext.phase = (spinnerContext.phase + 1) % ($spinnerPoints.length * 2);
    }, spinnerContext.timeSlot);
  };

  /**
   * Rewind the image analysis to start from the first entry.
   */
  JustifiedGallery.prototype.rewind = function () {
    this.lastFetchedEntry = null;
    this.lastAnalyzedIndex = -1;
    this.offY = this.border;
    this.rows = 0;
    this.clearBuildingRow();
  };

  /**
   * @returns {String} `settings.selector` rejecting spinner element
   */
  JustifiedGallery.prototype.getSelectorWithoutSpinner = function () {
    return `${this.settings.selector}, div:not(.jg-spinner)`;
  };

  /**
   * @returns {Array} all entries matched by `settings.selector`
   */
  JustifiedGallery.prototype.getAllEntries = function () {
    const selector = this.getSelectorWithoutSpinner();
    return this.$gallery.children(selector).toArray();
  };

  /**
   * Update the entries searching it from the justified gallery HTML element
   *
   * @param norewind if norewind only the new entries will be changed (i.e. randomized, sorted or filtered)
   * @returns {boolean} true if some entries has been founded
   */
  JustifiedGallery.prototype.updateEntries = function (norewind) {
    let newEntries;

    if (norewind && this.lastFetchedEntry != null) {
      const selector = this.getSelectorWithoutSpinner();
      newEntries = $(this.lastFetchedEntry).nextAll(selector).toArray();
    } else {
      this.entries = [];
      newEntries = this.getAllEntries();
    }

    if (newEntries.length > 0) {
      // Sort or randomize
      if ($.isFunction(this.settings.sort)) {
        newEntries = this.sortArray(newEntries);
      } else if (this.settings.randomize) {
        newEntries = this.shuffleArray(newEntries);
      }
      this.lastFetchedEntry = newEntries[newEntries.length - 1];

      // Filter
      if (this.settings.filter) {
        newEntries = this.filterArray(newEntries);
      } else {
        this.resetFilters(newEntries);
      }
    }

    this.entries = this.entries.concat(newEntries);
    return true;
  };

  /**
   * Apply the entries order to the DOM, iterating the entries and appending the images
   *
   * @param entries the entries that has been modified and that must be re-ordered in the DOM
   */
  JustifiedGallery.prototype.insertToGallery = function (entries) {
    const that = this;
    $.each(entries, function () {
      $(this).appendTo(that.$gallery);
    });
  };

  /**
   * Shuffle the array using the Fisher-Yates shuffle algorithm
   *
   * @param a the array to shuffle
   * @return the shuffled array
   */
  JustifiedGallery.prototype.shuffleArray = function (a) {
    let i;
    let j;
    let temp;
    for (i = a.length - 1; i > 0; i--) {
      j = Math.floor(Math.random() * (i + 1));
      temp = a[i];
      a[i] = a[j];
      a[j] = temp;
    }
    this.insertToGallery(a);
    return a;
  };

  /**
   * Sort the array using settings.comparator as comparator
   *
   * @param a the array to sort (it is sorted)
   * @return the sorted array
   */
  JustifiedGallery.prototype.sortArray = function (a) {
    a.sort(this.settings.sort);
    this.insertToGallery(a);
    return a;
  };

  /**
   * Reset the filters removing the 'jg-filtered' class from all the entries
   *
   * @param a the array to reset
   */
  JustifiedGallery.prototype.resetFilters = function (a) {
    for (let i = 0; i < a.length; i++) $(a[i]).removeClass('jg-filtered');
  };

  /**
   * Filter the entries considering theirs classes (if a string has been passed) or using a function for filtering.
   *
   * @param a the array to filter
   * @return the filtered array
   */
  JustifiedGallery.prototype.filterArray = function (a) {
    const { settings } = this;
    if ($.type(settings.filter) === 'string') {
      // Filter only keeping the entries passed in the string
      return a.filter((el) => {
        const $el = $(el);
        if ($el.is(settings.filter)) {
          $el.removeClass('jg-filtered');
          return true;
        }
        $el.addClass('jg-filtered').removeClass('jg-visible');
        return false;
      });
    } if ($.isFunction(settings.filter)) {
      // Filter using the passed function
      const filteredArr = a.filter(settings.filter);
      for (let i = 0; i < a.length; i++) {
        if (filteredArr.indexOf(a[i]) === -1) {
          $(a[i]).addClass('jg-filtered').removeClass('jg-visible');
        } else {
          $(a[i]).removeClass('jg-filtered');
        }
      }
      return filteredArr;
    }
  };

  /**
   * Revert the image src to the default value.
   */
  JustifiedGallery.prototype.resetImgSrc = function ($img) {
    if ($img.data('jg.originalSrcLoc') === 'src') {
      $img.attr('src', $img.data('jg.originalSrc'));
    } else {
      $img.attr('src', '');
    }
  };

  /**
   * Destroy the Justified Gallery instance.
   *
   * It clears all the css properties added in the style attributes. We doesn't backup the original
   * values for those css attributes, because it costs (performance) and because in general one
   * shouldn't use the style attribute for an uniform set of images (where we suppose the use of
   * classes). Creating a backup is also difficult because JG could be called multiple times and
   * with different style attributes.
   */
  JustifiedGallery.prototype.destroy = function () {
    clearInterval(this.checkWidthIntervalId);
    this.stopImgAnalyzerStarter();

    // Get fresh entries list since filtered entries are absent in `this.entries`
    $.each(this.getAllEntries(), $.proxy(function (_, entry) {
      const $entry = $(entry);

      // Reset entry style
      $entry.css('width', '');
      $entry.css('height', '');
      $entry.css('top', '');
      $entry.css('left', '');
      $entry.data('jg.loaded', undefined);
      $entry.removeClass('jg-entry jg-filtered jg-entry-visible');

      // Reset image style
      const $img = this.imgFromEntry($entry);
      if ($img) {
        $img.css('width', '');
        $img.css('height', '');
        $img.css('margin-left', '');
        $img.css('margin-top', '');
        this.resetImgSrc($img);
        $img.data('jg.originalSrc', undefined);
        $img.data('jg.originalSrcLoc', undefined);
        $img.data('jg.src', undefined);
      }

      // Remove caption
      this.removeCaptionEventsHandlers($entry);
      const $caption = this.captionFromEntry($entry);
      if ($entry.data('jg.createdCaption')) {
        // remove also the caption element (if created by jg)
        $entry.data('jg.createdCaption', undefined);
        if ($caption !== null) $caption.remove();
      } else if ($caption !== null) $caption.fadeTo(0, 1);
    }, this));

    this.$gallery.css('height', '');
    this.$gallery.removeClass('justified-gallery');
    this.$gallery.data('jg.controller', undefined);
    this.settings.triggerEvent.call(this, 'jg.destroy');
  };

  /**
   * Analyze the images and builds the rows. It returns if it found an image that is not loaded.
   *
   * @param isForResize if the image analyzer is called for resizing or not, to call a different callback at the end
   */
  JustifiedGallery.prototype.analyzeImages = function (isForResize) {
    for (let i = this.lastAnalyzedIndex + 1; i < this.entries.length; i++) {
      const $entry = $(this.entries[i]);
      if ($entry.data('jg.loaded') === true || $entry.data('jg.loaded') === 'skipped') {
        const availableWidth = this.galleryWidth - 2 * this.border - ((this.buildingRow.entriesBuff.length - 1) * this.settings.margins);
        const imgAspectRatio = $entry.data('jg.width') / $entry.data('jg.height');

        const test = availableWidth / (this.buildingRow.aspectRatio + imgAspectRatio);
        if (this.buildingRow.entriesBuff.length > 0 && test < this.settings.rowHeight) {
          this.flushRow(false, this.settings.maxRowsCount > 0 && this.rows === this.settings.maxRowsCount);

          if (++this.yield.flushed >= this.yield.every) {
            this.startImgAnalyzer(isForResize);
            return;
          }
        }

        this.buildingRow.entriesBuff.push($entry);
        this.buildingRow.aspectRatio += imgAspectRatio;
        this.buildingRow.width += imgAspectRatio * this.settings.rowHeight;
        this.lastAnalyzedIndex = i;
      } else if ($entry.data('jg.loaded') !== 'error') {
        return;
      }
    }

    // Last row flush (the row is not full)
    if (this.buildingRow.entriesBuff.length > 0) {
      this.flushRow(true, this.settings.maxRowsCount > 0 && this.rows === this.settings.maxRowsCount);
    }

    if (this.isSpinnerActive()) {
      this.stopLoadingSpinnerAnimation();
    }

    /* Stop, if there is, the timeout to start the analyzeImages.
     This is because an image can be set loaded, and the timeout can be set,
     but this image can be analyzed yet.
     */
    this.stopImgAnalyzerStarter();

    this.setGalleryFinalHeight(this.galleryHeightToSet);

    // On complete callback
    this.settings.triggerEvent.call(this, isForResize ? 'jg.resize' : 'jg.complete');
  };

  /**
   * Stops any ImgAnalyzer starter (that has an assigned timeout)
   */
  JustifiedGallery.prototype.stopImgAnalyzerStarter = function () {
    this.yield.flushed = 0;
    if (this.imgAnalyzerTimeout !== null) {
      clearTimeout(this.imgAnalyzerTimeout);
      this.imgAnalyzerTimeout = null;
    }
  };

  /**
   * Starts the image analyzer. It is not immediately called to let the browser to update the view
   *
   * @param isForResize specifies if the image analyzer must be called for resizing or not
   */
  JustifiedGallery.prototype.startImgAnalyzer = function (isForResize) {
    const that = this;
    this.stopImgAnalyzerStarter();
    this.imgAnalyzerTimeout = setTimeout(() => {
      that.analyzeImages(isForResize);
    }, 0.001); // we can't start it immediately due to a IE different behaviour
  };

  /**
   * Checks if the image is loaded or not using another image object. We cannot use the 'complete' image property,
   * because some browsers, with a 404 set complete = true.
   *
   * @param imageSrc the image src to load
   * @param onLoad callback that is called when the image has been loaded
   * @param onError callback that is called in case of an error
   */
  JustifiedGallery.prototype.onImageEvent = function (imageSrc, onLoad, onError) {
    if (!onLoad && !onError) return;

    const memImage = new Image();
    const $memImage = $(memImage);
    if (onLoad) {
      $memImage.one('load', () => {
        $memImage.off('load error');
        onLoad(memImage);
      });
    }
    if (onError) {
      $memImage.one('error', () => {
        $memImage.off('load error');
        onError(memImage);
      });
    }
    memImage.src = imageSrc;
  };

  /**
   * Init of Justified Gallery controlled
   * It analyzes all the entries starting theirs loading and calling the image analyzer (that works with loaded images)
   */
  JustifiedGallery.prototype.init = function () {
    let imagesToLoad = false;
    let skippedImages = false;
    const that = this;
    $.each(this.entries, (index, entry) => {
      const $entry = $(entry);
      const $image = that.imgFromEntry($entry);

      $entry.addClass('jg-entry');

      if ($entry.data('jg.loaded') !== true && $entry.data('jg.loaded') !== 'skipped') {
        // Link Rel global overwrite
        if (that.settings.rel !== null) $entry.attr('rel', that.settings.rel);

        // Link Target global overwrite
        if (that.settings.target !== null) $entry.attr('target', that.settings.target);

        if ($image !== null) {
          // Image src
          const imageSrc = that.extractImgSrcFromImage($image);

          /* If we have the height and the width, we don't wait that the image is loaded,
             but we start directly with the justification */
          if (that.settings.waitThumbnailsLoad === false || !imageSrc) {
            let width = parseFloat($image.attr('width'));
            let height = parseFloat($image.attr('height'));
            if ($image.prop('tagName') === 'svg') {
              width = parseFloat($image[0].getBBox().width);
              height = parseFloat($image[0].getBBox().height);
            }
            if (!Number.isNaN(width) && !Number.isNaN(height) && width !== 0 && height !== 0) {
              $entry.data('jg.width', width);
              $entry.data('jg.height', height);
              $entry.data('jg.loaded', 'skipped');
              skippedImages = true;
              that.startImgAnalyzer(false);
              return true; // continue
            }
          }

          $entry.data('jg.loaded', false);
          imagesToLoad = true;

          // Spinner start
          if (!that.isSpinnerActive()) that.startLoadingSpinnerAnimation();

          that.onImageEvent(imageSrc, (loadImg) => { // image loaded
            $entry.data('jg.width', loadImg.width);
            $entry.data('jg.height', loadImg.height);
            $entry.data('jg.loaded', true);
            that.startImgAnalyzer(false);
          }, () => { // image load error
            $entry.data('jg.loaded', 'error');
            that.startImgAnalyzer(false);
          });
        } else {
          $entry.data('jg.loaded', true);
          // eslint-disable-next-line no-bitwise
          $entry.data('jg.width', $entry.width() | parseFloat($entry.css('width')) | 1);
          // eslint-disable-next-line no-bitwise
          $entry.data('jg.height', $entry.height() | parseFloat($entry.css('height')) | 1);
        }
      }
    });

    if (!imagesToLoad && !skippedImages) this.startImgAnalyzer(false);
    this.checkWidth();
  };

  /**
   * Checks that it is a valid number. If a string is passed it is converted to a number
   *
   * @param settingContainer the object that contains the setting (to allow the conversion)
   * @param settingName the setting name
   */
  JustifiedGallery.prototype.checkOrConvertNumber = function (settingContainer, settingName) {
    if ($.type(settingContainer[settingName]) === 'string') {
      settingContainer[settingName] = parseFloat(settingContainer[settingName]);
    }

    if ($.type(settingContainer[settingName]) === 'number') {
      if (Number.isNaN(settingContainer[settingName])) throw new Error(`invalid number for ${settingName}`);
    } else {
      throw new Error(`${settingName} must be a number`);
    }
  };

  /**
   * Checks the sizeRangeSuffixes and, if necessary, converts
   * its keys from string (e.g. old settings with 'lt100') to int.
   */
  JustifiedGallery.prototype.checkSizeRangesSuffixes = function () {
    if ($.type(this.settings.sizeRangeSuffixes) !== 'object') {
      throw new Error('sizeRangeSuffixes must be defined and must be an object');
    }

    const suffixRanges = Object.keys(this.settings.sizeRangeSuffixes);
    const newSizeRngSuffixes = { 0: '' };
    for (let i = 0; i < suffixRanges.length; i++) {
      if ($.type(suffixRanges[i]) === 'string') {
        try {
          const numIdx = parseInt(suffixRanges[i].replace(/^[a-z]+/, ''), 10);
          newSizeRngSuffixes[numIdx] = this.settings.sizeRangeSuffixes[suffixRanges[i]];
        } catch (e) {
          throw new Error(`sizeRangeSuffixes keys must contains correct numbers (${e})`);
        }
      } else {
        newSizeRngSuffixes[suffixRanges[i]] = this.settings.sizeRangeSuffixes[suffixRanges[i]];
      }
    }

    this.settings.sizeRangeSuffixes = newSizeRngSuffixes;
  };

  /**
   * check and convert the maxRowHeight setting
   * requires rowHeight to be already set
   * TODO: should be always called when only rowHeight is changed
   * @return number or null
   */
  JustifiedGallery.prototype.retrieveMaxRowHeight = function () {
    let newMaxRowHeight = null;
    const { rowHeight } = this.settings;

    if ($.type(this.settings.maxRowHeight) === 'string') {
      if (this.settings.maxRowHeight.match(/^[0-9]+%$/)) {
        newMaxRowHeight = (rowHeight * parseFloat(this.settings.maxRowHeight.match(/^([0-9]+)%$/)[1])) / 100;
      } else {
        newMaxRowHeight = parseFloat(this.settings.maxRowHeight);
      }
    } else if ($.type(this.settings.maxRowHeight) === 'number') {
      newMaxRowHeight = this.settings.maxRowHeight;
    } else if (this.settings.maxRowHeight === false || this.settings.maxRowHeight == null) {
      return null;
    } else {
      throw new Error('maxRowHeight must be a number or a percentage');
    }

    // check if the converted value is not a number
    if (Number.isNaN(newMaxRowHeight)) throw new Error('invalid number for maxRowHeight');

    // check values, maxRowHeight must be >= rowHeight
    if (newMaxRowHeight < rowHeight) newMaxRowHeight = rowHeight;

    return newMaxRowHeight;
  };

  /**
   * Checks the settings
   */
  JustifiedGallery.prototype.checkSettings = function () {
    this.checkSizeRangesSuffixes();

    this.checkOrConvertNumber(this.settings, 'rowHeight');
    this.checkOrConvertNumber(this.settings, 'margins');
    this.checkOrConvertNumber(this.settings, 'border');
    this.checkOrConvertNumber(this.settings, 'maxRowsCount');

    const lastRowModes = [
      'justify',
      'nojustify',
      'left',
      'center',
      'right',
      'hide',
    ];
    if (lastRowModes.indexOf(this.settings.lastRow) === -1) {
      throw new Error(`lastRow must be one of: ${lastRowModes.join(', ')}`);
    }

    this.checkOrConvertNumber(this.settings, 'justifyThreshold');
    if (this.settings.justifyThreshold < 0 || this.settings.justifyThreshold > 1) {
      throw new Error('justifyThreshold must be in the interval [0,1]');
    }
    if ($.type(this.settings.cssAnimation) !== 'boolean') {
      throw new Error('cssAnimation must be a boolean');
    }

    if ($.type(this.settings.captions) !== 'boolean') throw new Error('captions must be a boolean');
    this.checkOrConvertNumber(this.settings.captionSettings, 'animationDuration');

    this.checkOrConvertNumber(this.settings.captionSettings, 'visibleOpacity');
    if (this.settings.captionSettings.visibleOpacity < 0
        || this.settings.captionSettings.visibleOpacity > 1) {
      throw new Error('captionSettings.visibleOpacity must be in the interval [0, 1]');
    }

    this.checkOrConvertNumber(this.settings.captionSettings, 'nonVisibleOpacity');
    if (this.settings.captionSettings.nonVisibleOpacity < 0
        || this.settings.captionSettings.nonVisibleOpacity > 1) {
      throw new Error('captionSettings.nonVisibleOpacity must be in the interval [0, 1]');
    }

    this.checkOrConvertNumber(this.settings, 'imagesAnimationDuration');
    this.checkOrConvertNumber(this.settings, 'refreshTime');
    this.checkOrConvertNumber(this.settings, 'refreshSensitivity');
    if ($.type(this.settings.randomize) !== 'boolean') throw new Error('randomize must be a boolean');
    if ($.type(this.settings.selector) !== 'string') throw new Error('selector must be a string');

    if (this.settings.sort !== false && !$.isFunction(this.settings.sort)) {
      throw new Error('sort must be false or a comparison function');
    }

    if (this.settings.filter !== false && !$.isFunction(this.settings.filter)
        && $.type(this.settings.filter) !== 'string') {
      throw new Error('filter must be false, a string or a filter function');
    }
  };

  /**
   * It brings all the indexes from the sizeRangeSuffixes and it orders them. They are then sorted and returned.
   * @returns {Array} sorted suffix ranges
   */
  JustifiedGallery.prototype.retrieveSuffixRanges = function () {
    const suffixRanges = [];
    // eslint-disable-next-line no-restricted-syntax
    for (const rangeIdx of Object.keys(this.settings.sizeRangeSuffixes)) {
      suffixRanges.push(parseInt(rangeIdx, 10));
    }
    // eslint-disable-next-line no-nested-ternary
    suffixRanges.sort((a, b) => (a > b ? 1 : (a < b ? -1 : 0)));
    return suffixRanges;
  };

  /**
   * Update the existing settings only changing some of them
   *
   * @param newSettings the new settings (or a subgroup of them)
   */
  JustifiedGallery.prototype.updateSettings = function (newSettings) {
    // In this case Justified Gallery has been called again changing only some options
    this.settings = $.extend({}, this.settings, newSettings);
    this.checkSettings();

    // As reported in the settings: negative value = same as margins, 0 = disabled
    this.border = this.settings.border >= 0 ? this.settings.border : this.settings.margins;

    this.maxRowHeight = this.retrieveMaxRowHeight();
    this.suffixRanges = this.retrieveSuffixRanges();
  };

  JustifiedGallery.prototype.defaults = {
    sizeRangeSuffixes: {}, /* e.g. Flickr configuration
        {
          100: '_t',  // used when longest is less than 100px
          240: '_m',  // used when longest is between 101px and 240px
          320: '_n',  // ...
          500: '',
          640: '_z',
          1024: '_b'  // used as else case because it is the last
        }
    */
    thumbnailPath: undefined, /* If defined, sizeRangeSuffixes is not used, and this function is used to determine the
    path relative to a specific thumbnail size. The function should accept respectively three arguments:
    current path, width and height */
    rowHeight: 120, // required? required to be > 0?
    maxRowHeight: false, // false or negative value to deactivate. Positive number to express the value in pixels,
    // A string '[0-9]+%' to express in percentage (e.g. 300% means that the row height
    // can't exceed 3 * rowHeight)
    maxRowsCount: 0, // maximum number of rows to be displayed (0 = disabled)
    margins: 1,
    border: -1, // negative value = same as margins, 0 = disabled, any other value to set the border

    lastRow: 'nojustify', // … which is the same as 'left', or can be 'justify', 'center', 'right' or 'hide'

    justifyThreshold: 0.90, /* if row width / available space > 0.90 it will be always justified
                             * (i.e. lastRow setting is not considered) */
    waitThumbnailsLoad: true,
    captions: true,
    cssAnimation: true,
    imagesAnimationDuration: 500, // ignored with css animations
    captionSettings: { // ignored with css animations
      animationDuration: 500,
      visibleOpacity: 0.7,
      nonVisibleOpacity: 0.0,
    },
    rel: null, // rewrite the rel of each analyzed links
    target: null, // rewrite the target of all links
    extension: /\.[^.\\/]+$/, // regexp to capture the extension of an image
    refreshTime: 200, // time interval (in ms) to check if the page changes its width
    refreshSensitivity: 0, // change in width allowed (in px) without re-building the gallery
    randomize: false,
    rtl: false, // right-to-left mode
    sort: false, /*
      - false: to do not sort
      - function: to sort them using the function as comparator (see Array.prototype.sort())
    */
    filter: false, /*
      - false, null or undefined: for a disabled filter
      - a string: an entry is kept if entry.is(filter string) returns true
                  see jQuery's .is() function for further information
      - a function: invoked with arguments (entry, index, array). Return true to keep the entry, false otherwise.
                    It follows the specifications of the Array.prototype.filter() function of JavaScript.
    */
    selector: 'a', // The selector that is used to know what are the entries of the gallery
    imgSelector: '> img, > a > img, > svg, > a > svg', // The selector that is used to know what are the images of each entry
    triggerEvent(event) { // This is called to trigger events, the default behavior is to call $.trigger
      this.$gallery.trigger(event); // Consider that 'this' is this set to the JustifiedGallery object, so it can
    }, // access to fields such as $gallery, useful to trigger events with jQuery.
  };

  /**
   * Justified Gallery plugin for jQuery
   *
   * Events
   *  - jg.complete : called when all the gallery has been created
   *  - jg.resize : called when the gallery has been resized
   *  - jg.rowflush : when a new row appears
   *
   * @param arg the action (or the settings) passed when the plugin is called
   * @returns {*} the object itself
   */
  $.fn.justifiedGallery = function (arg) {
    return this.each((index, gallery) => {
      const $gallery = $(gallery);
      $gallery.addClass('justified-gallery');

      let controller = $gallery.data('jg.controller');
      if (typeof controller === 'undefined') {
        // Create a controller and assign it to the object data
        if (typeof arg !== 'undefined' && arg !== null && $.type(arg) !== 'object') {
          if (arg === 'destroy') return; // Just a call to an unexisting object
          throw new Error('The argument must be an object');
        }
        controller = new JustifiedGallery($gallery, $.extend({}, JustifiedGallery.prototype.defaults, arg));
        $gallery.data('jg.controller', controller);
      } else if (arg === 'norewind') {
        // In this case, we don't rewind: we analyze only the latest images (e.g. to complete the last unfinished row
        // ... left to be more readable
      } else if (arg === 'destroy') {
        controller.destroy();
        return;
      } else {
        // In this case, Justified Gallery has been called again changing only some options
        controller.updateSettings(arg);
        controller.rewind();
      }

      // Update the entry list
      if (!controller.updateEntries(arg === 'norewind')) return;

      // Init justified gallery
      controller.init();
    });
  };
})();
