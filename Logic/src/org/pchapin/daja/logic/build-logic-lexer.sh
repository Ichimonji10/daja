#!/usr/bin/env sh
#
# When executed, this script will create several files named LogicLexer.* in the
# current directory.
#
# Execute this script from the current directory, not the root daja/ directory.
# Make sure antlr4 is installed.
set -euo pipefail

java -cp /usr/share/java/antlr-complete.jar org.antlr.v4.Tool LogicLexer.g4
