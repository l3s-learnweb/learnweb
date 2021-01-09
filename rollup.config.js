import path from 'path';
import copy from 'rollup-plugin-copy';
import scss from '@astappiev/rollup-plugin-scss';
import resolve from '@rollup/plugin-node-resolve';
import { terser } from 'rollup-plugin-terser';

// `npm run build` -> `production` is true
// `npm run watch` -> `production` is false
const production = !process.env.ROLLUP_WATCH;

export default [
  {
    input: 'WebContent/resources/learnweb/main.js',
    output: {
      file: 'WebContent/resources/bundle/learnweb.main.js',
      format: 'iife',
      name: 'Learnweb',
      sourcemap: true,
      globals: {
        jquery: '$',
      },
    },
    watch: {
      include: 'WebContent/resources/learnweb/**',
    },
    external: ['jquery'],
    plugins: [
      resolve(),
      production && terser({
        output: {
          comments: false,
        },
      }),
      scss({
        outputStyle: 'compressed',
        sourceMap: true,
        watch: path.resolve(__dirname, 'WebContent/resources/learnweb/sass'),
        importer: [
          function (url) {
            return url.startsWith('~') ? { file: `node_modules/${url.substring(1)}` } : null;
          },
        ],
      }),
      copy({
        copyOnce: true,
        targets: [
          { src: 'node_modules/font-awesome/fonts', dest: 'WebContent/resources/bundle' },
          { src: 'node_modules/video.js/dist/video-js.min.css', dest: 'WebContent/resources/bundle' },
          { src: 'node_modules/video.js/dist/alt/video.core.novtt.min.js', dest: 'WebContent/resources/bundle', rename: 'video-js.min.js' },
          { src: 'node_modules/highcharts/highcharts.js', dest: 'WebContent/resources/bundle' },
          { src: 'node_modules/@fancyapps/fancybox/dist/jquery.fancybox.min.css', dest: 'WebContent/resources/bundle' },
          { src: 'node_modules/@fancyapps/fancybox/dist/jquery.fancybox.min.js', dest: 'WebContent/resources/bundle' },
          { src: 'node_modules/jquery-contextmenu/dist/jquery.contextMenu.min.js', dest: 'WebContent/resources/bundle' },
          { src: 'node_modules/shepherd.js/dist/js/shepherd.min.js', dest: 'WebContent/resources/bundle' },
          { src: 'node_modules/@simonwep/pickr/dist/pickr.es5.min.js', dest: 'WebContent/resources/bundle', rename: 'pickr.min.js' },
          { src: 'node_modules/@simonwep/pickr/dist/themes/nano.min.css', dest: 'WebContent/resources/bundle', rename: 'pickr.min.css' },
          { src: 'node_modules/justifiedGallery/dist/js/jquery.justifiedGallery.min.js', dest: 'WebContent/resources/bundle' },
        ],
      }),
    ],
  },
];
