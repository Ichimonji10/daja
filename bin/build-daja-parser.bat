@echo off

cd src\edu\vtc\daja\lev0
java -cp ..\..\..\..\..\lib\antlr-4.5.1.jar org.antlr.v4.Tool -visitor Daja.g4
cd ..\..\..\..\..
