function groupRow() {
  const columnsToMerge = 5; // number of first columns to merge
  const glossaryEntryColumn = 2; // index of column that contains the glossaryEntryId

  const rows = $('#glossary_table').find('tr');
  let groupStartIndex = null;
  let rowGroupCount = 1;
  let rowGroupGlossaryEntryId = null;
  let groupCounter = 1;

  for (let i = 0; i < rows.length; i++) {
    const row = rows.eq(i);
    const glossaryEntryId = row.children('td').eq(glossaryEntryColumn).find('span').first()
      .data('itemid');

    if (rowGroupGlossaryEntryId !== glossaryEntryId) {
      groupCounter++;
      groupStartIndex = i;
      rowGroupGlossaryEntryId = glossaryEntryId;
      rowGroupCount = 1;
    } else {
      // remove the first 5 columns
      for (let k = 0; k < columnsToMerge; k++) row.children('td').eq(0).remove();

      rowGroupCount++;
    }

    if (groupStartIndex != null && rowGroupCount > 1) {
      for (let l = 0; l < columnsToMerge; l++) {
        const cell = rows.eq(groupStartIndex).children('td').eq(l);
        cell.attr('rowspan', rowGroupCount);
        cell.removeClass(); // remove all classes
        cell.addClass(groupCounter % 2 === 0 ? 'ui-datatable-even' : 'ui-datatable-odd');
      }
    }
  }
}

// noinspection JSUnusedGlobalSymbols
function setPasteStatus(element, field) {
  let id = $(element).attr('id');
  id = id.substring(0, id.lastIndexOf(':'));
  id = `${id}:paste_status_${field}`;
  id = `#${id.replace(/:/g, '\\:')}`; //  need to escape : for jquery
  $(id).val('true');
}

/* see LazyGlossaryTableView comments for more details */
function correctPaginatorCount(index) {
  const option = $(this);
  console.log('correctPaginatorCount', option.val(), option);
  option.text(option.val() / 20);
}

$(() => {
  groupRow();

  $('#glossary_table_paginator_bottom select option').each(correctPaginatorCount);
  $('#glossary_table_paginator_top select option').each(correctPaginatorCount);
});
