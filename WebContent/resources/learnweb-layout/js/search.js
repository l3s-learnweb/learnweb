var no_more_results = false;
var step = 200;
var scrolling = false;

// jquery extension: uncomment function
(function ($) {
    $.fn.uncomment = function () {
        $(this).contents().each(function () {
            if (this.nodeType === 8) {
                $(this).replaceWith(this.nodeValue);
                /*
                // Need to "evaluate" the HTML content,
                // otherwise simple text won't replace
                var e = $('<span>' + this.nodeValue + '</span>');
                $(this).replaceWith(e.contents());
                */
            }
            /*
            else if ( this.hasChildNodes() ) {
                $(this).uncomment(recurse);
            } */
        });
    };
})(jQuery);

function prepareResources() {
    if (view != 'list') {
        $("#gallery").justifiedGallery({
            rowHeight: "160",
            maxRowHeight: "180",
            margins: 10,
            captionsShowAlways : true,
            captionsAnimation : true
        });
        $('[data-fancybox="search-gallery"]').fancybox({
            baseClass: "fancybox-search-layout",
            infobar: false,
            touch: {
                vertical: false
            },
            buttons: ["close", "share"],
            animationEffect: "fade",
            transitionEffect: "fade",
            preventCaptionOverlap: false,
            idleTime: false,
            gutter: 0,
            caption: function(instance, current) {
                var $caption = $(current.opts.captionid);
                return $caption.html();
            },
            onInit : function(instance) {
                instance.$refs.inner.wrap('<div class="fancybox-outer"></div>');
            }
        });
    }
}


var loading = false;

function loadNextPage() {
    if (no_more_results || loading) // nothing found || not searched
        return;
    loading = true;
    $('#search_loading_more_results').show();
    ajaxLoadNextPage();
}

function displayNextPage(xhr, status, args) {
    var results = $('#new_results > div > div > div');  // this can include additional html like the "Page x:" on textsearch
    var resources = results.filter('.resource');
    $('#search_loading_more_results').hide();
    if (resources.length === 0 || status !== "success") {
        if (status !== "success")
            console.log('fehler', status);
        if (results.length > 0)
            $('#search_no_more_results').show();
        else
            $('#search_nothing_found').show();
        no_more_results = true;
        return;
    }
    prepareResources();
    $('#results > div').append(results);
    loading = false;
    if (view === 'list')
        createGroupTooltips();
    testIfResultsFillPage();
}

function testIfResultsFillPage() {
    if ($(window).scrollTop() + $(window).height() > $(document).height() - 100) {
        loadNextPage();
    }
}

window.onload = testIfResultsFillPage;

function createGroupTooltips() {
    $('.tooltip').tooltipster({
        contentAsHTML: true,
        maxWidth: 400,
        position: 'right',
        interactive: true,
        multiple: true,
        interactiveTolerance: 150,
        theme: 'tooltipster-custom-theme'
    });
}

$(document).ready(function () {
    prepareResources();
    <!-- TODO: this function doesn't exist, do we need this in future? -->
    /*else if(view === 'list')
    {
        ajaxLoadFactsheet();
    }*/

    $(document).bind("scroll", function () {
        testIfResultsFillPage();
    });

    //To keep track of resource click in the web search or resources_list view
    var logResourceClick = function () {
        var tempResourceId = $(this).closest('div.resource').attr('id').substring(9);
        logResourceOpened([{name: 'resource_id', value: tempResourceId}]);
        return true;
    };

    $('.resourceWebLink').mouseup(logResourceClick);
    $('.resource > div a').mouseup(logResourceClick);

    createGroupTooltips();
});
