package org.pchapin.daja

import org.antlr.v4.runtime._
import org.antlr.v4.runtime.tree._
import scalax.collection.Graph
import scalax.collection.edge.LDiEdge
import scalax.collection.io.dot._

/**
 * The main class of the Daja compiler.
 */
object Main {

  /**
    * Specifies the different modes of operation that are supported.
    *
    * CHECK    : Syntax and semantic check only, no code generation or execution.
    * LLVM     : Generate LLVM assembly language as output.
    */
  private object Mode extends Enumeration {
    val CHECK = Value
    val DOT = Value
    val LLVM = Value
  }

  private val reporter = new BasicConsoleReporter

  /**
    * Process input files in the Daja language. This method runs the compiler pipeline.
    *
    * @param input The input file to compile.
    * @param mode The desired target or mode of operation.
    */
  private def processDaja(input: ANTLRFileStream, mode: Mode.Value): Unit = {
    // Parse the input file as Daja.
    val lexer  = new DajaLexer(input)
    val tokens = new CommonTokenStream(lexer)
    val parser = new DajaParser(tokens)
    val tree: DajaParser.ModuleContext = parser.module()

    // Walk the tree created during the parse and analyze it for semantic errors.
    val symbolTable = new BasicSymbolTable
    val myAnalyzer  = new SemanticAnalyzer(symbolTable, reporter)
    myAnalyzer.visit(tree)

    if (reporter.getErrorCount > 0) {
      printf("%d errors found; compilation aborted!", reporter.getErrorCount)
    }
    else {
      // Construct the control flow graph of the module (really one function).
      val cfgBuilder = new CFGBuilder(symbolTable, reporter)
      val CFG = CFGBuilder.optimize(cfgBuilder.visit(tree))

      // Analyze program for the use of uninitialized variables.
      Analysis.liveness(parser, CFG)
      val ControlFlowGraph(entryBlock, _, _) = CFG

      mode match {
        case Mode.CHECK =>
          if (entryBlock.live.nonEmpty) {
            print("The following variables may be used uninitialized => ")
            for (varName <- entryBlock.live) {
              print(s"$varName ")
            }
            print("\n")
          }

        case Mode.LLVM =>
          System.out.println("LLVM code generation is not implemented!")
          val myLLVMGenerator = new LLVMGenerator(symbolTable, reporter)
          myLLVMGenerator.visit(tree)

        case Mode.DOT =>
          // Generate a DOT graph using: http://scala-graph.org/guides/dot.html
          val myDotRoot = DotRootGraph(directed=true, id=Some(Id("CFG")))
          def myEdgeTransformer(innerEdge: Graph[BasicBlock,LDiEdge]#EdgeT): Option[(DotGraph,DotEdgeStmt)] = {
            innerEdge.edge match {
              case LDiEdge(source, target, label) =>
                // label is one of 'T', 'F' or 'U' (true, false or unconditional)
                Some((myDotRoot, DotEdgeStmt(
                    node_1Id=NodeId(source.toString),
                    node_2Id=NodeId(target.toString),
                    attrList=List(DotAttr(Id("label"), Id(label.toString))),
                )))
            }
          }
          def myCNodeTransformer(innerNode: Graph[BasicBlock,LDiEdge]#NodeT): Option[(DotGraph,DotNodeStmt)] = {
            def genLabel(outerNode: BasicBlock): List[String] = {
              val assignmentsText = for (assignment <- outerNode.assignments) yield assignment.getText
              val conditionText = if (outerNode.condition.isDefined) {
                List(outerNode.condition.get.getText)
              } else {
                List()
              }
              List.concat(assignmentsText, conditionText)
            }
            val label: List[String] = genLabel(innerNode.toOuter)
            Some((myDotRoot, DotNodeStmt(
              nodeId=NodeId(innerNode.toString),
              attrList=List(DotAttr(Id("label"), Id(label.toString))),
            )))
          }
          val dot = CFG.graph.toDot(
            myDotRoot,
            myEdgeTransformer,
            cNodeTransformer=Some(myCNodeTransformer)
          )
          System.out.println(dot)
      }
    }
  }


  /**
    * The main program of the Daja educational compiler. This program accepts a file to compile
    * on the command line, along with some options the desired target, and produces error
    * messages or a compiled output file. Daja programs can also be interpreted in some cases.
    *
    * @param args The command line arguments.
    * @throws java.io.IOException If an I/O error occurs during File I/O.
    */
  def main(args: Array[String]): Unit = {
    // Don't include with normal output. Makes piping, grepping, etc easier.
    System.err.println("Daja D Compiler (C) 2017 by Vermont Technical College")

    // Analyze the command line.
    if (args.length != 2) {
      println("Usage: java -jar Daja (-k | -d | -l) source-file")
      System.exit(1)
    }

    val mode = args(0) match {
      case "-k" =>
        Mode.CHECK
      case "-d" =>
        Mode.DOT
      case "-l" =>
        Mode.LLVM
      case _ =>
        println("Error: Unknown mode, defaulting to CHECK!\n")
        Mode.CHECK
    }

    // Create a stream that reads from the specified file.
    val input = new ANTLRFileStream(args(1))
    processDaja(input, mode)
  }
}
