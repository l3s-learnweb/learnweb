function groupRow() {
  const columnsToMerge = 5; // number of first columns to merge

  const tableEl = document.getElementById('glossary_table_form:dt');
  const rows = tableEl.querySelectorAll('.ui-datatable-data > tr');

  // if no search result
  if (rows.length === 1) {
    return;
  }

  let groupsCounter = 1;
  let lastGroupCells = null;
  let lastGroupRows = 1;
  let lastGroupEntryId = null;
  for (let i = 0; i < rows.length; i++) {
    const row = rows[i];
    const cells = row.querySelectorAll(':scope > td');
    const glossaryEntryId = cells[0].dataset.itemid;

    // noinspection NegatedIfStatementJS
    if (lastGroupEntryId !== glossaryEntryId) {
      lastGroupEntryId = glossaryEntryId;
      lastGroupRows = 1;
      lastGroupCells = cells;
      groupsCounter++;
    } else {
      lastGroupRows++;

      for (let k = 0; k < columnsToMerge; k++) {
        row.removeChild(cells[k]); // remove merged columns
      }

      for (let k = 0; k < columnsToMerge; k++) {
        const cell = lastGroupCells[k];
        cell.setAttribute('rowspan', lastGroupRows);
        cell.removeAttribute('class');
        cell.classList.add(groupsCounter % 2 === 0 ? 'ui-datatable-even' : 'ui-datatable-odd');
      }
    }
  }
}

// noinspection JSUnusedGlobalSymbols
function setPasteStatus(element, field) {
  let { id } = element;
  id = id.substring(0, id.lastIndexOf(':'));
  id = `${id}:paste_status_${field}`;
  document.getElementById(id).value = 'true';
}

/* see LazyGlossaryTableView comments for more details */
function correctPaginatorCount(el) {
  console.log('correctPaginatorCount', el.value);
  el.text = el.value / 20;
}

$(() => {
  groupRow();

  Array.prototype.forEach.call(document.querySelectorAll('#glossary_table_paginator_bottom select option'), correctPaginatorCount);
  Array.prototype.forEach.call(document.querySelectorAll('#glossary_table_paginator_top select option'), correctPaginatorCount);
});
