// api-check.mjs: what Node.js exposes of ML-KEM and ML-DSA outside TLS.
// The note prints no code for this step; this file was written for the repository.
// usage: node api-check.mjs
import crypto from 'node:crypto';
import tls from 'node:tls';

console.log(`Node.js ${process.versions.node}, bundled OpenSSL ${process.versions.openssl}`);
console.log(`tls.DEFAULT_ECDH_CURVE = ${JSON.stringify(tls.DEFAULT_ECDH_CURVE)}`);
console.log(`tls.DEFAULT_MIN_VERSION = ${tls.DEFAULT_MIN_VERSION}, tls.DEFAULT_MAX_VERSION = ${tls.DEFAULT_MAX_VERSION}`);

for (const type of ['ml-kem-768', 'ml-dsa-65']) {
  try {
    crypto.generateKeyPairSync(type);
    console.log(`crypto.generateKeyPairSync('${type}'): a key pair`);
  } catch (err) {
    console.log(`crypto.generateKeyPairSync('${type}'): throws ${err.code}`);
  }
}

// Each name twice: with the key usages of a Node.js line that has the algorithm, then with
// no usage at all, so the answer is about the name alone
const usages = { 'ML-KEM-768': ['encapsulateKey', 'decapsulateKey'], 'ML-DSA-65': ['sign', 'verify'] };
for (const [name, keyUsages] of Object.entries(usages)) {
  for (const list of [keyUsages, []]) {
    const call = `crypto.subtle.generateKey('${name}', ${JSON.stringify(list)})`;
    try {
      await crypto.subtle.generateKey({ name }, true, list);
      console.log(`${call}: a key pair`);
    } catch (err) {
      console.log(`${call}: rejects with ${err.name}: ${err.message}`);
    }
  }
}
