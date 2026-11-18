// pq-handshake.mjs: which key exchange do the TLS defaults of Node.js negotiate, and
// what does Node tell the application about it?
//
// Note 008 prints only the core of this script: the group table, the ServerHello reader,
// the relay and the client's connect call. Those lines are below, unchanged, between the
// markers "From the note" and "End of the note's part". Everything else was written for
// this repository on 18 Nov 2026: the cases, the loop, the timing, the ClientHello reader
// and the JSON line per case.
//
// It needs key.pem and cert.pem for localhost in the current folder (see README.md).
// usage: node pq-handshake.mjs 200            Node's own TLS versions (1.2 and 1.3)
//        node pq-handshake.mjs 200 TLSv1.3    client and server limited to TLS 1.3
import fs from 'node:fs';
import net from 'node:net';
import tls from 'node:tls';

const runs = Number(process.argv[2]) || 200;
const minVersion = process.argv[3]; // undefined keeps Node's default
const key = fs.readFileSync('key.pem');
const cert = fs.readFileSync('cert.pem');

// One handshake's state. The relay below writes it, handshake() resets it.
let serverPort = 0;
let clientHello = null; // size of the client's first write
let up = 0; // bytes from the client
let down = 0; // bytes from the server
let serverBytes = Buffer.alloc(0);
let clientBytes = Buffer.alloc(0);
let serverSays; // getEphemeralKeyInfo() on the server's socket
let serverError;

// ---- From the note, unchanged ----
// pq-handshake.mjs, the core: the relay and the ServerHello reader.
// The full script also loops over the cases and prints a JSON line per case.
const GROUPS = { 0x001d: 'X25519', 0x0017: 'secp256r1', 0x11ec: 'X25519MLKEM768' };
// A HelloRetryRequest is a ServerHello with this fixed random (RFC 8446, 4.1.3)
const HRR = 'cf21ad74e59a6111be1d8c021e65b891c2a211167abb8c5e079e09e2c8a8339c';

function parseServerHello(buf) {
  if (buf[0] !== 0x16 || buf[5] !== 0x02) return null; // handshake record, ServerHello
  let p = 11; // record header, handshake header, legacy_version
  const random = buf.subarray(p, p + 32).toString('hex'); p += 32;
  p += 1 + buf[p]; // session id
  p += 3; // cipher suite, compression
  const end = p + 2 + buf.readUInt16BE(p); p += 2;
  while (p + 4 <= end) {
    const type = buf.readUInt16BE(p), len = buf.readUInt16BE(p + 2);
    if (type === 0x0033) return { group: GROUPS[buf.readUInt16BE(p + 4)], hrr: random === HRR }; // key_share
    p += 4 + len;
  }
  return null;
}

// Relay: client -> relay -> server, counting bytes and keeping the server's first bytes
const relay = net.createServer((c) => {
  const u = net.connect(serverPort, '127.0.0.1');
  c.on('data', (d) => { if (clientHello === null) clientHello = d.length; up += d.length; u.write(d); });
  u.on('data', (d) => { down += d.length; serverBytes = Buffer.concat([serverBytes, d]); c.write(d); });
});
// ---- End of the note's part ----

// The relay above has no error handler and keeps only the size of the client's first
// write. This second listener keeps the client's bytes and absorbs a late write to a
// client that has already left.
relay.on('connection', (c) => {
  c.on('data', (d) => { clientBytes = Buffer.concat([clientBytes, d]); });
  c.on('error', () => {});
});

const MORE_GROUPS = {
  0x0018: 'secp384r1', 0x0019: 'secp521r1', 0x001e: 'X448', 0x0100: 'ffdhe2048', 0x0101: 'ffdhe3072',
  0x11eb: 'SecP256r1MLKEM768', 0x11ed: 'SecP384r1MLKEM1024',
};
const groupName = (id) => GROUPS[id] || MORE_GROUPS[id] || `0x${id.toString(16).padStart(4, '0')}`;

// The groups a ClientHello offers (supported_groups) and the ones it sends a key for (key_share)
function parseClientHello(buf) {
  if (buf[0] !== 0x16 || buf[5] !== 0x01) return null; // handshake record, ClientHello
  let p = 43; // record header, handshake header, legacy_version, random
  p += 1 + buf[p]; // session id
  p += 2 + buf.readUInt16BE(p); // cipher suites
  p += 1 + buf[p]; // compression
  const end = p + 2 + buf.readUInt16BE(p); p += 2;
  const hello = { groups: [], keyShares: [] };
  while (p + 4 <= end) {
    const type = buf.readUInt16BE(p), len = buf.readUInt16BE(p + 2);
    if (type === 0x000a) for (let q = p + 6; q < p + 4 + len; q += 2) hello.groups.push(groupName(buf.readUInt16BE(q)));
    if (type === 0x0033) for (let q = p + 6; q < p + 4 + len; q += 4 + buf.readUInt16BE(q + 2)) hello.keyShares.push(groupName(buf.readUInt16BE(q)));
    p += 4 + len;
  }
  return hello;
}

// The size of every ClientHello record in what the client sent (two after a HelloRetryRequest)
function clientHelloSizes(buf) {
  const sizes = [];
  for (let p = 0; p + 5 <= buf.length; p += 5 + buf.readUInt16BE(p + 3)) {
    if (buf[p] === 0x16 && buf[p + 5] === 0x01) sizes.push(5 + buf.readUInt16BE(p + 3));
    if (buf[p] === 0x17) break; // encrypted from here on
  }
  return sizes;
}

