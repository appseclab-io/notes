const crypto = require('node:crypto');

// The one-shot form: the algorithm and the curve come from the key objects
function agree(privateKey, publicKey) {
  return crypto.diffieHellman({ privateKey, publicKey });
}

module.exports = { agree };
