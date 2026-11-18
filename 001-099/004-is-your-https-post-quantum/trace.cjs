// Which key exchange does Cloudflare's trace page report for Chrome and for Node.js?
// The note prints one-line commands for curl and Node.js; this file was written for the repository.
// It opens the page once with each client and prints only the kex= line.
// The page also shows the caller's IP address, which this script never prints.
const https = require('node:https');
const { chromium } = require('playwright');

const TRACE = 'https://www.cloudflare.com/cdn-cgi/trace';
const kex = (text) => (text.match(/^kex=.*$/m) || ['no kex line'])[0];

(async () => {
  const browser = await chromium.launch({ channel: 'chrome' });
  const page = await browser.newPage();
  const response = await page.goto(TRACE, { timeout: 30000 });
  console.log(`Chrome ${browser.version()}\t${kex(await response.text())}`);
  await browser.close();

  const body = await new Promise((resolve, reject) => {
    https.get(TRACE, (res) => {
      let text = '';
      res.setEncoding('utf8');
      res.on('data', (chunk) => { text += chunk; });
      res.on('end', () => resolve(text));
    }).on('error', reject);
  });
  console.log(`Node.js ${process.versions.node} (OpenSSL ${process.versions.openssl})\t${kex(body)}`);
})();
