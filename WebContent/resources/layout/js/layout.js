/* global onUnloadCommand, setPreferenceRemote */

/**
 * PrimeFaces LearnwebTheme Layout
 *
 * This file will be loaded on every page.
 * Include only methods which are required on every page.
 */
PrimeFaces.widget.LearnwebTheme = PrimeFaces.widget.BaseWidget.extend({

  init(cfg) {
    this._super(cfg);
    this.wrapper = $('.layout-wrapper');

    if (this.wrapper.length === 0) return;
    this.header = this.wrapper.children('.layout-header');
    this.menuButton = this.header.find('#menu-button');

    this._bindEvents();
    this._autoComplete();
  },

  _autoComplete() {
    let currentRequest;
    const myMarket = PrimeFaces.settings.locale;
    this.header.find('#header_left\\:searchfield').autoComplete({
      source(term, response) {
        currentRequest = $.ajax({
          url: 'https://api.bing.com/osjson.aspx?JsonType=callback&JsonCallback=?',
          data: {
            query: term,
            market: myMarket,
          },
          dataType: 'jsonp',
          beforeSend() {
            if (currentRequest != null) {
              currentRequest.abort();
            }
          },
          success(data) {
            const suggestions = [];
            $.each(data[1], (i, val) => {
              suggestions.push(val);
            });
            response(suggestions);
          },
        });
      },
    });
  },

  _bindEvents() {
    $(window).on('beforeunload', () => {
      PF('ajax-status').trigger('start');

      if (typeof onUnloadCommand === 'function') {
        onUnloadCommand();
      }
    });

    /**
     * Listener to trigger modal close, when clicked on dialog overlay.
     */
    $(document).on('click', '.ui-dialog-mask', (e) => {
      getWidgetVarById(e.currentTarget.id.replace('_modal', '')).hide();
    });

    this.menuButton.off('click').on('click', (e) => {
      if (this.isDesktop()) {
        this.wrapper.removeClass('layout-wrapper-sidebar-mobile-active');
        if (this.wrapper.hasClass('layout-wrapper-sidebar-inactive')) {
          this.wrapper.removeClass('layout-wrapper-sidebar-inactive');
          setPreference('HIDE_SIDEBAR', false);
        } else {
          this.wrapper.addClass('layout-wrapper-sidebar-inactive');
          setPreference('HIDE_SIDEBAR', true);
        }
      } else {
        this.wrapper.removeClass('layout-wrapper-sidebar-inactive');
        this.wrapper.toggleClass('layout-wrapper-sidebar-mobile-active');
      }

      e.preventDefault();
    });
  },

  isDesktop() {
    return window.innerWidth > 1200; // Do not forget to change scss value according
  },

  isTouchDevice() {
    return (('ontouchstart' in window) || (navigator.maxTouchPoints > 0));// eslint-disable-line compat/compat
  },

  resize() {
    if (window.cqApi && typeof window.cqApi.reevaluate === 'function') {
      window.cqApi.reevaluate(false);

      setTimeout(() => {
        window.cqApi.reevaluate(false);
      }, 200);
    }
  },
});

/**
 * PrimeFaces LearnwebMenu component
 */
