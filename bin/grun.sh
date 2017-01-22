#!/usr/bin/env bash
#
# Sample usage: bin/grun.sh module -gui testData/arrays.daja
#
set -euo pipefail

java \
    -cp .:/usr/share/java/antlr-4.5.3-complete.jar:build/production/Daja \
    org.antlr.v4.gui.TestRig \
    org.pchapin.daja.Daja "$@"
