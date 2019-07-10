PrimeFaces.widget.LearnwebMenu = PrimeFaces.widget.BaseWidget.extend({

    init: function(cfg) {
        this._super(cfg);
        this.headers = this.jq.find('> .ui-lwmenu-panel > .ui-lwmenu-panel-header:not(.ui-state-disabled)');
        this.menuContent = this.jq.find('> .ui-lwmenu-panel > .ui-lwmenu-panel-content');
        this.menuitemLinks = this.jq.find('.ui-menuitem-link:not(.ui-state-disabled)');
        this.menuText = this.menuitemLinks.find('.ui-menuitem-text');

        //keyboard support
        this.focusedItem = null;
        this.menuText.attr('tabindex', -1);

        //ScreenReader support
        this.menuText.attr('role', 'menuitem');

        this.bindEvents();

        if(this.cfg.stateful) {
            this.stateKey = 'lwMenu-' + this.id;
        }

        this.restoreState();
    },

    bindEvents: function() {
        var $this = this;

        this.headers.mouseover(function() {
            var element = $(this);
            if(!element.hasClass('ui-state-active')) {
                element.addClass('ui-state-hover');
            }
        }).mouseout(function() {
            var element = $(this);
            if(!element.hasClass('ui-state-active')) {
                element.removeClass('ui-state-hover');
            }
        }).click(function(e) {
            if (e.target.tagName === "A") {
                var href = $(e.target).attr('href');

                if(href && href !== '#') {
                    window.location.href = href;
                    e.preventDefault();
                    return;
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
        });

        this.menuitemLinks.mouseover(function() {
            $(this).addClass('ui-state-hover');
        }).mouseout(function() {
            $(this).removeClass('ui-state-hover');
        }).click(function(e) {
            var currentLink = $(this);

            if (e.target.className.indexOf('ui-lwmenu-icon') === -1) {
                $this.focusItem(currentLink.closest('.ui-menuitem'));
                var href = currentLink.attr('href');

                if(href && href !== '#') {
                    window.location.href = href;
                    e.preventDefault();
                    return;
                }
            }

            if (currentLink.parent().hasClass("ui-menu-parent")) {
                var submenu = currentLink.parent(),
                    submenuList = currentLink.next();

                if(submenuList.is(':visible'))
                    $this.collapseTreeItem(submenu);
                else
                    $this.expandTreeItem(submenu, false);
            }

            e.preventDefault();
        });

        this.bindKeyEvents();
    },

    bindKeyEvents: function() {
        var $this = this;

        if(PrimeFaces.env.isIE()) {
            this.focusCheck = false;
        }

        this.headers.on('focus.lwmenu', function(){
            $(this).addClass('ui-menuitem-outline');
        })
            .on('blur.lwmenu', function(){
                $(this).removeClass('ui-menuitem-outline ui-state-hover');
            })
            .on('keydown.lwmenu', function(e) {
                var keyCode = $.ui.keyCode,
                    key = e.which;

                if(key === keyCode.SPACE || key === keyCode.ENTER) {
                    $(this).trigger('click');
                    e.preventDefault();
                }
            });

        this.menuContent.on('mousedown.lwmenu', function(e) {
            if($(e.target).is(':not(:input:enabled)')) {
                e.preventDefault();
            }
        }).on('focus.lwmenu', function(){
            if(!$this.focusedItem) {
                $this.focusItem($this.getFirstItemOfContent($(this)));
                if(PrimeFaces.env.isIE()) {
                    $this.focusCheck = false;
                }
            }
        });

        this.menuContent.off('keydown.lwmenu blur.lwmenu').on('keydown.lwmenu', function(e) {
            if(!$this.focusedItem) {
                return;
            }

            var keyCode = $.ui.keyCode;

            switch(e.which) {
                case keyCode.LEFT:
                    if($this.isExpanded($this.focusedItem)) {
                        $this.focusedItem.children('.ui-menuitem-link').trigger('click');
                    }
                    else {
                        var parentListOfItem = $this.focusedItem.closest('ul.ui-menu-list');

                        if(parentListOfItem.parent().is(':not(.ui-lwmenu-panel-content)')) {
                            $this.focusItem(parentListOfItem.closest('li.ui-menuitem'));
                        }
                    }

                    e.preventDefault();
                    break;

                case keyCode.RIGHT:
                    if($this.focusedItem.hasClass('ui-menu-parent') && !$this.isExpanded($this.focusedItem)) {
                        $this.focusedItem.children('.ui-menuitem-link').trigger('click');
                    }
                    e.preventDefault();
                    break;

                case keyCode.UP:
                    var itemToFocus = null,
                        prevItem = $this.focusedItem.prev();

                    if(prevItem.length) {
                        itemToFocus = prevItem.find('li.ui-menuitem:visible:last');
                        if(!itemToFocus.length) {
                            itemToFocus = prevItem;
                        }
                    }
                    else {
                        itemToFocus = $this.focusedItem.closest('ul').parent('li');
                    }

                    if(itemToFocus.length) {
                        $this.focusItem(itemToFocus);
                    }

                    e.preventDefault();
                    break;

                case keyCode.DOWN:
                    var itemToFocus = null,
                        firstVisibleChildItem = $this.focusedItem.find('> ul > li:visible:first');

                    if(firstVisibleChildItem.length) {
                        itemToFocus = firstVisibleChildItem;
                    }
                    else if($this.focusedItem.next().length) {
                        itemToFocus = $this.focusedItem.next();
                    }
                    else {
                        if($this.focusedItem.next().length === 0) {
                            itemToFocus = $this.searchDown($this.focusedItem);
                        }
                    }

                    if(itemToFocus && itemToFocus.length) {
                        $this.focusItem(itemToFocus);
                    }

                    e.preventDefault();
                    break;

                case keyCode.ENTER:
                case keyCode.SPACE:
                    var currentLink = $this.focusedItem.children('.ui-menuitem-link');
                    //IE fix
                    setTimeout(function(){
                        currentLink.trigger('click');
                    },1);
                    $this.jq.blur();

                    var href = currentLink.attr('href');
                    if(href && href !== '#') {
                        window.location.href = href;
                    }
                    e.preventDefault();
                    break;

                case keyCode.TAB:
                    if($this.focusedItem) {
                        if(PrimeFaces.env.isIE()) {
                            $this.focusCheck = true;
                        }
                        $(this).focus();
                    }
                    break;
            }
        }).on('blur.lwmenu', function(e) {
            if(PrimeFaces.env.isIE() && !$this.focusCheck) {
                return;
            }

            $this.removeFocusedItem();
        });

        var clickNS = 'click.' + this.id;
        //remove focusedItem when document is clicked
        $(document.body).off(clickNS).on(clickNS, function(event) {
            if(!$(event.target).closest('.ui-lwmenu').length) {
                $this.removeFocusedItem();
            }
        });
    },

    collapseActiveSibling: function(header) {
        this.collapseRootSubmenu(header.parent().siblings().children('.ui-lwmenu-panel-header.ui-state-active').eq(0));
    },

    searchDown: function(item) {
        var nextOfParent = item.closest('ul').parent('li').next(),
            itemToFocus = null;

        if(nextOfParent.length) {
            itemToFocus = nextOfParent;
        }
        else if(item.closest('ul').parent('li').length === 0){
            itemToFocus = item;
        }
        else {
            itemToFocus = this.searchDown(item.closest('ul').parent('li'));
        }

        return itemToFocus;
    },

    getFirstItemOfContent: function(content) {
        return content.find('> .ui-menu-list > .ui-menuitem:visible:first-child');
    },

    getItemText: function(item) {
        return item.find('> .ui-menuitem-link > span.ui-menuitem-text');
    },

    focusItem: function(item) {
        this.removeFocusedItem();
        this.getItemText(item).addClass('ui-menuitem-outline').focus();
        this.focusedItem = item;
    },

    removeFocusedItem: function() {
        if(this.focusedItem) {
            this.getItemText(this.focusedItem).removeClass('ui-menuitem-outline');
            this.focusedItem = null;
        }
    },

    isExpanded: function(item) {
        return item.children('ul.ui-menu-list').is(':visible');
    },

    collapseRootSubmenu: function(header) {
        var panel = header.next();

        header.attr('aria-expanded', false).removeClass('ui-state-active ui-corner-top').addClass('ui-state-hover ui-corner-all')
            .children('.ui-icon').removeClass('ui-icon-triangle-1-s').addClass('ui-icon-triangle-1-e');

        panel.attr('aria-hidden', true).slideUp('normal', 'easeInOutCirc');

        this.removeAsExpanded(panel);
    },

    expandRootSubmenu: function(header, restoring) {
        var panel = header.next();

        header.attr('aria-expanded', true).addClass('ui-state-active ui-corner-top').removeClass('ui-state-hover ui-corner-all')
            .children('.ui-icon').removeClass('ui-icon-triangle-1-e').addClass('ui-icon-triangle-1-s');

        if(restoring) {
            panel.attr('aria-hidden', false).show();
        }
        else {
            panel.attr('aria-hidden', false).slideDown('normal', 'easeInOutCirc');

            this.addAsExpanded(panel);
        }
    },

    expandTreeItem: function(submenu, restoring) {
        var submenuLink = submenu.find('> .ui-menuitem-link');

        submenuLink.find('> .ui-menuitem-text').attr('aria-expanded', true);
        submenuLink.find('> .ui-lwmenu-icon').addClass('ui-icon-triangle-1-s');
        submenu.children('.ui-menu-list').show();

        if(!restoring) {
            this.addAsExpanded(submenu);
        }
    },

    collapseTreeItem: function(submenu) {
        var submenuLink = submenu.find('> .ui-menuitem-link');

        submenuLink.find('> .ui-menuitem-text').attr('aria-expanded', false);
        submenuLink.find('> .ui-lwmenu-icon').removeClass('ui-icon-triangle-1-s');
        submenu.children('.ui-menu-list').hide();

        this.removeAsExpanded(submenu);
    },

    saveState: function() {
        if(this.cfg.stateful) {
            var expandedNodeIds = this.expandedNodes.join(',');

            PrimeFaces.setCookie(this.stateKey, expandedNodeIds, {path:'/'});
        }
    },

    restoreState: function() {
        var expandedNodeIds = null;

        if(this.cfg.stateful) {
            expandedNodeIds = PrimeFaces.getCookie(this.stateKey);
        }

        if(expandedNodeIds) {
            this.collapseAll();
            this.expandedNodes = expandedNodeIds.split(',');

            for(var i = 0 ; i < this.expandedNodes.length; i++) {
                var element = $(PrimeFaces.escapeClientId(this.expandedNodes[i]));
                if(element.is('.ui-lwmenu-panel-content'))
                    this.expandRootSubmenu(element.prev(), true);
                else if(element.is('li.ui-menu-parent'))
                    this.expandTreeItem(element, true);
            }
        }
        else {
            this.expandedNodes = [];
            var activeHeaders = this.headers.filter('.ui-state-active'),
                activeTreeSubmenus = this.jq.find('.ui-menu-parent > .ui-menu-list:not(.ui-helper-hidden)');

            for(var i = 0; i < activeHeaders.length; i++) {
                this.expandedNodes.push(activeHeaders.eq(i).next().attr('id'));
            }

            for(var i = 0; i < activeTreeSubmenus.length; i++) {
                this.expandedNodes.push(activeTreeSubmenus.eq(i).parent().attr('id'));
            }
        }
    },

    removeAsExpanded: function(element) {
        var id = element.attr('id');

        this.expandedNodes = $.grep(this.expandedNodes, function(value) {
            return value != id;
        });

        this.saveState();
    },

    addAsExpanded: function(element) {
        this.expandedNodes.push(element.attr('id'));

        this.saveState();
    },

    clearState: function() {
        if(this.cfg.stateful) {
            PrimeFaces.deleteCookie(this.stateKey, {path:'/'});
        }
    },

    collapseAll: function() {
        this.headers.filter('.ui-state-active').each(function() {
            var header = $(this);
            header.removeClass('ui-state-active').children('.ui-icon-triangle-1-s').addClass('ui-icon-triangle-1-e').removeClass('ui-icon-triangle-1-s');
            header.next().addClass('ui-helper-hidden');
        });

        this.jq.find('.ui-menu-parent > .ui-menu-list:not(.ui-helper-hidden)').each(function() {
            $(this).addClass('ui-helper-hidden').prev().children('.ui-lwmenu-icon').removeClass('ui-icon-triangle-1-s').addClass('ui-icon-triangle-1-e');
        });
    }

});


/**
 * PrimeFaces California Layout
 */
PrimeFaces.widget.California = PrimeFaces.widget.BaseWidget.extend({

    init: function(cfg) {
        this._super(cfg);
        this.wrapper = $(document.body).children('.layout-wrapper');
        this.topbar = this.wrapper.children('.layout-topbar');
        this.sidebar = this.wrapper.children('.layout-sidebar');
        this.layoutMegaMenu = $('#layout-megamenu');
        this.nanoContainer = $('#nano-container');
        this.menu = this.jq;
        this.menulinks = this.menu.find('a');
        this.expandedMenuitems = this.expandedMenuitems||[];
        this.menuButton = $('#menu-button');
        this.megaMenuButton = $('#layout-megamenu-button');
        this.topbarMenuButton = $('#topbar-menu-button');
        this.topbarUserMenu = $('#topbar-usermenu');

        this.megaMenuClick = false;
        this.rightSidebarClick = false;
        this.sidebarMenuClick = false;
        this.sidebarUserMenuClick = false;
        this.topbarMenuClick = false;

        this._bindEvents();

        if(!this.isSlimMenu() && !this.isHorizontalMenu()) {
            this.restoreMenuState();
        }

        //this.nanoContainer.nanoScroller({flash:true});
    },

    _bindEvents: function() {
        var $this = this;


        $this.topbarMenuButton.off('click').on('click', function (e) {
            //TODO: Move to CSS
            $this.topbarUserMenu.css({ top: '60px', right: '0', left: 'auto' });
            $this.topbarMenuClick = true;

            if ($this.topbarUserMenu.hasClass('usermenu-active')) {
                $this.topbarUserMenu.removeClass('fadeInDown').addClass('fadeOutUp');

                setTimeout(function () {
                    $this.topbarUserMenu.removeClass('usermenu-active fadeOutUp');
                }, 250);
            }
            else {
                $this.topbarUserMenu.addClass('usermenu-active fadeInDown');
            }

            e.preventDefault();
        });

        $this.topbarUserMenu.off('click').on('click', function (e) {
            $this.topbarMenuClick = true;
        });


        $(document.body).off('click').on('click', function () {


            if (!$this.topbarMenuClick && $this.topbarUserMenu.hasClass('usermenu-active')) {
                $this.topbarUserMenu.removeClass('usermenu-active')
            }

            $this.topbarMenuClick = false;
        });


        $this.sidebar.off('click').on('click', function () {
            $this.sidebarMenuClick = true;
        });

        $this.menuButton.off('click').on('click', function (e) {
            $this.sidebarMenuClick = true;

            if ($this.isDesktop()) {
                if ($this.isOverlay())
                    $this.wrapper.toggleClass('layout-wrapper-overlay-sidebar-active');
                else
                    $this.wrapper.toggleClass('layout-wrapper-sidebar-inactive');
            }
            else {
                $this.wrapper.toggleClass('layout-wrapper-sidebar-mobile-active');
            }

            e.preventDefault();
        });

        $this.megaMenuButton.off('click').on('click', function (e) {
            $this.megaMenuClick = true;

            if ($this.layoutMegaMenu.hasClass('layout-megamenu-active')) {
                $this.layoutMegaMenu.removeClass('fadeInDown').addClass('fadeOutUp');

                setTimeout(function () {
                    $this.layoutMegaMenu.removeClass('layout-megamenu-active fadeOutUp');
                    $(document.body).removeClass('body-megamenu-active');
                }, 250);
            }
            else {
                $(document.body).addClass('body-megamenu-active');
                $this.layoutMegaMenu.addClass('layout-megamenu-active fadeInDown');
            }

            e.preventDefault();
        });

        $this.layoutMegaMenu.off('click').on('click', function (e) {
            $this.megaMenuClick = true;
        });

        $this.menulinks.off('click').on('click', function (e) {
            var link = $(this),
            item = link.parent('li'),
                submenu = item.children('ul');

            if (item.hasClass('active-menuitem')) {
                if (submenu.length) {
                    $this.removeMenuitem(item.attr('id'));

                if($this.isSlimMenu() || $this.isHorizontalMenu()) {
                    item.removeClass('active-menuitem');
                    submenu.hide();
                }
                else {
                    submenu.slideUp(400, function() {
                        item.removeClass('active-menuitem');
                    });
                }
            }

            if(item.parent().is($this.jq)) {
                $this.menuActive = false;
                }
            }
            else {
                $this.addMenuitem(item.attr('id'));

            if($this.isSlimMenu() || $this.isHorizontalMenu()) {
                $this.deactivateItems(item.siblings(), false);
            }
            else {
                $this.deactivateItems(item.siblings(), true);
            }

                $this.activate(item);

            if(item.parent().is($this.jq)) {
                $this.menuActive = true;
            }
            }

                // setTimeout(function() {
                //     $this.nanoContainer.nanoScroller();
                // }, 500);

            if (submenu.length) {
                e.preventDefault();
            }
        });

        this.menu.children('li').on('mouseenter', function(e) {
            if($this.isHorizontalMenu() || $this.isSlimMenu()) {
                var item = $(this);

                if(!item.hasClass('active-menuitem')) {
                    $this.menu.find('.active-menuitem').removeClass('active-menuitem');
                    $this.menu.find('ul:visible').hide();

                    if($this.menuActive) {
                        item.addClass('active-menuitem');
                        item.children('ul').show();

                        if($this.isSlimMenu()) {
                            var userProfile = $this.sidebar.find('.user-profile');
                            userProfile.removeClass('layout-profile-active');
                            userProfile.children('ul').hide();
                        }
                    }
                }
            }
        });

        this.sidebar.find('.user-profile').off('mouseenter').on('mouseenter', function (e) {
            if($this.isSlimMenu()) {
                var item = $(this);

                if(!item.hasClass('layout-profile-active')) {
                    $this.menu.find('.active-menuitem').removeClass('active-menuitem');
                    $this.menu.find('ul:visible').hide();

                    if($this.menuActive) {
                        item.addClass('layout-profile-active');
                        item.children('ul').show();
                    }
                }
            }
        });

        $(function() {
            $this._initRightSidebar();
        });
    },

    activate: function(item) {
        var submenu = item.children('ul');
        item.addClass('active-menuitem');

        if(submenu.length) {
            if(this.isSlimMenu() || this.isHorizontalMenu())
                submenu.show();
            else
            submenu.slideDown();
        }
    },

    deactivate: function(item) {
        var submenu = item.children('ul');
        item.removeClass('active-menuitem');

        if(submenu.length) {
            submenu.hide();
        }
    },

    deactivateItems: function(items, animate) {
        var $this = this;

        for(var i = 0; i < items.length; i++) {
            var item = items.eq(i),
            submenu = item.children('ul');

            if(submenu.length) {
                if(item.hasClass('active-menuitem')) {
                    var activeSubItems = item.find('.active-menuitem');
                    item.removeClass('active-menuitem');
                    item.find('.ink').remove();

                    if(animate) {
                        submenu.slideUp('normal', function() {
                            $(this).parent().find('.active-menuitem').each(function() {
                                $this.deactivate($(this));
                            });
                        });
                    }
                    else {
                        submenu.hide();
                        item.find('.active-menuitem').each(function() {
                            $this.deactivate($(this));
                        });
                    }

                    $this.removeMenuitem(item.attr('id'));
                    activeSubItems.each(function() {
                        $this.removeMenuitem($(this).attr('id'));
                    });
                }
                else {
                    item.find('.active-menuitem').each(function() {
                        var subItem = $(this);
                        $this.deactivate(subItem);
                        $this.removeMenuitem(subItem.attr('id'));
                    });
                }
            }
            else if(item.hasClass('active-menuitem')) {
                $this.deactivate(item);
                $this.removeMenuitem(item.attr('id'));
            }
        }
    },

    removeMenuitem: function (id) {
        this.expandedMenuitems = $.grep(this.expandedMenuitems, function (value) {
            return value !== id;
        });
        this.saveMenuState();
    },

    addMenuitem: function (id) {
        if ($.inArray(id, this.expandedMenuitems) === -1) {
            this.expandedMenuitems.push(id);
        }
        this.saveMenuState();
    },

    saveMenuState: function() {
        $.cookie('california_expandeditems', this.expandedMenuitems.join(','), {path: '/'});
    },

    clearMenuState: function() {
        $.removeCookie('california_expandeditems', {path: '/'});
    },

    setInlineProfileState: function(expanded) {
        if(expanded)
            $.cookie('california_inlineprofile_expanded', "1", {path: '/'});
        else
            $.removeCookie('california_inlineprofile_expanded', {path: '/'});
    },

    restoreMenuState: function() {
        var menucookie = $.cookie('california_expandeditems');
        if (menucookie) {
            this.expandedMenuitems = menucookie.split(',');
            for (var i = 0; i < this.expandedMenuitems.length; i++) {
                var id = this.expandedMenuitems[i];
                if (id) {
                    var menuitem = $("#" + this.expandedMenuitems[i].replace(/:/g, "\\:"));
                    menuitem.addClass('active-menuitem');

                    var submenu = menuitem.children('ul');
                    if(submenu.length) {
                        submenu.show();
                    }
                }
            }
        }
    },

    enableModal: function() {
        this.modal = this.wrapper.append('<div class="layout-mask"></div>').children('.layout-mask');
    },

    disableModal: function() {
        this.modal.remove();
    },

    isOverlay: function() {
        return this.wrapper.hasClass('layout-wrapper-overlay-sidebar');
    },

    isTablet: function() {
        var width = window.innerWidth;
        return width <= 1024 && width > 640;
    },

    isDesktop: function() {
        return window.innerWidth > 1024;
    },

    isHorizontalMenu: function() {
        return this.wrapper.hasClass('layout-wrapper-horizontal-sidebar') && this.isDesktop();
    },

    isSlimMenu: function() {
        return this.isDesktop() && this.wrapper.hasClass('layout-wrapper-slim-sidebar');
    },

    isMobile: function() {
        return window.innerWidth <= 640;
    },

    _initRightSidebar: function() {
        var $this = this;

        this.rightSidebarButton = $('#right-sidebar-button');
        this.rightSidebar = $('#layout-right-sidebar');

        this.rightSidebarButton.off('click').on('click', function(e) {
            $this.rightSidebar.toggleClass('layout-right-sidebar-active');
            $this.rightSidebarClick = true;
            e.preventDefault();
        });

        this.rightSidebar.off('click').on('click', function (e) {
            $this.rightSidebarClick = true;
            e.preventDefault();
        });
    },

    closeRightSidebarMenu: function() {
        if(this.rightSidebar) {
            this.rightSidebar.removeClass('right-sidebar-active');
        }
    }

});

/*!
 * jQuery Cookie Plugin v1.4.1
 * https://github.com/carhartl/jquery-cookie
 *
 * Copyright 2006, 2014 Klaus Hartl
 * Released under the MIT license
 */
(function (factory) {
	if (typeof define === 'function' && define.amd) {
		// AMD (Register as an anonymous module)
		define(['jquery'], factory);
	} else if (typeof exports === 'object') {
		// Node/CommonJS
		module.exports = factory(require('jquery'));
	} else {
		// Browser globals
		factory(jQuery);
	}
}(function ($) {

	var pluses = /\+/g;

	function encode(s) {
		return config.raw ? s : encodeURIComponent(s);
	}

	function decode(s) {
		return config.raw ? s : decodeURIComponent(s);
	}

	function stringifyCookieValue(value) {
		return encode(config.json ? JSON.stringify(value) : String(value));
	}

	function parseCookieValue(s) {
		if (s.indexOf('"') === 0) {
			// This is a quoted cookie as according to RFC2068, unescape...
			s = s.slice(1, -1).replace(/\\"/g, '"').replace(/\\\\/g, '\\');
		}

		try {
			// Replace server-side written pluses with spaces.
			// If we can't decode the cookie, ignore it, it's unusable.
			// If we can't parse the cookie, ignore it, it's unusable.
			s = decodeURIComponent(s.replace(pluses, ' '));
			return config.json ? JSON.parse(s) : s;
		} catch(e) {}
	}

	function read(s, converter) {
		var value = config.raw ? s : parseCookieValue(s);
		return $.isFunction(converter) ? converter(value) : value;
	}

	var config = $.cookie = function (key, value, options) {

		// Write

		if (arguments.length > 1 && !$.isFunction(value)) {
			options = $.extend({}, config.defaults, options);

			if (typeof options.expires === 'number') {
				var days = options.expires, t = options.expires = new Date();
				t.setMilliseconds(t.getMilliseconds() + days * 864e+5);
			}

			return (document.cookie = [
				encode(key), '=', stringifyCookieValue(value),
				options.expires ? '; expires=' + options.expires.toUTCString() : '', // use expires attribute, max-age is not supported by IE
				options.path    ? '; path=' + options.path : '',
				options.domain  ? '; domain=' + options.domain : '',
				options.secure  ? '; secure' : ''
			].join(''));
		}

		// Read

		var result = key ? undefined : {},
			// To prevent the for loop in the first place assign an empty array
			// in case there are no cookies at all. Also prevents odd result when
			// calling $.cookie().
			cookies = document.cookie ? document.cookie.split('; ') : [],
			i = 0,
			l = cookies.length;

		for (; i < l; i++) {
			var parts = cookies[i].split('='),
				name = decode(parts.shift()),
				cookie = parts.join('=');

			if (key === name) {
				// If second argument (value) is a function it's a converter...
				result = read(cookie, value);
				break;
			}

			// Prevent storing a cookie that we couldn't decode.
			if (!key && (cookie = read(cookie)) !== undefined) {
				result[name] = cookie;
			}
		}

		return result;
	};

	config.defaults = {};

	$.removeCookie = function (key, options) {
		// Must not alter options, thus extending a fresh object...
		$.cookie(key, '', $.extend({}, options, { expires: -1 }));
		return !$.cookie(key);
	};

}));

/* Issue #924 is fixed for 5.3+ and 6.0. (compatibility with 5.3) */
if(window['PrimeFaces'] && window['PrimeFaces'].widget.Dialog) {
    PrimeFaces.widget.Dialog = PrimeFaces.widget.Dialog.extend({

        enableModality: function() {
            this._super();
            $(document.body).children(this.jqId + '_modal').addClass('ui-dialog-mask');
        },

        syncWindowResize: function() {}
    });
}

if (PrimeFaces.widget.InputSwitch) {
    PrimeFaces.widget.InputSwitch = PrimeFaces.widget.InputSwitch.extend({

        init: function (cfg) {
            this._super(cfg);

            if (this.input.prop('checked')) {
                this.jq.addClass('ui-inputswitch-checked');
            }
        },

        toggle: function () {
            var $this = this;

            if(this.input.prop('checked')) {
                 this.uncheck();
                 setTimeout(function() {
                    $this.jq.removeClass('ui-inputswitch-checked');
                 }, 100);
             }
             else {
                 this.check();
                 setTimeout(function() {
                    $this.jq.addClass('ui-inputswitch-checked');
                 }, 100);
             }
        }
    });
}

/* Issue #2131 */
if(window['PrimeFaces'] && window['PrimeFaces'].widget.Schedule) {
    PrimeFaces.widget.Schedule = PrimeFaces.widget.Schedule.extend({

        setupEventSource: function() {
            var $this = this,
            offset = moment().utcOffset()*60000;

            this.cfg.events = function(start, end, timezone, callback) {
                var options = {
                    source: $this.id,
                    process: $this.id,
                    update: $this.id,
                    formId: $this.cfg.formId,
                    params: [
                        {name: $this.id + '_start', value: start.valueOf() + offset},
                        {name: $this.id + '_end', value: end.valueOf() + offset}
                    ],
                    onsuccess: function(responseXML, status, xhr) {
                        PrimeFaces.ajax.Response.handle(responseXML, status, xhr, {
                                widget: $this,
                                handle: function(content) {
                                    callback($.parseJSON(content).events);
                                }
                            });

                        return true;
                    }
                };

                PrimeFaces.ajax.Request.handle(options);
            }
        }
    });
}