#!/usr/bin/env sh
#
# When executed, this script will generate several files in
# src/org/pchapin/daja/.
#
# Execute this script from the root daja/ directory, not the daja/bin/
# directory. Make sure antlr4 is installed.
set -euo pipefail

(
    cd src/org/pchapin/daja \
    && java -cp /usr/share/java/antlr-complete.jar org.antlr.v4.Tool -visitor Daja.g4
)
