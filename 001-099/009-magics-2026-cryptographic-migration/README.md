# Note 009: signing a 1 GiB file with ML-DSA

Lab for the note [MAgiCS 2026: ten open papers on cryptographic migration](https://www.appseclab.io/blog/009-magics-2026-cryptographic-migration/).

The note is a reading guide to the proceedings of a workshop, and it checks one claim from one of the papers. ECDSA signs a hash, so the signer receives 32 bytes whatever the size of the message. ML-DSA signs the message itself, so by default whoever holds the private key needs every byte of it, unless the 64-byte message representative μ is computed outside the signer, which FIPS 204 allows. This lab signs a 1 GiB file the three ways with OpenSSL and compares what the signer receives, its peak memory and the signature.

## What is here

| File | What it is |
| --- | --- |
| `commands.sh` | The commands printed in the note, unchanged |
| `run.sh` | Runs them in a work directory, with `/usr/bin/time` in front of each signing command, as the note says to do. It also prints the file sizes at the end and makes 50 more ECDSA signatures to show the range of their sizes |
| `results-1gib.txt`, `results-256mib.txt` | The re-run of 18 November 2026 |

## Run it

It needs OpenSSL with ML-DSA and the `mu` option as `openssl`. The note and the re-run used OpenSSL 4.1.0 from Homebrew. The same commands also ran on OpenSSL 3.5.9 with a 1 MiB file, as a quick check that is not in the results. The `openssl` that ships with macOS is LibreSSL and has no ML-DSA, so check `openssl version` first.

It also needs 1 GiB of disk outside the repository and a machine that can hold 1 GiB in memory. The note's laptop had 16 GB.

```bash
sh run.sh /tmp/note-009
sh run.sh /tmp/note-009-256 256
```

The argument is the work directory. `run.sh` creates it, writes the keys, the random file and the signatures there, and leaves them. The second line repeats the test with a 256 MiB file, the note's second run. The 1 GiB run takes about ten seconds. Delete the two folders when you are done.

Nothing here touches the network.

## Reading the output

`run.sh` prints the OpenSSL version, the line of `dd`, then one block of `/usr/bin/time` for each of the three signing commands, in this order: ECDSA over the digest, ML-DSA over the message, ML-DSA over μ. On macOS read the line `maximum resident set size`, in bytes. On Linux `/usr/bin/time -v` prints `Maximum resident set size` in kilobytes.

After the three blocks:

- `cmp full.sig mu.sig` prints nothing when the two ML-DSA signatures are the same bytes. `run.sh` runs it a second time and prints its exit status, 0 for no difference.
- `Signature Verified Successfully` is the signature made from 64 bytes, checked against the whole file with no special option.
- The sizes in bytes: `digest.bin` 32, `mu.bin` 64, `full.sig` and `mu.sig` 3,309.

The note's table:

| Signer | Receives | Peak memory | Signature |
| --- | --- | --- | --- |
| ECDSA P-256, digest | 32 bytes | 5 MiB | 71 bytes |
| ML-DSA-65, message | 1,073,741,824 bytes | 1,029 MiB | 3,309 bytes |
| ML-DSA-65, external μ | 64 bytes | 5 MiB | 3,309 bytes, the same ones |

With the 256 MiB file the note's middle row used 261 MiB and the other two did not move. These are the numbers of the run in this folder.

## Limits

- The keys and the message are new on every run, so no signature here equals one from another run. The comparison holds inside one run: `full.sig` against `mu.sig`.
- `deterministic:1` is there only so those two signatures can be compared. ML-DSA signs with fresh randomness by default.
- The numbers in the note are the ones of this run, of 18 November 2026. The note's first run, the day before, gave 1,028 and 260 MiB in the middle row: under 1 MiB of difference.
  - **Peak memory, to the byte.** ECDSA 5,586,944 bytes (5.3 MiB), ML-DSA over the message 1,078,951,936 bytes (1,029.0 MiB), ML-DSA over μ 5,242,880 bytes (5.0 MiB). With the 256 MiB file the middle row was 273,645,568 bytes (261.0 MiB) and the other two were the same to the byte.
  - **ECDSA signatures of 69 to 72 bytes.** The signature of the file was 71 bytes in both runs. Of the 100 extra signatures, 29 had 70 bytes, 46 had 71, 24 had 72 and one had 69. The encoding of an ECDSA signature drops leading zero bytes, so a shorter one turns up now and then: 70 to 72 is the usual range, not a limit.
  - 32 and 64 bytes handed to the signer, 3,309 bytes for both ML-DSA signatures, no difference between them, and the one made from μ verifies against the file.
- The note's column is the resident set size. On macOS, `/usr/bin/time -l` prints a second figure, `peak memory footprint`, and it stayed near 2 MiB for all three commands in this run: 2,409,600 bytes for the ML-DSA signature of the 1 GiB file. The two figures differ by the size of the file because OpenSSL 4.1.0 maps the input file into memory for an algorithm that signs in one shot (`app_mmap_file` in [`apps/pkeyutl.c`](https://github.com/openssl/openssl/blob/openssl-4.1.0/apps/pkeyutl.c)): the file's pages count as resident and are not memory the command allocated. The same file in release 3.5.0 allocates a buffer of the file's size and reads into it. Either way, 1,029 MiB is the size of what the signer needed within reach.
- As the note says, that number is OpenSSL's way of doing it: its ML-DSA takes the message in one piece. What does not depend on the implementation is that, without μ or a pre-hash variant, every byte has to reach whoever holds the key.
- Signing only, one scheme, one parameter set. The Linux form of the measurement was not run.

## Credits

The test follows the paper [Scaling of Memory and Bandwidth Requirements of Post-quantum Signatures with Message Size](https://doi.org/10.1007/978-3-032-28946-9_6), by Falko Strenzke, pages 102 to 120 of these proceedings:

*Migration and Agility in Cryptographic Systems: First International Workshop, MAgiCS 2026, Rome, Italy, May 10, 2026, Proceedings.* Edited by Daniel Loebenberger (Fraunhofer AISEC and Amberg-Weiden University of Applied Sciences) and Marc Stöttinger (Hochschule RheinMain University of Applied Sciences and Arts), who organized the workshop and chaired its program. Communications in Computer and Information Science, volume 3002. Springer, Cham, 2026. eBook ISBN 978-3-032-28946-9. [https://doi.org/10.1007/978-3-032-28946-9](https://doi.org/10.1007/978-3-032-28946-9)

Copyright belongs to the editors and the authors. The book is open access under the [Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International license](https://creativecommons.org/licenses/by-nc-nd/4.0/).

That research is not ours. This lab copies no text, figure, table or code from the book: the commands and the measurements were made for the note. Where this page and the paper differ, the paper is right.
