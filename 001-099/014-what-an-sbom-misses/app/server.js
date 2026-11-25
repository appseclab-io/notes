'use strict';

const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');

// 1. A dependency we declared, from the npm registry. It brings 2, a package we never named (ms).
const debug = require('debug')('demo');
// 4. A dependency installed from a URL on GitHub, not from the registry
const isNumber = require('is-number');
// 5. A library copied into the repository as one file, with no manifest
const _ = require('./vendor/lodash.js');
// 8. A function copied from a library into a file of our own
const { escapeHtml, truncate } = require('./src/text.js');

// 3 is a development dependency (mime), used only by a script that never ships.
// 6 and 7 are in public/index.html: a minified library committed under public/js/ and one loaded from a CDN.
// 9 and 10 are in the Dockerfile: a system package and a binary downloaded with curl.

const page = fs.readFileSync(path.join(__dirname, 'public/index.html'), 'utf8');

http
  .createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost');
    const n = url.searchParams.get('n');
    debug('%s %s', req.method, url.pathname);
    if (url.pathname === '/double') {
      const body = isNumber(n) ? String(Number(n) * 2) : 'not a number';
      res.writeHead(200, { 'content-type': 'text/plain' }).end(body);
      return;
    }
    const name = truncate(_.capitalize(url.searchParams.get('name') || 'world'), 40);
    res.writeHead(200, { 'content-type': 'text/html' }).end(page.replace('{{name}}', escapeHtml(name)));
  })
  .listen(3000);