const CASES = [
  // The seven rows of the note's table, in its order
  { server: 'default', client: 'default' },
  { server: 'X25519', client: 'X25519' },
  { server: 'X25519', client: 'default' },
  { server: 'default', client: 'X25519' },
  { server: 'X25519', client: 'X25519MLKEM768:X25519' },
  { server: 'X25519', client: 'X25519MLKEM768' },
  { server: 'X25519MLKEM768', client: 'X25519' },
  // The hardening snippet of the note's text
  { server: 'default', client: 'P-256:P-384' },
];
const options = (groups) => ({
  ...(minVersion ? { minVersion } : {}),
  ...(groups === 'default' ? {} : { ecdhCurve: groups }),
});

function startServer(serverOpts) {
  const server = tls.createServer({ key, cert, ...serverOpts }, (socket) => {
    serverSays = socket.getEphemeralKeyInfo();
    socket.on('error', () => {});
    socket.resume();
  });
  server.on('tlsClientError', (err) => { serverError = err.code || err.message; });
  return new Promise((resolve) => server.listen(0, '127.0.0.1', () => resolve(server)));
}

// Wait until every socket of a handshake is closed (client, both sides of the relay, server),
// so the bytes of one handshake never land in the counters of the next
const openSockets = () => process.getActiveResourcesInfo().filter((r) => r === 'TCPSocketWrap').length;
async function quiet() {
  const deadline = Date.now() + 2000;
  while (openSockets() > 0 && Date.now() < deadline) await new Promise((resolve) => setImmediate(resolve));
}

function handshake(clientOpts) {
  return new Promise((resolve) => {
    clientHello = null; up = 0; down = 0;
    serverBytes = Buffer.alloc(0); clientBytes = Buffer.alloc(0);
    serverSays = undefined; serverError = undefined;
    const result = { completed: false };
    const started = process.hrtime.bigint();

// ---- From the note, unchanged (it sits inside handshake(), so it is not indented) ----
// The client asks Node what was negotiated
const s = tls.connect({ port: relay.address().port, ca: cert, servername: 'localhost', ...clientOpts }, () => {
  console.log(parseServerHello(serverBytes), s.getEphemeralKeyInfo());
});
// ---- End of the note's part ----

    // Measured before the note's callback prints
    s.prependOnceListener('secureConnect', () => {
      result.ms = Number(process.hrtime.bigint() - started) / 1e6;
      result.completed = true;
      result.protocol = s.getProtocol();
      result.wire = parseServerHello(serverBytes);
      result.nodeClient = s.getEphemeralKeyInfo();
      result.serverFlight = down; // ServerHello to Finished, before any session ticket
    });
    s.once('secureConnect', () => s.end());
    s.on('error', (err) => { result.clientError = err.code || err.message; });
    s.on('close', async () => {
      await quiet();
      const hello = parseClientHello(clientBytes) || {};
      resolve({
        ...result,
        nodeServer: serverSays,
        serverError,
        clientHello,
        clientHellos: clientHelloSizes(clientBytes),
        offered: hello.groups,
        keyShares: hello.keyShares,
        serverBytes: down, // everything the server sent, session tickets and close included
      });
    });
    s.resume();
  });
}

// One value when every handshake agrees, the list of distinct values when they do not
function distinct(values) {
  const seen = [...new Set(values.map((v) => JSON.stringify(v)))].filter((v) => v !== undefined);
  if (seen.length === 0) return undefined;
  return seen.length === 1 ? JSON.parse(seen[0]) : seen.map((v) => JSON.parse(v));
}

function spread(values) {
  if (values.length === 0) return undefined;
  const sorted = [...values].sort((a, b) => a - b);
  const mid = sorted.length / 2;
  const median = sorted.length % 2 ? sorted[Math.floor(mid)] : (sorted[mid - 1] + sorted[mid]) / 2;
  return { min: sorted[0], median, max: sorted[sorted.length - 1] };
}

await new Promise((resolve) => relay.listen(0, '127.0.0.1', resolve));

for (const { server: serverGroups, client: clientGroups } of CASES) {
  const server = await startServer(options(serverGroups));
  serverPort = server.address().port;
  const results = [];
  for (let i = 0; i < runs; i += 1) results.push(await handshake(options(clientGroups)));
  await new Promise((resolve) => server.close(resolve));

  const done = results.filter((r) => r.completed);
  const ms = spread(done.map((r) => r.ms));
  console.log(JSON.stringify({
    server: serverGroups,
    client: clientGroups,
    minVersion: minVersion || 'default',
    runs,
    completed: done.length,
    protocol: distinct(done.map((r) => r.protocol)),
    wire: distinct(done.map((r) => r.wire)),
    nodeClient: distinct(done.map((r) => r.nodeClient)),
    nodeServer: distinct(done.map((r) => r.nodeServer)),
    clientError: distinct(results.map((r) => r.clientError)),
    serverError: distinct(results.map((r) => r.serverError)),
    clientHello: distinct(results.map((r) => r.clientHello)),
    clientHellos: distinct(results.map((r) => r.clientHellos)),
    offered: distinct(results.map((r) => r.offered)),
    keyShares: distinct(results.map((r) => r.keyShares)),
    serverFlight: spread(done.map((r) => r.serverFlight)),
    serverBytes: spread(done.map((r) => r.serverBytes)),
    medianMs: ms && Number(ms.median.toFixed(2)),
  }));
}

relay.close();
