#!/bin/bash
## To Run:
# MDELite8.jar must be on the CLASSPATH as well as the "build/classes/"
# directory from the directory with this script.
# Takes a single argument (lead name of a violet class file "X" for "X.class.violet"

java Violett.ClassParser $1.class.violet
java Violett.ClassConform $1.vpl.pl
java toschema.ToOOSchema $1.vpl.pl
echo "Wrote $1.ooschema.pl"
