/****************************************************************
 This file will be loaded on every page.
 Include only methods which are required on every page.
 ****************************************************************/

/** @external logQuerySuggestion */
/** @external onUnloadCommand */
/** @external setPreferenceRemote */

/**
 * PrimeFaces LearnwebTheme Layout
 */
PrimeFaces.widget.LearnwebTheme = PrimeFaces.widget.BaseWidget.extend({

    init: function (cfg) {
        this._super(cfg);
        this.body = $(document.body);
        this.overlay = this.body.find('.layout-overlay');
        this.wrapper = this.body.children('.layout-wrapper');

        this.header = this.wrapper.children('.layout-header');
        this.menuButton = this.header.find('#menu-button');

        // this.mainPane = this.body.find('.layout-main-pane');
        this.rightPane = this.body.find('.layout-right-pane');
        this.isRightPaneOpen = false;

        this._bindEvents();

        var myMarket = PrimeFaces.settings.locale;
        this.header.find('#searchfield').autoComplete({
            source: function (term, response) {
                try {
                    xhr.abort();
                } catch (e) {
                }
                // noinspection JSIgnoredPromiseFromCall
                $.ajax({
                    url: 'https://api.bing.com/osjson.aspx?JsonType=callback&JsonCallback=?',
                    data: {
                        'query': term,
                        'market': myMarket
                    },
                    dataType: 'jsonp',
                    success: function (data) {
                        var suggestions = [];
                        $.each(data[1], function (i, val) {
                            suggestions.push(val);
                        });
                        response(suggestions);

                        var logQuerySuggestionAsync = function () {
                            logQuerySuggestion([
                                {name: 'query', value: term},
                                {name: 'market', value: myMarket},
                                {name: 'suggestions', value: suggestions}
                            ]);
                        };
                        setTimeout(logQuerySuggestionAsync, 0);
                    }
                });
            }
        });
    },

    _bindEvents: function () {
        var $this = this;

        $(window).on('beforeunload', function () {
            if (typeof onUnloadCommand === 'function') {
                onUnloadCommand();
            }
        });

        /**
         * Listener to trigger modal close, when clicked on dialog overlay.
         */
        $(document).on('click', '.ui-dialog-mask', function () {
            $this.getWidgetVarById(this.id.replace('_modal', '')).hide();
        });

        $this.overlay.on('mouseup', function () {
            if ($this.isRightPaneOpen) {
                $this.hideRightPane();
            }
        });

        $this.menuButton.off('click').on('click', function (e) {
            if ($this.isDesktop()) {
                $this.wrapper.toggleClass('layout-wrapper-sidebar-inactive');
                $this.wrapper.removeClass('layout-wrapper-sidebar-mobile-active');
            } else {
                $this.wrapper.toggleClass('layout-wrapper-sidebar-mobile-active');
                $this.wrapper.removeClass('layout-wrapper-sidebar-inactive');
            }

            e.preventDefault();
        });

        $this.rightPane.on('click', '.layout-right-pane-close', function (e) {
            $this.hideRightPane();
            $this.updateSearchParams({resource_id: null});

            e.preventDefault();
        });

        if ($this.rightPane.find('#right_pane_content div').length) {
            $this.showRightPane();
        }
    },

    updateSearchParams: function (searchParams, cleanExisting) {
        var sp = new URLSearchParams(cleanExisting ? undefined : window.location.search);

        Object.keys(searchParams).forEach(function (key) {
            var value = searchParams[key];

            if (value === null) {
                sp.delete(key);
            } else {
                sp.set(key, value);
            }
        });

        var newUrl = window.location.protocol + "//" + window.location.host + window.location.pathname + '?' + sp.toString();
        window.history.pushState({url: newUrl}, '', newUrl);
    },

    showRightPane: function () {
        this.body.addClass('right-pane-open');
        this.isRightPaneOpen = true;
        this.resize();
    },

    hideRightPane: function () {
        this.body.removeClass('right-pane-open');
        this.isRightPaneOpen = false;
        this.resize();
    },

    isDesktop: function () {
        return window.innerWidth > 1200; // Do not forget to change scss value according
    },

    resize: function () {
        if (window.cqApi && typeof window.cqApi.reevaluate === 'function') {
            window.cqApi.reevaluate(false);

            setTimeout(function () {
                window.cqApi.reevaluate(false);
            }, 200);
        }
    },

    /**
     * Returns the PrimefacesWidget from element ID
     * @param elementId
     * @returns {*}
     */
    getWidgetVarById: function (elementId) {
        for (var propertyName in PrimeFaces.widgets) {
            // noinspection JSUnfilteredForInLoop
            var widget = PrimeFaces.widgets[propertyName];
            if (widget && widget.elementId === elementId) {
                return widget;
            }
        }
    }
});

