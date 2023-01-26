import postcss from 'postcss';
import autoprefixer from 'autoprefixer';
import copy from 'rollup-plugin-copy';
import scss from 'rollup-plugin-scss';
import terser from '@rollup/plugin-terser';
import replace from '@rollup/plugin-replace';
import resolve from '@rollup/plugin-node-resolve';

const production = !process.env.ROLLUP_WATCH && process.env.NODE_ENV !== 'development';

const defaultOptions = {
  sourcemap: true,
  input: null,
  watchJs: null,
  watchScss: null,
  copyTargets: [],
};

function bundle(name, options) {
  options = { ...defaultOptions, ...options };

  return {
    input: options.input,
    output: {
      file: `src/main/webapp/resources/bundle/${name}.js`,
      format: 'iife',
      name: 'Learnweb',
      sourcemap: options.sourcemap,
      globals: {
        jquery: '$',
      },
    },
    watch: {
      include: options.watchJs,
    },
    external: ['jquery'],
    plugins: [
      resolve(),
      replace({
        'process.env.NODE_ENV': JSON.stringify('production'),
        preventAssignment: true,
      }),
      production && terser({
        output: {
          comments: false,
        },
      }),
      scss({
        fileName: `${name}.css`,
        outputStyle: production ? 'compressed' : 'expanded',
        sourceMap: options.sourcemap,
        watch: options.watchScss,
        importer: [
          function (url) {
            return url.startsWith('~') ? { file: `node_modules/${url.substring(1)}` } : null;
          },
        ],
        processor: () => postcss([autoprefixer]),
      }),
      copy({
        copyOnce: true,
        targets: options.copyTargets,
      }),
    ],
  };
}

export default [
  bundle('learnweb.main', {
    input: 'src/main/webapp/resources/learnweb/main.js',
    watchJs: 'src/main/webapp/resources/learnweb/**',
    watchScss: 'src/main/webapp/resources/learnweb/sass/**',
    copyTargets: [
      { src: 'node_modules/@fortawesome/fontawesome-free/webfonts/fa-regular-*', dest: 'src/main/webapp/resources/bundle/webfonts' },
      { src: 'node_modules/@fortawesome/fontawesome-free/webfonts/fa-solid-*', dest: 'src/main/webapp/resources/bundle/webfonts' },
      { src: 'node_modules/video.js/dist/video-js.min.css', dest: 'src/main/webapp/resources/bundle' },
      { src: 'node_modules/video.js/dist/alt/video.core.novtt.min.js', dest: 'src/main/webapp/resources/bundle', rename: 'video-js.min.js' },
      { src: 'node_modules/highcharts/highcharts.js*', dest: 'src/main/webapp/resources/bundle' },
      { src: 'node_modules/@fancyapps/fancybox/dist/jquery.fancybox.min.css', dest: 'src/main/webapp/resources/bundle' },
      { src: 'node_modules/@fancyapps/fancybox/dist/jquery.fancybox.min.js', dest: 'src/main/webapp/resources/bundle' },
      { src: 'node_modules/jquery-contextmenu/dist/jquery.contextMenu.min.js*', dest: 'src/main/webapp/resources/bundle' },
      { src: 'node_modules/shepherd.js/dist/js/shepherd.min.js*', dest: 'src/main/webapp/resources/bundle' },
      { src: 'node_modules/@simonwep/pickr/dist/pickr.min.js*', dest: 'src/main/webapp/resources/bundle' },
      { src: 'node_modules/@simonwep/pickr/dist/themes/nano.min.css', dest: 'src/main/webapp/resources/bundle', rename: 'pickr.min.css' },
      { src: 'node_modules/justifiedGallery/dist/js/jquery.justifiedGallery.min.js', dest: 'src/main/webapp/resources/bundle' },
    ],
  }),
];
