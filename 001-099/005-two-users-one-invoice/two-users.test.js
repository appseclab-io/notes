// Two customers, one invoice each way. Run: node --test two-users.test.js
const test = require('node:test');
const assert = require('node:assert/strict');
const { createServer } = require('./api.js');

let base;
const server = createServer();
test.before(() => new Promise((done) => server.listen(0, '127.0.0.1', () => {
  base = `http://127.0.0.1:${server.address().port}`;
  done();
})));
test.after(() => server.close());

const as = (token, path) => fetch(base + path, { headers: { authorization: `Bearer ${token}` } });

test('Ana reads her own invoice', async () => {
  assert.equal((await as('token-ana', '/invoices/1')).status, 200);
});

test("Ana cannot read Bruno's invoice", async () => {
  const res = await as('token-ana', '/invoices/2');
  assert.equal(res.status, 404, `got ${res.status}: ${await res.text()}`);
});

test('the list shows Ana only her invoices', async () => {
  const list = await (await as('token-ana', '/invoices')).json();
  assert.deepEqual(list.map((invoice) => invoice.owner), ['ana', 'ana']);
});

test('without a token, nothing', async () => {
  assert.equal((await fetch(base + '/invoices/1')).status, 401);
});
