// Starts the invoices API from api.js on 127.0.0.1:8092 and counts what it is asked for.
// api.js is not changed: this file adds a second listener to the same server.
// Run: node count.js   Stop it with Ctrl+C to print the counts.
const { createServer } = require('./api.js');

const byMethodAndStatus = new Map();
const brunoToAna = new Map();
let requests = 0;

const server = createServer();
server.prependListener('request', (req, res) => {
  const { method, url } = req;
  const asAna = (req.headers.authorization || '').replace(/^Bearer /, '') === 'token-ana';
  // Keep the body the API sends, to see whose invoice went out.
  let body = '';
  const end = res.end;
  res.end = function (chunk, ...rest) {
    if (typeof chunk === 'string') body = chunk;
    return end.call(this, chunk, ...rest);
  };
  res.on('finish', () => {
    requests += 1;
    const key = `${method} ${res.statusCode}`;
    byMethodAndStatus.set(key, (byMethodAndStatus.get(key) || 0) + 1);
    let sent;
    try {
      sent = JSON.parse(body);
    } catch {
      return;
    }
    if (asAna && res.statusCode === 200 && sent && sent.owner === 'bruno') {
      brunoToAna.set(`${method} ${url}`, (brunoToAna.get(`${method} ${url}`) || 0) + 1);
    }
  });
});

function report() {
  console.log(`requests answered: ${requests}`);
  for (const [key, count] of [...byMethodAndStatus].sort()) console.log(`  ${key}: ${count}`);
  const total = [...brunoToAna.values()].reduce((sum, count) => sum + count, 0);
  console.log(`Bruno's invoice sent to Ana with status 200: ${total}`);
  for (const [key, count] of [...brunoToAna].sort()) console.log(`  ${key}: ${count}`);
  process.exit(0);
}
process.on('SIGINT', report);
process.on('SIGTERM', report);

server.listen(8092, '127.0.0.1', () => console.log('invoices API on :8092, counting (Ctrl+C prints the counts)'));
