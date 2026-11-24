// 32 ways to ask Node.js for cryptography, one per line, for the scanner to read.
// Never run this file: some of the names are spellings Node.js refuses.
const crypto = require('node:crypto');
const { createHash: mkHash } = require('node:crypto');
const tls = require('node:tls');
const jwt = require('jsonwebtoken');

const DIGEST = 'sha512';
const pick = (name) => crypto.createHash(name);

function probe(k, iv, data, key, cfg, token) {
  // Hashes and MACs
  crypto.createHash('sha256'); // name in the call
  crypto.createHash(DIGEST); // name in a constant
  mkHash('sha3-256'); // renamed import
  pick('sha384'); // name passed through a function
  crypto.createHash(cfg.digest); // name known only when the app runs
  crypto.createHmac('sha256', k);
  // Key derivation
  crypto.pbkdf2Sync('p', 's', 100000, 32, 'sha256');
  crypto.hkdfSync('sha256', k, 's', 'info', 32);
  crypto.scryptSync('p', 's', 32);
  // Ciphers
  crypto.createCipheriv('aes-256-gcm', k, iv); // the spelling Node.js uses
  crypto.createCipheriv('id-aes256-GCM', k, iv); // an alias Node.js accepts
  crypto.createCipheriv('aes256-GCM', k, iv); // a spelling Node.js refuses
  crypto.createCipheriv('aes-256-cbc', k, iv);
  crypto.createCipheriv('chacha20-poly1305', k, iv);
  // Signatures
  crypto.createSign('RSA-SHA256');
  crypto.createSign('sha256WithRSAEncryption');
  crypto.sign('sha256', data, key);
  // Key pairs and key agreement
  crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
  crypto.generateKeyPairSync('ec', { namedCurve: 'prime256v1' });
  crypto.generateKeyPairSync('ed25519');
  crypto.generateKeyPairSync('x25519');
  crypto.generateKeyPairSync('ml-kem-768');
  crypto.generateKeyPairSync('ml-dsa-65');
  crypto.createECDH('prime256v1');
  crypto.diffieHellman({ privateKey: key, publicKey: key });
  // WebCrypto
  crypto.subtle.digest('SHA-256', data);
  crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, data);
  crypto.subtle.generateKey({ name: 'ECDSA', namedCurve: 'P-256' }, true, ['sign']);
  // JSON Web Tokens
  jwt.sign(data, key, { algorithm: 'ES256' });
  jwt.sign(data, k); // the library's default, HS256
  jwt.verify(token, key, { algorithms: ['RS256'] });
  // TLS
  tls.connect({ host: 'example.com', port: 443, ecdhCurve: 'X25519MLKEM768' });
}

module.exports = { probe };
