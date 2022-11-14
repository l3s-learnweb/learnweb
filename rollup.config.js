import postcss from 'postcss';
import autoprefixer from 'autoprefixer';
import copy from 'rollup-plugin-copy';
import replace from '@rollup/plugin-replace';
import scss from 'rollup-plugin-scss';
import resolve from '@rollup/plugin-node-resolve';
import terser from '@rollup/plugin-terser';

const production = !process.env.ROLLUP_WATCH && process.env.NODE_ENV !== 'development';

export default [
  {
    input: 'src/main/webapp/resources/learnweb/main.js',
    output: {
      file: 'src/main/webapp/resources/bundle/learnweb.main.js',
      format: 'iife',
      name: 'Learnweb',
      sourcemap: true,
      globals: {
        jquery: '$',
      },
    },
    watch: {
      include: 'src/main/webapp/resources/learnweb/**',
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
        outputStyle: production ? 'compressed' : 'expanded',
        sourceMap: true,
        watch: 'src/main/webapp/resources/learnweb/sass',
        importer: [
          function (url) {
            return url.startsWith('~') ? { file: `node_modules/${url.substring(1)}` } : null;
          },
        ],
        processor: () => postcss([autoprefixer]),
      }),
      copy({
        copyOnce: true,
        targets: [
          // { src: 'node_modules/@fortawesome/fontawesome-free/webfonts/fa-regular-*', dest: 'src/main/webapp/resources/bundle/webfonts' },
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
    ],
  },
];