/**
 * PrimeFaces LearnwebMenu component
 */
PrimeFaces.widget.LearnwebMenu = PrimeFaces.widget.BaseWidget.extend({

    init: function (cfg) {
        this._super(cfg);
        this.menuitemLinks = this.jq.find('.ui-menuitem-link:not(.ui-state-disabled)');

        this.bindEvents();

        this.markCurrentMenuItem();
    },

    bindEvents: function () {
        var $this = this;

        this.menuitemLinks.on('click', function (e) {
            var currentLink = $(this);

            if (e.target.className.indexOf('ui-menuitem-icon-expand') === -1) {
                var href = currentLink.attr('href');

                if (href && href !== '#') {
                    window.location.href = href;
                    e.preventDefault();
                    return;
                }
            }

            var submenu = currentLink.parent();
            if (submenu.hasClass('ui-menu-parent')) {
                if ($this.isExpanded(submenu)) {
                    $this.collapseTreeItem(submenu);
                } else {
                    $this.expandTreeItem(submenu, false);
                }
            }

            e.preventDefault();
        });
    },

    isExpanded: function (item) {
        return item.children('.ui-menu-list').is(':visible');
    },

    collapseTreeItem: function (submenu) {
        submenu.find('> .ui-menuitem-link > .ui-menuitem-text').attr('aria-expanded', false);
        submenu.children('.ui-menu-list').attr('aria-hidden', true);

        submenu.children('.ui-menu-list').slideUp('normal', 'easeInOutCirc', function () {
            submenu.removeClass('ui-state-expand');
        });
    },

    expandTreeItem: function (submenu, restoring) {
        submenu.find('> .ui-menuitem-link > .ui-menuitem-text').attr('aria-expanded', true);
        submenu.children('.ui-menu-list').attr('aria-hidden', false);

        if (restoring) {
            submenu.addClass('ui-state-expand');
        } else {
            submenu.addClass('ui-state-expand');
            submenu.children('.ui-menu-list').hide().slideDown('normal', 'easeInOutCirc');
        }
    },

    markCurrentMenuItem: function () {
        var $this = this;

        var currentPath = window.location.href;
        this.menuitemLinks.filter(function () {
            return currentPath.indexOf(this.href) === 0;
        }).each(function () {
            var $activeMenuLink = $(this);
            var $activeMenuItem = $activeMenuLink.closest('.ui-menuitem');

            $activeMenuLink.addClass('ui-state-active');
            $activeMenuItem[0].scrollIntoView();

            $this.expandMenuItemThree($activeMenuItem);
        });
    },

    expandMenuItemThree: function (submenu) {
        if (!submenu.hasClass('ui-state-empty')) {
            this.expandTreeItem(submenu, true);
        }

        var parentSubmenu = submenu.parent().closest('.ui-menu-parent');
        if (parentSubmenu.length) {
            this.expandMenuItemThree(parentSubmenu);
        }
    }
});

/**
 * Method required for the search field
 * TODO: why do we need it?
 */
function removeViewState(searchForm) {
    $(searchForm).find("[name='javax.faces.ViewState']").remove();
}

/**
 * TODO: Find better way to call it
 */
function updateCarousel2() {
    PrimeFaces.cw('LimitedList', 'me', {id: 'learnweb'});
}

/*
 * Store preferences in user account settings
 */
function setPreference(prefKey, prefValue) {
    setPreferenceRemote([
        {name: 'key', value: prefKey},
        {name: 'value', value: prefValue}
    ]);
}

/**
 * On document ready events
 */
