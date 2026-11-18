// A small items API with three planted bugs. Node only, no dependencies.
const http = require('node:http');

const items = [{ id: 1, name: 'scanner' }];
const spec = {
  openapi: '3.0.3',
  info: { title: 'Items', version: '1.0.0' },
  paths: {
    '/items': {
      get: { responses: { 200: { description: 'All items', content: { 'application/json': { schema: { type: 'array', items: { $ref: '#/components/schemas/Item' } } } } } } },
      post: {
        requestBody: { required: true, content: { 'application/json': { schema: { $ref: '#/components/schemas/NewItem' } } } },
        responses: { 201: { description: 'Created', content: { 'application/json': { schema: { $ref: '#/components/schemas/Item' } } } }, 400: { description: 'Invalid item' } },
      },
    },
    '/items/{id}': {
      get: {
        parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'integer' } }],
        responses: { 200: { description: 'One item', content: { 'application/json': { schema: { $ref: '#/components/schemas/Item' } } } }, 404: { description: 'No such item' } },
      },
    },
  },
  components: {
    schemas: {
      Item: { type: 'object', required: ['id', 'name'], properties: { id: { type: 'integer' }, name: { type: 'string' } } },
      NewItem: { type: 'object', required: ['name'], properties: { name: { type: 'string', minLength: 1, maxLength: 50 } }, additionalProperties: false },
    },
  },
};

const send = (res, status, body) => {
  res.writeHead(status, { 'content-type': 'application/json' });
  res.end(JSON.stringify(body));
};

http
  .createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost');
    try {
      if (url.pathname === '/openapi.json') return send(res, 200, spec);
      if (url.pathname === '/items' && req.method === 'GET') {
        // Bug A: the id goes out as a string, the contract says integer.
        return send(res, 200, items.map((item) => ({ id: String(item.id), name: item.name })));
      }
      if (url.pathname === '/items' && req.method === 'POST') {
        let raw = '';
        req.on('data', (chunk) => (raw += chunk));
        req.on('end', () => {
          try {
            const body = JSON.parse(raw);
            // Bug B: only checks that a name is there; any length and any extra field go in.
            const item = { id: items.length + 1, name: body.name.trim() };
            items.push(item);
            send(res, 201, item);
          } catch {
            send(res, 400, { error: 'invalid item' });
          }
        });
        return;
      }
      const match = url.pathname.match(/^\/items\/([^/]+)$/);
      if (match && req.method === 'GET') {
        // Bug C: an unknown id is not a 404; reading .name of undefined throws.
        const item = items.find((i) => i.id === Number(match[1]));
        return send(res, 200, { id: item.id, name: item.name });
      }
      send(res, 404, { error: 'not found' });
    } catch {
      send(res, 500, { error: 'internal error' });
    }
  })
  .listen(8091, '127.0.0.1', () => console.log('items API on :8091'));
