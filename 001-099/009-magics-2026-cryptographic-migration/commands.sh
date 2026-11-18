openssl genpkey -algorithm EC -out ec.pem \
  -pkeyopt ec_paramgen_curve:P-256
openssl genpkey -algorithm ML-DSA-65 -out mldsa.pem
openssl pkey -in mldsa.pem -pubout -out mldsa.pub
dd if=/dev/urandom of=msg.bin bs=1048576 count=1024

# 1. ECDSA: hash beside the file, sign 32 bytes
openssl dgst -sha256 -binary -out digest.bin msg.bin
openssl pkeyutl -sign -inkey ec.pem -in digest.bin \
  -pkeyopt digest:sha256 -out ec.sig

# 2. ML-DSA: hand the signer the message
openssl pkeyutl -sign -inkey mldsa.pem -in msg.bin \
  -pkeyopt deterministic:1 -out full.sig

# 3. ML-DSA with external mu
#    tr = SHAKE256(public key), 64 bytes
#    mu = SHAKE256(tr, two zero bytes, message), 64 bytes
openssl pkey -in mldsa.pem -pubout -outform DER |
  tail -c 1952 > pk.bin
openssl dgst -shake256 -xoflen 64 -binary \
  -out tr.bin pk.bin
{ cat tr.bin; printf '\0\0'; cat msg.bin; } |
  openssl dgst -shake256 -xoflen 64 -binary -out mu.bin
openssl pkeyutl -sign -inkey mldsa.pem -in mu.bin \
  -pkeyopt mu:1 -pkeyopt deterministic:1 -out mu.sig

# Same signature? Does it verify against the file?
cmp full.sig mu.sig
openssl pkeyutl -verify -pubin -inkey mldsa.pub \
  -in msg.bin -sigfile mu.sig
