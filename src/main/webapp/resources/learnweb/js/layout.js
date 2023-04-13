/* global onUnloadCommand, commandSetPreference */

/**
 * PrimeFaces LearnwebTheme Layout
 *
 * This file will be loaded on every page.
 * Include only methods which are required on every page.
 */
PrimeFaces.widget.LearnwebTheme = PrimeFaces.widget.BaseWidget.extend({

  init(cfg) {
    // eslint-disable-next-line no-underscore-dangle
    this._super(cfg);
    this.wrapper = $('.layout-wrapper');

    if (this.wrapper.length === 0) return;
    this.header = this.wrapper.children('.navbar');
    this.sidebar = this.wrapper.children('.layout-sidebar');
    this.menuButton = this.header.find('#menu-button');

    this.sidebarMenuClick = false;

    this.bindEvents();
    this.autoComplete();
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

    $(document.body).off('click.layoutBody').on('click.layoutBody', () => {
      if (!this.sidebarMenuClick && (this.wrapper.hasClass('layout-wrapper-sidebar-mobile-active'))) {
        this.wrapper.removeClass('layout-wrapper-sidebar-mobile-active');
      }

      this.sidebarMenuClick = false;
    });
  },

  autoComplete() {
    const searchField = this.header.find('#navbar_form\\:searchfield');

    if (typeof URLSearchParams === 'function') {
      const reqQuery = new URLSearchParams(window.location.search).get('query');
      if (reqQuery) {
        searchField.val(reqQuery);
      }
    }

    let currentRequest;
    const myMarket = PrimeFaces.settings.locale;
    searchField.autoComplete({
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

  isDesktop() {
    return window.innerWidth > 1200; // Do not forget to change scss value according
  },

  isTouchDevice() {
    return (('ontouchstart' in window) || (navigator.maxTouchPoints > 0));
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
    // eslint-disable-next-line no-underscore-dangle
    this._super(cfg);

    this.menu = this.jq;
    this.menulinks = this.menu.find('a');

    this.bindEvents();
    this.expandActiveItems();
  },

  bindEvents() {
    this.menulinks.off('click.menu').on('click.menu', (e) => {
      const link = $(e.currentTarget);
      const item = link.parent('li');
      const submenu = item.children('ul');
      const hasAction = link.href !== '#';
      const clickOnIcon = e.target.nodeName === 'I';

      if (item.hasClass('active-menuitem')) {
        if (submenu.length) {
          submenu.slideUp(400, () => {
            item.removeClass('active-menuitem');
          });
        }
      } else if (submenu.length && (clickOnIcon || !hasAction)) {
        this.deactivateItems(item.siblings(), true);
        this.activate(item, true);
      }

      if (submenu.length && (!hasAction || clickOnIcon)) {
        e.preventDefault();
      }
    });
  },

  activate(item, animate) {
    const submenu = item.children('ul');
    item.addClass('active-menuitem');

    if (submenu.length) {
      if (animate) {
        submenu.slideDown();
      } else {
        submenu.show();
      }
    }
  },

  deactivate(item) {
    const submenu = item.children('ul');
    item.removeClass('active-menuitem');

    if (submenu.length) {
      submenu.hide();
    }
  },

  deactivateItems(items, animate) {
    for (let i = 0; i < items.length; i++) {
      const item = items.eq(i);
      const submenu = item.children('ul');

      if (submenu.length) {
        if (item.hasClass('active-menuitem')) {
          item.removeClass('active-menuitem');
          item.find('.ink').remove();

          if (animate) {
            submenu.slideUp('normal', () => {
              submenu.parent().find('.active-menuitem').each((ignore, el) => {
                this.deactivate($(el));
              });
            });
          } else {
            submenu.hide();
            item.find('.active-menuitem').each((ignore, el) => {
              this.deactivate($(el));
            });
          }
        } else {
          item.find('.active-menuitem').each((ignore, el) => {
            this.deactivate($(el));
          });
        }
      } else if (item.hasClass('active-menuitem')) {
        this.deactivate(item);
      }
    }
  },

  expandActiveItems() {
    let currentPath = window.location.href;
    if (currentPath.includes('groups_')) {
      const replace = currentPath.substring(currentPath.indexOf('_'), currentPath.indexOf('.'));
      currentPath = currentPath.replace(replace, '');
    }

    this.menulinks.filter((i, el) => currentPath.indexOf(el.href) === 0).each((i, el) => {
      const link = $(el);

      link.parentsUntil('.layout-menu', 'li').each((ignore, li) => {
        this.activate($(li));
      });
    });
  },
});

function updateSearchQuery(param, value) {
  const urlParams = new URLSearchParams(window.location.search);
  urlParams.set(param, value);
  window.history.pushState({ [param]: value }, null, `${window.location.pathname}?${urlParams}`);
}

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
  commandSetPreference([
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

  const $fbResources = $('[data-resview="single"]');
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
 * Reset center position of Dialog after content is modified.
 */
PrimeFaces.widget.Dialog.prototype.show = (((_show) => function () {
  _show.call(this);

  const $this = this;
  if (!this.contentResizeObserver) {
    this.contentResizeObserver = new ResizeObserver(() => {
      if ($this.cfg.fitViewport) {
        $this.fitViewport();
      }

      if ($this.isVisible()) {
        // instant reinit position
        $this.initPosition();
      } else {
        // reset, so the dialog will be positioned again when showing the dialog next time
        $this.positionInitialized = false;
      }
    });
  }

  this.contentResizeObserver.observe(this.content[0]);
})(PrimeFaces.widget.Dialog.prototype.show));

PrimeFaces.widget.Dialog.prototype.hide = (((_hide) => function () {
  _hide.call(this);

  if (this.contentResizeObserver) {
    this.contentResizeObserver.disconnect();
  }
})(PrimeFaces.widget.Dialog.prototype.hide));

/**
 * This is a new implementation of old script which changes size of filters list.
 * If list contains more than N items, then all after N will be hidden and 'Show more' will be displayed instead.
 */
PrimeFaces.widget.LimitedList = PrimeFaces.widget.BaseWidget.extend({
  init(cfg) {
    // eslint-disable-next-line no-underscore-dangle
    this._super(cfg);

    this.defaultVisibleItems = 5;
    this.targetLists = $('.js-limited-list');
    this.targetLists.each(this.initList);
  },

  initList(index, element) {
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

if (PrimeFaces.widget.InputSwitch) {
  PrimeFaces.widget.InputSwitch = PrimeFaces.widget.InputSwitch.extend({

    init(cfg) {
      // eslint-disable-next-line no-underscore-dangle
      this._super(cfg);

      if (this.input.prop('checked')) {
        this.jq.addClass('ui-inputswitch-checked');
      }
    },

    toggle() {
      if (this.input.prop('checked')) {
        this.uncheck();
        setTimeout(() => {
          this.jq.removeClass('ui-inputswitch-checked');
        }, 100);
      } else {
        this.check();
        setTimeout(() => {
          this.jq.addClass('ui-inputswitch-checked');
        }, 100);
      }
    },
  });
}

if (PrimeFaces.widget.Menu) {
  PrimeFaces.widget.Menu.prototype.align = ((() => function () {
    this.jq.css({ left: 0, top: 0, 'transform-origin': 'center top' }).position(this.cfg.pos);
  })(PrimeFaces.widget.Menu.prototype.align));
}
