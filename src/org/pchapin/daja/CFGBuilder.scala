package org.pchapin.daja

import scalax.collection.Graph
import scalax.collection.GraphPredef._
import scalax.collection.edge.LDiEdge

class CFGBuilder(
    symbolTable: SymbolTable,
    reporter   : Reporter) extends DajaBaseVisitor[ControlFlowGraph] {

  import scala.collection.JavaConverters._

  // ctx is a java.util.List, not a scala.List.

  private def combineStatementSequence(
      statements: Iterable[DajaParser.StatementContext]): ControlFlowGraph = {
    val graphList = statements map { visit(_) }
    graphList reduce {(left: ControlFlowGraph, right: ControlFlowGraph) =>
      val ControlFlowGraph(leftEntry, leftGraph, leftExit) = left
      val ControlFlowGraph(rightEntry, rightGraph, rightExit) = right
      ControlFlowGraph(
        leftEntry,
        (leftGraph union rightGraph) + LDiEdge(leftExit, rightEntry)('U'),
        rightExit
      )
    }
  }

  override def visitModule(
      ctx: DajaParser.ModuleContext): ControlFlowGraph = {
    // TODO: Initialized declarations should be the first basic block of the procedure's CFG.
    visit(ctx.block_statement)
  }

  override def visitBlock_statement(
      ctx: DajaParser.Block_statementContext): ControlFlowGraph = {
    combineStatementSequence(ctx.statement.asScala)
  }

  override def visitExpression_statement(
      ctx: DajaParser.Expression_statementContext): ControlFlowGraph = {
    val primitiveBlock = new BasicBlock(List(ctx), None)
    ControlFlowGraph(primitiveBlock, Graph(primitiveBlock), primitiveBlock)
  }

  override def visitIfThenStatement(
      ctx: DajaParser.IfThenStatementContext): ControlFlowGraph = {
    // create blocks and CFGs
    val expressionBlock = new BasicBlock(List(), Some(ctx.expression))
    val thenCFG = combineStatementSequence(
      ctx.block_statement.statement.asScala)
    val exitBlock = new BasicBlock(List(), None)

    // link them together
    val nodesGraph = Graph(expressionBlock, exitBlock) union thenCFG.graph
    val nodesEdgesGraph = nodesGraph +
      LDiEdge(expressionBlock, thenCFG.entryBlock)('T') +
      LDiEdge(expressionBlock, exitBlock)('F') +
      LDiEdge(thenCFG.exitBlock, exitBlock)('U')
    ControlFlowGraph(expressionBlock, nodesEdgesGraph, exitBlock)
  }

  override def visitIfThenElseStatement(
      ctx: DajaParser.IfThenElseStatementContext): ControlFlowGraph = {
    // create blocks and CFGs
    val expressionBlock = new BasicBlock(List(), Some(ctx.expression))
    val thenCFG = combineStatementSequence(
      ctx.block_statement(0).statement.asScala)
    val elseCFG = combineStatementSequence(
      ctx.block_statement(1).statement.asScala)
    val exitBlock = new BasicBlock(List(), None)

    // link them together
    val nodesGraph = Graph(expressionBlock, exitBlock) union thenCFG.graph union elseCFG.graph
    val nodesEdgesGraph = nodesGraph +
      LDiEdge(expressionBlock, thenCFG.entryBlock)('T') +
      LDiEdge(expressionBlock, elseCFG.entryBlock)('F') +
      LDiEdge(thenCFG.exitBlock, exitBlock)('U') +
      LDiEdge(elseCFG.exitBlock, exitBlock)('U')
    ControlFlowGraph(expressionBlock, nodesEdgesGraph, exitBlock)
  }

  override def visitWhile_statement(
      ctx: DajaParser.While_statementContext): ControlFlowGraph = {
    val expressionBlock = new BasicBlock(List(), Some(ctx.expression))
    val nullBlock = new BasicBlock(List(), None)
    val ControlFlowGraph(bodyEntry, bodyGraph, bodyExit) =
      combineStatementSequence(ctx.block_statement.statement.asScala)
    val allNodesGraph = Graph(expressionBlock, nullBlock) union bodyGraph
    val overallGraph = allNodesGraph +
      LDiEdge(expressionBlock, bodyEntry)('T') +
      LDiEdge(expressionBlock, nullBlock)('F') +
      LDiEdge(bodyExit, expressionBlock)('U')
    ControlFlowGraph(expressionBlock, overallGraph, nullBlock)
  }
}

object CFGBuilder {
  /**
   * Method that optimizes the CFG by 1) removing all possible null blocks, and 2) combining
   * blocks when possible to eliminate or minimize the number of blocks containing just one
   * assignment statement.
   *
   * @param CFG The control flow graph to optimize.
   * @return The optimized control flow graph.
   */
  def optimize(CFG: ControlFlowGraph): ControlFlowGraph = {
    // TODO: Implement CFG optimization.
    CFG
  }
}