PrimeFaces.widget.LearnwebMenu = PrimeFaces.widget.BaseWidget.extend({

  init(cfg) {
    this._super(cfg);
    this.menuitemLinks = this.jq.find('.ui-menuitem-link:not(.ui-state-disabled)');

    this.bindEvents();

    this.markCurrentMenuItem();
  },

  bindEvents() {
    // Used for expanding resource filters on devices without hover
    $(document).on('click', '.res-filters .filter', (e) => {
      const $target = $(e.currentTarget);
      $target.toggleClass('ui-state-expand');

      if ($target.hasClass('ui-state-expand')) {
        $(document).one('click', () => {
          $target.removeClass('ui-state-expand');
        });
      }
    });

    this.menuitemLinks.on('click', (e) => {
      const $currentLink = $(e.currentTarget);

      if (e.target.className.indexOf('ui-menuitem-icon-expand') === -1) {
        const href = $currentLink.attr('href');

        if (href && href !== '#') {
          window.location.href = href;
          e.preventDefault();
          return;
        }
      }

      const submenu = $currentLink.parent();
      if (submenu.hasClass('ui-menu-parent')) {
        if (this.isExpanded(submenu)) {
          this.collapseTreeItem(submenu);
        } else {
          this.expandTreeItem(submenu, false);
        }
      }

      e.preventDefault();
    });
  },

  isExpanded(item) {
    return item.children('.ui-menu-list').is(':visible');
  },

  collapseTreeItem(submenu) {
    submenu.find('> .ui-menuitem-link > .ui-menuitem-text').attr('aria-expanded', false);
    submenu.children('.ui-menu-list').attr('aria-hidden', true);

    submenu.children('.ui-menu-list').slideUp('normal', 'easeInOutCirc', () => {
      submenu.removeClass('ui-state-expand');
    });
  },

  expandTreeItem(submenu, restoring) {
    submenu.find('> .ui-menuitem-link > .ui-menuitem-text').attr('aria-expanded', true);
    submenu.children('.ui-menu-list').attr('aria-hidden', false);

    if (restoring) {
      submenu.addClass('ui-state-expand');
    } else {
      submenu.addClass('ui-state-expand');
      submenu.children('.ui-menu-list').hide().slideDown('normal', 'easeInOutCirc');
    }
  },

  markCurrentMenuItem() {
    let currentPath = window.location.href;
    if (currentPath.includes('groups_')) {
      const replace = currentPath.substring(currentPath.indexOf('_'), currentPath.indexOf('.'));
      currentPath = currentPath.replace(replace, '');
    }

    this.menuitemLinks.filter((i, el) => currentPath.indexOf(el.href) === 0).each((i, el) => {
      const $activeMenuLink = $(el);
      const $activeMenuItem = $activeMenuLink.closest('.ui-menuitem');

      $activeMenuLink.addClass('ui-state-active');
      $activeMenuItem[0].scrollIntoView();

      this.expandMenuItemThree($activeMenuItem);
    });
  },

  expandMenuItemThree(submenu) {
    if (!submenu.hasClass('ui-state-empty')) {
      this.expandTreeItem(submenu, true);
    }

    const parentSubmenu = submenu.parent().closest('.ui-menu-parent');
    if (parentSubmenu.length) {
      this.expandMenuItemThree(parentSubmenu);
    }
  },
});

/*
 * Returns the PrimefacesWidget from element ID
 * @param elementId
 * @returns {*}
 */
function getWidgetVarById(elementId) {
  return Object.values(PrimeFaces.widgets).find((widget) => widget && widget.id === elementId);
}

/*
 * Store preferences in user account settings
 */
function setPreference(prefKey, prefValue) {
  setPreferenceRemote([
    { name: 'key', value: prefKey },
    { name: 'value', value: prefValue },
  ]);
}

function handlePopstateResView(e) {
  if (e.state && e.state.fancybox_open === false) {
    $.fancybox.close();

    const state = window.history.state || {};
    delete state.fancybox_open;
    window.history.replaceState(state, null);
  }
}

function openResourceView(items, target, isEdit = false) {
  $.fancybox.open(items, {
    defaultType: 'iframe',
    closeExisting: true,
    arrows: false,
    infobar: false,
    toolbar: false,
    slideClass: 'p-0',
    hash: false,
    iframe: {
      css: {
        height: '100%',
        width: '100%',
      },
    },
    onInit: (instance) => {
      for (let i = 0, l = instance.group.length; i < l; ++i) {
        const el = instance.group[i].opts.$orig;
        if (target.is(el)) {
          instance.currIndex = instance.group[i].index;
          if (isEdit) instance.group[i].src += '&edit=true';
          break;
        }
      }

      const state = window.history.state || {};
      state.fancybox_open = false;
      window.history.replaceState(state, null);
      window.history.pushState({ fancybox_open: true }, null, instance.group[instance.currIndex].src);
    },
    beforeClose: () => {
      if (window.history.state && window.history.state.fancybox_open === true) {
        window.history.back();
      }
    },
  });

  window.addEventListener('popstate', handlePopstateResView, false);
}

/**
 * On document ready events
 */
$(() => {
  // We created a PrimeFaces widget without an active element, so we need manually tell PrimeFaces to run it
  // After that, we can access any method of it by using `PF('learnweb')`
  PrimeFaces.cw('LearnwebTheme', 'learnweb', { id: 'learnweb' });

  const $fbResources = $('[data-resview="default"]');
  if ($.fancybox && $fbResources.length) {
    $fbResources
      .on('click', (e) => e.preventDefault())
      .on('dblclick', (e) => {
        const $target = $(e.currentTarget);
        openResourceView($fbResources, $target);
      });
  }

  // This code is executed after all "on-page-ready" listeners
  $(() => $('.ui-loading').removeClass('ui-loading'));
});

