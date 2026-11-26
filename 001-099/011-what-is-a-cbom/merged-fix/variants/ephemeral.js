const crypto = require('node:crypto');

// A new key pair on every call: this side of the agreement is short-lived
function agree(peerPublicKey) {
  const ecdh = crypto.createECDH('prime256v1');
  ecdh.generateKeys();
  return ecdh.computeSecret(peerPublicKey);
}

module.exports = { agree };
