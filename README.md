# AppSecLab notes

Code and raw results behind the lab notes published at [www.appseclab.io/blog](https://www.appseclab.io/blog/).

Every note on the site has something a reader can reproduce. This repository holds it: one folder per note, with the code the note prints, the commands to run it and the raw output of our own run.

## Finding a note

A folder is named as its note is on the site: the note at `/blog/NNN-slug/` is the folder `NNN-slug`. The folders are kept in blocks of a hundred, by number, and each block opens with the list of its notes.

| Notes | Block |
| --- | --- |
| 001 to 099 | [001-099](001-099/) |

## Running a lab

Each folder has its own README with what it needs and the command to run it. The results files in the folder are our run, with its date and the versions it ran on. Where a run did not give the numbers of the note, the README says so under Limits. A different browser or tool version can give a different result, and that is worth knowing too.

## Read this before you run anything

- Most labs run only on your own machine, against targets they create there. One reaches out, as its note did: 004 loads the home page of 19 public sites once.
- Some folders hold code or a dependency list that is vulnerable on purpose, because that is what the note measures. It is there to be read and scanned, not installed or deployed.

## Credits

A lab that builds on someone else's research says so in its README, with a link to the original and its license. The code in this repository is ours unless a file says otherwise.

## License

[MIT](LICENSE).
