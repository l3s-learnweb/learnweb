$('#advanced_options').on('click', function () {
    $(this).find(".fa-angle-double-up").toggleClass('open');
    $('#all_activities').stop(true).slideToggle(200);

    if ($('#grouped_activities').is(":visible")) {
        $('#grouped_activities').find('div.ui-chkbox-box')
            .removeClass('ui-state-active')
            .find('span.ui-chkbox-icon')
            .removeClass('ui-icon-check')
            .addClass('ui-icon-blank');
    }

    $('#grouped_activities').stop(true).slideToggle(200);
    if ($('#all_activities').is(":visible")) {
        $('#all_activities').find('div.ui-chkbox-box')
            .removeClass('ui-state-active')
            .find('span.ui-chkbox-icon')
            .removeClass('ui-icon-check')
            .addClass('ui-icon-blank');
    }
});

function unselectAllCheckBoxes(manyCheckBoxes) {
    manyCheckBoxes.inputs.each(function () {
        $(this).prop('checked', false);
    });
}
