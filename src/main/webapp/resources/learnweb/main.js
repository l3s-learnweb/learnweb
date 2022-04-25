// import styles, rollup entry point
import './sass/main.scss';

// external dependencies
import Tooltip from 'bootstrap/js/src/tooltip';
import Popover from 'bootstrap/js/src/popover';
import Scrollspy from 'bootstrap/js/src/scrollspy';
import Collapse from 'bootstrap/js/src/collapse';

// Learnweb modules
import './js/modules/jquery.auto-complete';
// import './js/modules/*';

window.bootstrap = {
  Tooltip,
  Popover,
  Scrollspy,
  Collapse,
};