$(function () {
    // We created a PrimeFaces widget without an active element, so we need manually tell PrimeFaces to run it
    // After that, we can access any method of it by using `PF('learnweb')`, like `PF('learnweb').showRightPane();`
    PrimeFaces.cw('LearnwebTheme', 'learnweb', {id: 'learnweb'});
});

/**
 * Reset center position of Dialog after content is loaded.
 */
PrimeFaces.widget.Dialog = PrimeFaces.widget.Dialog.extend({
    show: function () {
        this._super();
        this.resetPosition();
    }
});

/**
 * Reproducible in PrimeFaces 7.0
 * https://github.com/primefaces/primefaces/issues/5035 (the issue was resolved and should be released in PF 7.1)
 * Fix for PrimeFaces issue when the offset was set wrong due to scrollbar which appearing after element is visible but still not aligned.
 */
PrimeFaces.widget.Menu.prototype.show = function () {
    this.align();
    this.jq.css({'z-index': ++PrimeFaces.zindex}).show();
};

// noinspection JSUnusedGlobalSymbols
/**
 * This is used for adaptive PrimeFaces Carousel.
 * By default you can set only fixed amount of column and it will be changed only on mobile phones to single column.
 * But this script allows to set dynamic number of column based on the size of first element.
 * To use it, set `breakpoint` property of `p:carousel` to `-1`.
 */
PrimeFaces.widget.Carousel.prototype.refreshDimensions = function () {
    if (this.cfg.breakpoint === -1) {
        var firstItem = this.items.eq(0);
        firstItem.css('width', 'auto');
        var firstItemWidth = firstItem.length ? firstItem.width() : 150; // firstItem.outerWidth(true), firstItem.width()
        var viewportInnerWidth = this.viewport.innerWidth();
        this.columns = Math.floor(viewportInnerWidth / firstItemWidth);
        this.calculateItemWidths();
        this.totalPages = Math.ceil(this.itemsCount / this.columns);
        if (this.totalPages === 1) {
            this.nextNav.hide();
            this.prevNav.hide();
        }
        this.responsiveDropdown.hide();
        this.pageLinks.show();
    } else {
        var win = $(window);
        if (win.width() <= this.cfg.breakpoint) {
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
    }

    this.page = Math.ceil(this.first / this.columns);
    this.updateNavigators();
    this.itemsContainer.css('left', (-1 * (this.viewport.innerWidth() * this.page)));
};

// /**
//  * Uncomment if we don't want to show overlay for fast requests
//  */
// var ajaxInProgress;
// PrimeFaces.widget.AjaxStatus.prototype.trigger = function(event, args) {
//     var callback = this.cfg[event];
//     if(callback) {
//         callback.apply(document, args);
//     }
//
//     if (event === 'start') {
//         $('body').css('cursor', 'progress');
//         ajaxInProgress = setTimeout(function () {
//             this.jq.children().hide().filter(this.toFacetId(event)).show();
//         }.bind(this), 200);
//     } else {
//         $('body').css('cursor', 'default');
//         clearTimeout(ajaxInProgress);
//         ajaxInProgress = null;
//         this.jq.children().hide().filter(this.toFacetId(event)).show();
//     }
// };

/**
 * This is a new implementation of old script which changes size of filters list.
 * If list contains more than N items, then all after N will be hidden and 'Show more' will be displayed instead.
 */
PrimeFaces.widget.LimitedList = PrimeFaces.widget.BaseWidget.extend({
    init: function (cfg) {
        this._super(cfg);

        this.defaultVisibleItems = 5;
        this.targetLists = $('.js-limited-list');
        this.targetLists.each(this._init);
    },

    _init: function () {
        var $list = $(this);
        var visibleItems = $list.data('visible-items') || this.defaultVisibleItems;
        var $items = $list.find('li:not(.expand-list)');
        var $expandBtn = $list.find('li.expand-list');

        var totalRecords = $items.length;
        if (visibleItems + 1 < totalRecords) {
            $list.addClass('list-collapsed');
            $expandBtn.show();
            for (var i = visibleItems; i < totalRecords; ++i) {
                $($items[i]).hide();
            }

            $expandBtn.on('click', function (e) {
                $list.removeClass('list-collapsed');
                $expandBtn.hide();
                $items.show();

                e.preventDefault();
                return false;
            });
        }
    }
});
