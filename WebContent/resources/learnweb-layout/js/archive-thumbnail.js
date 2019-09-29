/* global ContentFlow */

let timeMaps = {};
let maxItemHeight = 325;

function loadTimeMaps() {
  timeMaps = {};
  let j = 0;
  $('.timeline-event-box').each((i, obj) => {
    if ($(obj).hasClass('selected')) timeMaps[i] = j++;
  });
}

function timelineEventSelect() {
  if (Object.keys(timeMaps).length === 0) PF('contentFlow').cf.moveTo(PF('tmlnbar').getSelectedIndex());
  else if (Object.keys(timeMaps).length > 0) PF('contentFlow').cf.moveTo(timeMaps[PF('tmlnbar').getSelectedIndex()]);
}

function loadCF() {
  const cf = new ContentFlow('contentFlow', {
    reflectionColor: '#000000',
    circularFlow: false,
    visibleItems: 3,
    scrollWheelSpeed: 1.5,
    startItem: 'visible',
    reflectionHeight: 0.3,
    maxItemHeight,
  });
  cf._init();
}

function loadMaxItemSize() {
  const screenWidth = window.screen.width;
  if (screenWidth < 1300) maxItemHeight = 325;
  else if (screenWidth >= 1300 && screenWidth < 1600) maxItemHeight = 425;
  else if (screenWidth >= 1600 && screenWidth < 2100) maxItemHeight = 600;
  else if (screenWidth >= 2100) maxItemHeight = 700;
}

$(() => {
  loadMaxItemSize();
  // loadCF();
  loadTimeMaps();
});
