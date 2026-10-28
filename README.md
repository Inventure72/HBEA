# HBEA - CP2 Interfaces Project

Matteo Giorgetti · mgiorgetti.ieu2024@student.ie.edu

Java 21 project for the supplied HBEA assignment. The source uses simple arrays,
loops, interfaces and inheritance. No Maven, Gradle or external libraries are needed.
Git commits in this project use the school GitHub identity `Inventure72`.

## Compile and run

From this folder:

```sh
javac -d build src/*.java
java -cp build Main
```

Enter your official email and a message containing 1-32 printable ASCII characters.
The program shows HBEA and XOR encryption, decryption, round-trip checks, a
wrong-key example and the repeated blocks for `Hi Anas`.

Or run the build with compiler warnings treated as errors:

```sh
sh scripts/build.sh
sh scripts/test.sh
java -cp build RoundTrace mgiorgetti.ieu2024@student.ie.edu
java -cp build DesWorksheet
```

All classes use the default package so an instructor can compile the individual
Java files together without changing package names. Interfaces and the required
classes retain the names and signatures specified in the assignment.

## Files to submit

- `submission/HBEA_Matteo_Giorgetti.zip`: complete project, including the report.
- `submission/HBEA_Report.pdf`: four-page report with the class diagram,
  explanations, reflection answers and actual Terminal screenshots.
- `src/*.java`: upload these individual source files separately as requested.
- `submission/DES_Worksheet_Solution.pdf`: worked solution for the second PDF.

The original PDFs are in `assignment/`. Execution logs and screenshots are in
`evidence/`. `evidence/tdd-red.txt` records the initial failing validation/XOR tests;
`tdd-green.txt` records the same behavior after implementation. `tests.txt` contains
the full final test run. The report explains TDD and BDD evidence without claiming
that every part of the program was developed test-first.

The simplified DES activity is separate from HBEA. Its explicit K2 value is
`10111001`; the red label beside the worksheet diagram contains only seven bits.
The solution uses the complete eight-bit key specified in the activity text and
parameter box.

HBEA is an educational cipher. Email-derived keys, independent blocks and eight
rounds make it unsuitable for protecting real messages.
