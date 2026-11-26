const crypto = require('node:crypto');
const fs = require('node:fs');

// A private key read from disk: this side of the agreement is long-lived
const privateKey = fs.readFileSync('/etc/app/ecdh.key');

function agree(peerPublicKey) {
  const ecdh = crypto.createECDH('prime256v1');
  ecdh.setPrivateKey(privateKey);
  return ecdh.computeSecret(peerPublicKey);
}

module.exports = { agree };
