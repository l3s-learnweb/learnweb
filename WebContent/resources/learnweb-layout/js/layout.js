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

        this.mainPane = this.body.find('.layout-main-pane');
        this.rightPane = this.body.find('.layout-right-pane');
        this.isRightPaneOpen = false;

        this._bindEvents();
    },

    _bindEvents: function () {
        var $this = this;

        $(window).on('beforeunload', function() {
            if (typeof onUnloadCommand === 'function') {
                onUnloadCommand();
            }
        });

        $this.overlay.on('mouseup', function(e) {
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

            e.preventDefault();
        });

        if ($this.rightPane.find('#right_pane_content div').length) {
            $this.showRightPane();
        }
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
    }
});

/**
 * PrimeFaces LearnwebMenu component
 */
PrimeFaces.widget.LearnwebMenu = PrimeFaces.widget.BaseWidget.extend({

    init: function (cfg) {
        this._super(cfg);
        this.headers = this.jq.find('> .ui-lwmenu-panel > .ui-lwmenu-panel-header:not(.ui-state-disabled)');
        this.menuContent = this.jq.find('> .ui-lwmenu-panel > .ui-lwmenu-panel-content');
        this.menuitemLinks = this.jq.find('.ui-menuitem-link:not(.ui-state-disabled)');
        this.menuText = this.menuitemLinks.find('.ui-menuitem-text');
        this.expandedNodes = [];

        //keyboard support
        this.focusedItem = null;
        this.menuText.attr('tabindex', -1);

        //ScreenReader support
        this.menuText.attr('role', 'menuitem');

        this.bindEvents();

        if (this.cfg.stateful) {
            this.stateKey = 'lwMenu-' + this.id;
        }

        // this.restoreState();
        this.markCurrentMenuItem();
    },

    bindEvents: function () {
        var $this = this;

        this.headers.click(function (e) {
            if (e.target.tagName === "A") {
                var href = $(e.target).attr('href');

                if (href && href !== '#') {
                    window.location.href = href;
                    e.preventDefault();
                    return false;
                }
            }

            var header = $(this);

            if (!$this.cfg.multiple) {
                $this.collapseActiveSibling(header);
            }

            if (header.hasClass('ui-state-active'))
                $this.collapseRootSubmenu($(this));
            else
                $this.expandRootSubmenu($(this), false);

            $this.removeFocusedItem();
            header.focus();
            e.preventDefault();
            return false;
        });

        this.menuitemLinks.click(function (e) {
            var currentLink = $(this);

            if (e.target.className.indexOf('ui-lwmenu-icon') === -1) {
                $this.focusItem(currentLink.closest('.ui-menuitem'));
                var href = currentLink.attr('href');

                if (href && href !== '#') {
                    window.location.href = href;
                    e.preventDefault();
                    return;
                }
            }

            if (currentLink.parent().hasClass("ui-menu-parent")) {
                var submenu = currentLink.parent(),
                    submenuList = currentLink.next();

                if (submenuList.is(':visible'))
                    $this.collapseTreeItem(submenu);
                else
                    $this.expandTreeItem(submenu, false);
            }

            e.preventDefault();
        });

        this.bindKeyEvents();
    },

    bindKeyEvents: function () {
        var $this = this;

        if (PrimeFaces.env.isIE()) {
            this.focusCheck = false;
        }

        this.headers.on('focus.lwmenu', function () {
            $(this).addClass('ui-menuitem-outline');
        })
            .on('blur.lwmenu', function () {
                $(this).removeClass('ui-menuitem-outline');
            })
            .on('keydown.lwmenu', function (e) {
                var keyCode = $.ui.keyCode,
                    key = e.which;

                if (key === keyCode.SPACE || key === keyCode.ENTER) {
                    $(this).trigger('click');
                    e.preventDefault();
                }
            });

        this.menuContent.on('mousedown.lwmenu', function (e) {
            if ($(e.target).is(':not(:input:enabled)')) {
                e.preventDefault();
            }
        }).on('focus.lwmenu', function () {
            if (!$this.focusedItem) {
                $this.focusItem($this.getFirstItemOfContent($(this)));
                if (PrimeFaces.env.isIE()) {
                    $this.focusCheck = false;
                }
            }
        });

        this.menuContent.off('keydown.lwmenu blur.lwmenu').on('keydown.lwmenu', function (e) {
            if (!$this.focusedItem) {
                return;
            }

            var keyCode = $.ui.keyCode;

            switch (e.which) {
                case keyCode.LEFT:
                    if ($this.isExpanded($this.focusedItem)) {
                        $this.focusedItem.children('.ui-menuitem-link').trigger('click');
                    } else {
                        var parentListOfItem = $this.focusedItem.closest('ul.ui-menu-list');

                        if (parentListOfItem.parent().is(':not(.ui-lwmenu-panel-content)')) {
                            $this.focusItem(parentListOfItem.closest('li.ui-menuitem'));
                        }
                    }

                    e.preventDefault();
                    break;

                case keyCode.RIGHT:
                    if ($this.focusedItem.hasClass('ui-menu-parent') && !$this.isExpanded($this.focusedItem)) {
                        $this.focusedItem.children('.ui-menuitem-link').trigger('click');
                    }
                    e.preventDefault();
                    break;

                case keyCode.UP:
                    var itemToFocus = null,
                        prevItem = $this.focusedItem.prev();

                    if (prevItem.length) {
                        itemToFocus = prevItem.find('li.ui-menuitem:visible:last');
                        if (!itemToFocus.length) {
                            itemToFocus = prevItem;
                        }
                    } else {
                        itemToFocus = $this.focusedItem.closest('ul').parent('li');
                    }

                    if (itemToFocus.length) {
                        $this.focusItem(itemToFocus);
                    }

                    e.preventDefault();
                    break;

                case keyCode.DOWN:
                    var itemToFocus = null,
                        firstVisibleChildItem = $this.focusedItem.find('> ul > li:visible:first');

                    if (firstVisibleChildItem.length) {
                        itemToFocus = firstVisibleChildItem;
                    } else if ($this.focusedItem.next().length) {
                        itemToFocus = $this.focusedItem.next();
                    } else {
                        if ($this.focusedItem.next().length === 0) {
                            itemToFocus = $this.searchDown($this.focusedItem);
                        }
                    }

                    if (itemToFocus && itemToFocus.length) {
                        $this.focusItem(itemToFocus);
                    }

                    e.preventDefault();
                    break;

                case keyCode.ENTER:
                case keyCode.SPACE:
                    var currentLink = $this.focusedItem.children('.ui-menuitem-link');
                    //IE fix
                    setTimeout(function () {
                        currentLink.trigger('click');
                    }, 1);
                    $this.jq.blur();

                    var href = currentLink.attr('href');
                    if (href && href !== '#') {
                        window.location.href = href;
                    }
                    e.preventDefault();
                    break;

                case keyCode.TAB:
                    if ($this.focusedItem) {
                        if (PrimeFaces.env.isIE()) {
                            $this.focusCheck = true;
                        }
                        $(this).focus();
                    }
                    break;
            }
        }).on('blur.lwmenu', function () {
            if (PrimeFaces.env.isIE() && !$this.focusCheck) {
                return;
            }

            $this.removeFocusedItem();
        });

        var clickNS = 'click.' + this.id;
        //remove focusedItem when document is clicked
        $(document.body).off(clickNS).on(clickNS, function (event) {
            if (!$(event.target).closest('.ui-lwmenu').length) {
                $this.removeFocusedItem();
            }
        });
    },

    collapseActiveSibling: function (header) {
        this.collapseRootSubmenu(header.parent().siblings().children('.ui-lwmenu-panel-header.ui-state-active').eq(0));
    },

    searchDown: function (item) {
        var nextOfParent = item.closest('ul').parent('li').next(),
            itemToFocus = null;

        if (nextOfParent.length) {
            itemToFocus = nextOfParent;
        } else if (item.closest('ul').parent('li').length === 0) {
            itemToFocus = item;
        } else {
            itemToFocus = this.searchDown(item.closest('ul').parent('li'));
        }

        return itemToFocus;
    },

    getFirstItemOfContent: function (content) {
        return content.find('> .ui-menu-list > .ui-menuitem:visible:first-child');
    },

    getItemText: function (item) {
        return item.find('> .ui-menuitem-link > span.ui-menuitem-text');
    },

    focusItem: function (item) {
        this.removeFocusedItem();
        this.getItemText(item).addClass('ui-menuitem-outline').focus();
        this.focusedItem = item;
    },

    removeFocusedItem: function () {
        if (this.focusedItem) {
            this.getItemText(this.focusedItem).removeClass('ui-menuitem-outline');
            this.focusedItem = null;
        }
    },

    isExpanded: function (item) {
        return item.children('ul.ui-menu-list').is(':visible');
    },

    collapseRootSubmenu: function (header) {
        header.parent().removeClass('ui-state-expand');
        header.attr('aria-expanded', false).removeClass('ui-state-active');

        var panel = header.next();
        panel.attr('aria-hidden', true).slideUp('normal', 'easeInOutCirc');
        this.removeAsExpanded(panel);
    },

    expandRootSubmenu: function (header, restoring) {
        header.parent().addClass('ui-state-expand');
        header.attr('aria-expanded', true).addClass('ui-state-active');

        var panel = header.next();
        if (restoring) {
            panel.attr('aria-hidden', false);
        } else {
            panel.attr('aria-hidden', false).slideDown('normal', 'easeInOutCirc');

            this.addAsExpanded(panel);
        }
    },

    expandTreeItem: function (submenu, restoring) {
        submenu.addClass('ui-state-expand');
        submenu.find('> .ui-menuitem-link > .ui-menuitem-text').attr('aria-expanded', true);

        if (!restoring) {
            this.addAsExpanded(submenu);
        }
    },

    collapseTreeItem: function (submenu) {
        submenu.removeClass('ui-state-expand');
        submenu.find('> .ui-menuitem-link > .ui-menuitem-text').attr('aria-expanded', false);

        this.removeAsExpanded(submenu);
    },

    saveState: function () {
        if (this.cfg.stateful) {
            var expandedNodeIds = this.expandedNodes.join(',');

            PrimeFaces.setCookie(this.stateKey, expandedNodeIds, {path: '/'});
        }
    },

    restoreState: function () {
        var expandedNodeIds = null;

        if (this.cfg.stateful) {
            expandedNodeIds = PrimeFaces.getCookie(this.stateKey);
        }

        if (expandedNodeIds) {
            this.collapseAll();
            this.expandedNodes = expandedNodeIds.split(',');

            for (var i = 0; i < this.expandedNodes.length; i++) {
                var element = $(PrimeFaces.escapeClientId(this.expandedNodes[i]));
                if (element.is('.ui-lwmenu-panel-content'))
                    this.expandRootSubmenu(element.prev(), true);
                else if (element.is('li.ui-menu-parent'))
                    this.expandTreeItem(element, true);
            }
        } else {
            this.expandedNodes = [];
            var activeHeaders = this.headers.filter('.ui-state-active'),
                activeTreeSubmenus = this.jq.find('.ui-menu-parent > .ui-menu-list:not(.ui-helper-hidden)');

            for (var i = 0; i < activeHeaders.length; i++) {
                this.expandedNodes.push(activeHeaders.eq(i).next().attr('id'));
            }

            for (var i = 0; i < activeTreeSubmenus.length; i++) {
                this.expandedNodes.push(activeTreeSubmenus.eq(i).parent().attr('id'));
            }
        }
    },

    markCurrentMenuItem: function () {
        var currentPath = window.location.href;
        var activeMenuLinks = this.menuitemLinks.filter(function() {
            return currentPath.indexOf(this.href) === 0;
        });

        if (activeMenuLinks.length) {
            var that = this;
            activeMenuLinks.each(function () {
                var activeMenuLink = $(this);
                var activeMenuItem = activeMenuLink.closest('.ui-menuitem');

                activeMenuLink.addClass('ui-state-active');
                that.expandMenuItemThree(activeMenuItem);
            });
        }
    },

    expandMenuItemThree: function (submenu) {
        if (!submenu.hasClass('ui-state-empty')) {
            this.expandTreeItem(submenu);
        }

        var parentSubmenu = submenu.parent().closest('.ui-menu-parent');
        if (parentSubmenu.length) {
            this.expandMenuItemThree(parentSubmenu);
        }
    },

    removeAsExpanded: function (element) {
        var id = element.attr('id');

        this.expandedNodes = $.grep(this.expandedNodes, function (value) {
            return value !== id;
        });

        // this.saveState();
    },

    addAsExpanded: function (element) {
        var id = element.attr('id');
        if (id) {
            this.expandedNodes.push(element.attr('id'));
            // this.saveState();
        }
    },

    clearState: function () {
        if (this.cfg.stateful) {
            PrimeFaces.deleteCookie(this.stateKey, {path: '/'});
        }
    },

    collapseAll: function () {
        this.headers.filter('.ui-state-active').each(function () {
            var header = $(this);
            header.removeClass('ui-state-active');
            header.next().addClass('ui-helper-hidden');
        });

        this.jq.find('.ui-menu-parent > .ui-menu-list:not(.ui-helper-hidden)').each(function () {
            $(this).addClass('ui-helper-hidden');
        });
    }
});

