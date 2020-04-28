function showQueriesList() {
  const $queries = $('#queries');
  const $snippet = $('#snippetsList');

  $queries.removeClass('col-12 col-md-12 col-lg-12').addClass('col-6 col-md-6 col-lg-6');
  $snippet.show();
}

function hideQueriesList() {
  const $queries = $('#queries');
  const $snippet = $('#snippetsList');

  $snippet.hide();
  $queries.removeClass('col-6 col-md-6 col-lg-6').addClass('col-12 col-md-12 col-lg-12');
}

let searchRanks;
function filterSnippets() {
  // The below code removes all snippets that doesn't contain the selected entity
  $('.snippet').each((i, el) => {
    const searchId = $(el).data('searchid');
    const rank = $(el).data('rank');
    if (!searchRanks.get(searchId).includes(rank)) $(el).hide();
  });

  showQueriesList();
}

$(document).on('click', '.session_block', (e) => {
  hideQueriesList();
});
