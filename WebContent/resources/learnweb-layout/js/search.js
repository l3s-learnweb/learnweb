var no_more_results = false;
var gridItemWidth = 190;
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
        $('[data-fancybox="gallery"]').fancybox({
            caption: function (instance, item) {
                return $(this).find('.metadata').html();
            }
        });
        $("#gallery").justifiedGallery('norewind');
    }
}

function resizeGridResources() {
    var results = $('#results');
    var innerWidth = results.width() - 21;
    gridItemWidth = 100 / Math.floor(results.innerWidth() / 220) + '%';
    $('.resource', results).width(gridItemWidth);
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
    $("#gallery").justifiedGallery();
    prepareResources();
    if (view === 'grid')
        resizeGridResources();
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
