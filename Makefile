CLASSPATH=".:/usr/share/java/antlr-complete.jar:/home/ichimonji10/code/org.scala-graph/graph-core_2.12-1.11.5.jar:/home/ichimonji10/code/org.scala-graph/graph-dot_2.12-1.11.5.jar:$$CLASSPATH"
SETUP=cd src && CLASSPATH=$(CLASSPATH)

help:
	@echo "Please use \`make <target>' where <target> is one of:"
	@echo "  help"
	@echo "    to show this message"
	@echo "  compile"
	@echo "    to compile the application"
	@echo "  grun"
	@echo "    to run ANTLR4's TestRig on testData/uninitialized.daja"
	@echo "  run-check"
	@echo "    to run the application in CHECK mode"
	@echo "  run-dot"
	@echo "    to run the application in DOT mode (pass --silent to make)"

compile: src/org/pchapin/daja/Main.class

grun: compile
	$(SETUP) java org.antlr.v4.gui.TestRig org.pchapin.daja.Daja module -gui \
	< ../testData/uninitialized.daja

run-check: compile
	$(SETUP) scala org.pchapin.daja.Main -k ../testData/uninitialized.daja

run-dot: compile
	$(SETUP) scala org.pchapin.daja.Main -d ../testData/uninitialized.daja

src/org/pchapin/daja/Analysis.class: \
		src/org/pchapin/daja/Analysis.scala \
		src/org/pchapin/daja/ControlFlowGraph.class \
		src/org/pchapin/daja/IdentifierExtractorListener.class
	$(SETUP) scalac org/pchapin/daja/Analysis.scala

src/org/pchapin/daja/BasicBlock.class: \
		src/org/pchapin/daja/BasicBlock.scala \
		src/org/pchapin/daja/DajaParser.class
	$(SETUP) scalac org/pchapin/daja/BasicBlock.scala

src/org/pchapin/daja/BasicConsoleReporter.class: \
		src/org/pchapin/daja/BasicConsoleReporter.scala \
		src/org/pchapin/daja/Reporter.class
	$(SETUP) scalac org/pchapin/daja/BasicConsoleReporter.scala

src/org/pchapin/daja/BasicSymbolTable.class: \
		src/org/pchapin/daja/BasicSymbolTable.scala \
		src/org/pchapin/daja/SymbolTable.class
	$(SETUP) scalac org/pchapin/daja/BasicSymbolTable.scala

src/org/pchapin/daja/CFGBuilder.class: \
		src/org/pchapin/daja/CFGBuilder.scala \
		src/org/pchapin/daja/BasicBlock.class \
		src/org/pchapin/daja/ControlFlowGraph.class \
		src/org/pchapin/daja/DajaBaseVisitor.class \
		src/org/pchapin/daja/Reporter.class \
		src/org/pchapin/daja/SymbolTable.class
	$(SETUP) scalac org/pchapin/daja/CFGBuilder.scala

src/org/pchapin/daja/ControlFlowGraph.class: \
		src/org/pchapin/daja/ControlFlowGraph.scala \
		src/org/pchapin/daja/BasicBlock.class
	$(SETUP) scalac org/pchapin/daja/ControlFlowGraph.scala

src/org/pchapin/daja/DajaBaseListener.java:
	bin/build-daja-parser.sh

src/org/pchapin/daja/DajaBaseListener.class: src/org/pchapin/daja/DajaBaseListener.java
	$(SETUP) javac org/pchapin/daja/DajaBaseListener.java

src/org/pchapin/daja/DajaBaseVisitor.java:
	bin/build-daja-parser.sh

src/org/pchapin/daja/DajaBaseVisitor.class: src/org/pchapin/daja/DajaBaseVisitor.java
	$(SETUP) javac org/pchapin/daja/DajaBaseVisitor.java

src/org/pchapin/daja/DajaLexer.java:
	bin/build-daja-parser.sh

src/org/pchapin/daja/DajaLexer.class: src/org/pchapin/daja/DajaLexer.java
	$(SETUP) javac org/pchapin/daja/DajaLexer.java

src/org/pchapin/daja/DajaParser.java:
	bin/build-daja-parser.sh

src/org/pchapin/daja/DajaParser.class: src/org/pchapin/daja/DajaParser.java
	$(SETUP) javac org/pchapin/daja/DajaParser.java

src/org/pchapin/daja/IdentifierExtractorListener.class: \
		src/org/pchapin/daja/IdentifierExtractorListener.scala \
		src/org/pchapin/daja/DajaBaseListener.class
	$(SETUP) scalac org/pchapin/daja/IdentifierExtractorListener.scala

src/org/pchapin/daja/LLVMGenerator.class: \
		src/org/pchapin/daja/LLVMGenerator.scala \
		src/org/pchapin/daja/BasicSymbolTable.class \
		src/org/pchapin/daja/DajaBaseVisitor.class \
		src/org/pchapin/daja/Reporter.class
	$(SETUP) scalac org/pchapin/daja/LLVMGenerator.scala

src/org/pchapin/daja/Main.class: \
		src/org/pchapin/daja/Main.scala \
		src/org/pchapin/daja/Analysis.class \
		src/org/pchapin/daja/BasicConsoleReporter.class \
		src/org/pchapin/daja/BasicSymbolTable.class \
		src/org/pchapin/daja/CFGBuilder.class \
		src/org/pchapin/daja/ControlFlowGraph.class \
		src/org/pchapin/daja/DajaLexer.class \
		src/org/pchapin/daja/LLVMGenerator.class \
		src/org/pchapin/daja/SemanticAnalyzer.class
	$(SETUP) scalac org/pchapin/daja/Main.scala

src/org/pchapin/daja/Reporter.class: src/org/pchapin/daja/Reporter.scala
	$(SETUP) scalac org/pchapin/daja/Reporter.scala

src/org/pchapin/daja/SemanticAnalyzer.class: \
		src/org/pchapin/daja/SemanticAnalyzer.scala \
		src/org/pchapin/daja/BasicSymbolTable.class \
		src/org/pchapin/daja/DajaBaseVisitor.class \
		src/org/pchapin/daja/TypeRep.class
	$(SETUP) scalac org/pchapin/daja/SemanticAnalyzer.scala

src/org/pchapin/daja/SymbolTable.class: \
		src/org/pchapin/daja/SymbolTable.scala \
		src/org/pchapin/daja/TypeRep.class
	$(SETUP) scalac org/pchapin/daja/SymbolTable.scala

src/org/pchapin/daja/TypeRep.class: src/org/pchapin/daja/TypeRep.scala
	$(SETUP) scalac org/pchapin/daja/TypeRep.scala

.PHONY: help compile grun run-check run-dot