/**
 * On document ready events
 */
$(function () {
    PrimeFaces.cw("LearnwebTheme", "learnweb", {id: "learnweb"});
});

/**
 * Reproducible in PrimeFaces 7.0
 * https://github.com/primefaces/primefaces/issues/5035
 * Fix for Primefaces issue when the offset was set wrong due to scrollbar which appearing after element is visible but still not aligned.
 */
PrimeFaces.widget.Menu.prototype.show = function() {
    this.align();
    this.jq.css({"z-index": ++PrimeFaces.zindex}).show();
};

/**
 *
 */
PrimeFaces.widget.Carousel.prototype.refreshDimensions = function() {
    if(this.cfg.breakpoint === -1) {
        var firstItem = this.items.eq(0);
        firstItem.css('width', 'auto');
        var firstItemWidth = firstItem.length ? firstItem.width() : 150; // firstItem.outerWidth(true), firstItem.width()
        var viewportInnerWidth = this.viewport.innerWidth();
        this.columns = Math.floor(viewportInnerWidth / firstItemWidth);
        this.calculateItemWidths();
        this.totalPages = Math.ceil(this.itemsCount / this.columns);
        this.responsiveDropdown.hide();
        this.pageLinks.show();
    } else {
        var win = $(window);
        if(win.width() <= this.cfg.breakpoint) {
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

    this.page = parseInt(this.first / this.columns);
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
//         $("body").css("cursor", "progress");
//         ajaxInProgress = setTimeout(function () {
//             this.jq.children().hide().filter(this.toFacetId(event)).show();
//         }.bind(this), 200);
//     } else {
//         $("body").css("cursor", "default");
//         clearTimeout(ajaxInProgress);
//         ajaxInProgress = null;
//         this.jq.children().hide().filter(this.toFacetId(event)).show();
//     }
// };

PrimeFaces.widget.LimitedList = PrimeFaces.widget.BaseWidget.extend({
    init: function (cfg) {
        this._super(cfg);

        this.targetLists = $('.js-limited-list');
        this.targetLists.each(this._init);
    },

    _init: function () {
        var $list = $(this);
        var visibleItems = $list.data('visible-items');
        var $items = $list.find('li:not(.expand-list)');
        var $expandBtn = $list.find('li.expand-list');

        var totalRecords = $items.length;
        if (visibleItems < totalRecords) {
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
