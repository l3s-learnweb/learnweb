// import styles, rollup entry point
import './sass/main.scss';

// external dependencies
import Tooltip from 'bootstrap/js/src/tooltip';
import Popover from 'bootstrap/js/src/popover';
import Scrollspy from 'bootstrap/js/src/scrollspy';
import Collapse from 'bootstrap/js/src/collapse';

// Learnweb modules
import './js/modules/jquery.auto-complete';
import './js/modules/jquery.fancybox';

window.bootstrap = {
  Tooltip,
  Popover,
  Scrollspy,
  Collapse,
};

// translate fancybox button tooltips using the PrimeFaces locale of the current page
if (window.jQuery && $.fancybox && window.PrimeFaces) {
  const lang = document.documentElement.lang || 'en';
  $.fancybox.defaults.i18n[lang] = $.extend({}, $.fancybox.defaults.i18n.en, $.fancybox.defaults.i18n[lang], {
    CLOSE: PrimeFaces.getAriaLabel('close'),
    NEXT: PrimeFaces.getAriaLabel('next'),
    PREV: PrimeFaces.getAriaLabel('previous'),
  });
  $.fancybox.defaults.lang = lang;
}
