// Which key exchange does Chrome negotiate with each site? Prints one line per site.
// npm install playwright, then: node pq-check.cjs https://www.example.com/ ...
const { chromium } = require('playwright');

const signatures = { 1027: 'ECDSA P-256', 1283: 'ECDSA P-384', 2052: 'RSA-PSS', 2053: 'RSA-PSS', 2054: 'RSA-PSS' };

(async () => {
  const browser = await chromium.launch({ channel: 'chrome' });
  console.log(`Chrome ${browser.version()}`);
  for (const site of process.argv.slice(2)) {
    const context = await browser.newContext();
    const page = await context.newPage();
    const cdp = await context.newCDPSession(page);
    await cdp.send('Network.enable');
    let tls;
    cdp.on('Network.responseReceived', ({ type, response }) => {
      if (type === 'Document' && !tls && response.securityDetails) tls = response.securityDetails;
    });
    await page.goto(site, { waitUntil: 'domcontentloaded', timeout: 30000 }).catch(() => {});
    const kex = tls ? tls.keyExchangeGroup || tls.keyExchange : 'no answer';
    const signature = tls ? signatures[tls.serverSignatureAlgorithm] || tls.serverSignatureAlgorithm : '';
    console.log([site, tls?.protocol, kex, signature].join('\t'));
    await context.close();
  }
  await browser.close();
})();
