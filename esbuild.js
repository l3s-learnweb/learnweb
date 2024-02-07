import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import * as esbuild from 'esbuild'
import {sassPlugin} from 'esbuild-sass-plugin'

const copyPlugin = ({copyOnce, targets}) => ({
    name: 'esbuild-copy-plugin',
    setup(build) {
        let copiedOnce = false;
        build.onStart(() => {
            if (copyOnce && copiedOnce) {
                return;
            }

            for (const {from, to} of targets) {
                const dest = to || path.basename(from);
                fs.cpSync(from, path.join(build.initialOptions.outdir, dest), {
                    recursive: true,
                    force: true,
                    dereference: true
                });
            }

            copiedOnce = true;
            console.log('Static files copied.');
        });
    },
});

const /** @type {esbuild.BuildOptions} */ defaultOptions = {
    entryPoints: [
        {in: 'src/main/webapp/resources/learnweb/main.js', out: 'learnweb.main'},
    ],
    outdir: 'src/main/webapp/resources/bundle',
    plugins: [
        sassPlugin({
            precompile(source) {
                // Replace relative paths to images with relative to the output directory (../images)
                return source.replace(/(url\(['"]?)(\.{1,2}\/)+images\/([^'")]+['"]?\))/g, `$1../images/$3`)
            },
        }),
        copyPlugin({
            copyOnce: true,
            targets: [
                {from: 'node_modules/@fortawesome/fontawesome-free/webfonts'},
                {from: 'node_modules/video.js/dist/video-js.min.css'},
                {from: 'node_modules/video.js/dist/alt/video.core.novtt.min.js', to: 'video-js.min.js'},
                {from: 'node_modules/luxon/build/global/luxon.js', to: 'luxon.js'}, // Needed for chartjs-adapter-luxon, to work with timeseries
                {from: 'node_modules/chartjs-adapter-luxon/dist/chartjs-adapter-luxon.umd.min.js', to: 'chartjs-adapter-luxon.js'},
                {from: 'node_modules/hammerjs/hammer.min.js', to: 'hammer.js'}, // Needed for chartjs-plugin-zoom, to support touch events
                {from: 'node_modules/chartjs-plugin-zoom/dist/chartjs-plugin-zoom.min.js', to: 'chartjs-plugin-zoom.min.js'},
                {from: 'node_modules/@fancyapps/fancybox/dist/jquery.fancybox.min.css'},
                {from: 'node_modules/@fancyapps/fancybox/dist/jquery.fancybox.min.js'},
                {from: 'node_modules/jquery-contextmenu/dist/jquery.contextMenu.min.js'},
                {from: 'node_modules/shepherd.js/dist/js/shepherd.min.js'},
                {from: 'node_modules/justifiedGallery/dist/js/jquery.justifiedGallery.min.js'},
            ],
        }),
    ],
    external: ['../images/*', './webfonts/*'],
    loader: {
        '.woff': 'file',
        '.woff2': 'file',
    },
    bundle: true,
    logLevel: 'debug',
    target: ['es2020'],
    format: 'iife',
    globalName: 'Learnweb',
}

const builds = {
    prod: {
        ...defaultOptions,
        minify: true,
        drop: ['debugger'],
    },
    dev: {
        ...defaultOptions,
        sourcemap: true,
    },
}

if (process.argv[2] !== 'watch') {
    await esbuild.build(builds[process.argv[2]] || builds.prod);
} else {
    const ctx = await esbuild.context(builds.dev);
    await ctx.watch();
}