/**
 * Reset center position of Dialog after content is loaded.
 * Override original show method and call `resetPosition` at the end of it.
 */
PrimeFaces.widget.Dialog.prototype.show = (((_show) => function () {
  _show.call(this);
  this.resetPosition();
})(PrimeFaces.widget.Dialog.prototype.show));

/**
 * Update p:linkButton focus styles
 * TODO: remove after PF 9.0 is released
 * https://github.com/primefaces/primefaces/issues/5698
 */
PrimeFaces.widget.LinkButton = PrimeFaces.widget.BaseWidget.extend({
  init(cfg) {
    this._super(cfg);
    this.button = this.jq;
    this.link = this.jq.children('a');

    PrimeFaces.skinButton(this.button);
    this.bindEvents();
  },

  bindEvents() {
    const $this = this;

    if (this.link) {
      this.link.off().on('focus.linkbutton keydown.linkbutton', () => {
        $this.button.addClass('ui-state-focus ui-state-active');
      }).on('blur.linkbutton', () => {
        $this.button.removeClass('ui-state-focus ui-state-active');
      });
    }
  },
});

// noinspection JSUnusedGlobalSymbols
/**
 * This is used for adaptive PrimeFaces Carousel.
 * By default you can set only fixed amount of column and it will be changed only on mobile phones to single column.
 * But this script allows to set dynamic number of column based on the size of first element.
 * To use it, set `breakpoint` property of `p:carousel` to `-1`.
 */
PrimeFaces.widget.Carousel.prototype.refreshDimensions = function () {
  if (this.cfg.breakpoint === -1) {
    const firstItem = this.items.eq(0);
    firstItem.css('width', 'auto');
    const firstItemWidth = firstItem.length ? firstItem.width() : 150; // firstItem.outerWidth(true), firstItem.width()
    const viewportInnerWidth = this.viewport.innerWidth();
    this.columns = Math.floor(viewportInnerWidth / firstItemWidth);
    this.calculateItemWidths();
    this.totalPages = Math.ceil(this.itemsCount / this.columns);
    if (this.totalPages === 1) {
      this.nextNav.hide();
      this.prevNav.hide();
    }
    this.responsiveDropdown.hide();
    this.pageLinks.show();
  } else if ($(window).width() <= this.cfg.breakpoint) {
    this.columns = 1;
    this.calculateItemWidths(this.columns);
    this.totalPages = this.itemsCount;
    this.responsiveDropdown.show();
    this.pageLinks.hide();
  } else {
    this.columns = this.cfg.numVisible;
    this.calculateItemWidths();
    this.totalPages = Math.ceil(this.itemsCount / this.cfg.numVisible);
    this.responsiveDropdown.hide();
    this.pageLinks.show();
  }

  this.page = Math.ceil(this.first / this.columns);
  this.updateNavigators();
  this.itemsContainer.css('left', (-1 * (this.viewport.innerWidth() * this.page)));
};

/**
 * This is a new implementation of old script which changes size of filters list.
 * If list contains more than N items, then all after N will be hidden and 'Show more' will be displayed instead.
 */
PrimeFaces.widget.LimitedList = PrimeFaces.widget.BaseWidget.extend({
  init(cfg) {
    this._super(cfg);

    this.defaultVisibleItems = 5;
    this.targetLists = $('.js-limited-list');
    this.targetLists.each(this._init);
  },

  _init(index, element) {
    const $list = $(element);
    const visibleItems = $list.data('visible-items') || this.defaultVisibleItems;
    const $items = $list.find('li:not(.expand-list)');
    const $expandBtn = $list.find('li.expand-list');

    const totalRecords = $items.length;
    if (visibleItems + 1 < totalRecords) {
      $list.addClass('list-collapsed');
      $expandBtn.show();
      for (let i = visibleItems; i < totalRecords; ++i) {
        $($items[i]).hide();
      }

      $expandBtn.on('click', (e) => {
        $list.removeClass('list-collapsed');
        $expandBtn.hide();
        $items.show();

        e.preventDefault();
        return false;
      });
    }
  },
});
