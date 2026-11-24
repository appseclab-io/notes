const crypto = require('node:crypto');
const https = require('node:https');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');

// 1. A hash, named in the call
function etag(body) {
  return crypto.createHash('md5').update(body).digest('hex');
}

// 2. A cipher, named in the call
function seal(key, text) {
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);
  return Buffer.concat([iv, cipher.update(text), cipher.final(), cipher.getAuthTag()]);
}

// 3. A key pair: the algorithm is named, its size is an option
const signing = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });

// 4. A key agreement on a named curve
function agree(peerPublicKey) {
  const ecdh = crypto.createECDH('prime256v1');
  ecdh.generateKeys();
  return ecdh.computeSecret(peerPublicKey);
}

// 5. The same call as 1, with the name read from the environment
function fingerprint(file) {
  return crypto.createHash(process.env.DIGEST || 'sha1').update(file).digest('hex');
}

// 6. A signature made inside a dependency: the app names RS256
function session(user) {
  return jwt.sign({ sub: user.id }, signing.privateKey, { algorithm: 'RS256' });
}

// 7. A password hash made inside a dependency: the app names nothing
function storePassword(password) {
  return bcrypt.hash(password, 12);
}

// 8. TLS: the two ends choose the key exchange when they connect
function rates(done) {
  https.get('https://api.example.com/rates', (res) => done(res.statusCode));
}

module.exports = { etag, seal, agree, fingerprint, session, storePassword, rates };
