// Invoices API for two customers, signed in with bearer tokens. Node only, no dependencies.
// OWNERSHIP_CHECK=1 turns on the line that the first version did not have.
const http = require('node:http');

const users = new Map([['token-ana', 'ana'], ['token-bruno', 'bruno']]);
const invoices = [
  { id: 1, owner: 'ana', items: 3 },
  { id: 2, owner: 'bruno', items: 7 },
  { id: 3, owner: 'ana', items: 1 },
];

function createServer({ ownershipCheck = process.env.OWNERSHIP_CHECK === '1' } = {}) {
  return http.createServer((req, res) => {
    const send = (status, body) => {
      res.writeHead(status, { 'content-type': 'application/json' });
      res.end(JSON.stringify(body));
    };
    if (req.url === '/openapi.json') return send(200, require('./openapi.json'));

    const user = users.get((req.headers.authorization || '').replace(/^Bearer /, ''));
    if (!user) return send(401, { error: 'sign in first' });

    if (req.method === 'GET' && req.url === '/invoices') {
      // The list filters by owner.
      return send(200, invoices.filter((invoice) => invoice.owner === user));
    }
    const match = req.url.match(/^\/invoices\/(\d+)$/);
    if (req.method === 'GET' && match) {
      const invoice = invoices.find((i) => i.id === Number(match[1]));
      // The detail looks the invoice up by id only, unless the check is on.
      if (!invoice || (ownershipCheck && invoice.owner !== user)) return send(404, { error: 'no such invoice' });
      return send(200, invoice);
    }
    return send(404, { error: 'not found' });
  });
}

module.exports = { createServer };
if (require.main === module) createServer().listen(8092, '127.0.0.1', () => console.log('invoices API on :8092'));
