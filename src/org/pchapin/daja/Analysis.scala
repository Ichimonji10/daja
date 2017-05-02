package org.pchapin.daja

import scala.collection.JavaConverters._

import org.antlr.v4.runtime.tree.ParseTreeWalker

/**
 * This object contains methods that perform various kinds of analysis on the program's CFG.
 */
object Analysis {
  /**
   * Compute the upwardly exposed sets and kill sets for each basic block in the given CFG. This
   * method mutates the basic blocks by decorating them with appropriate sets. It assumes the UE
   * and kill sets are empty initially.
   *
   * @param CFG The control flow graph to process.
   */
  private def computeUEAndKillSets(parser: DajaParser, CFG: ControlFlowGraph): Unit = {
    for (basicBlock  <- CFG.graph.outerNodeTraverser(CFG.graph get CFG.entryBlock);
        expressionStatement <- basicBlock.assignments) {

      // Get identifiers from left and right sides of assignment expression.
      val walker = new ParseTreeWalker()
      val leftExtractor = new IdentifierExtractorListener(parser)
      walker.walk(
        leftExtractor,
        expressionStatement.expression.comma_expression.assignment_expression.relational_expression
      )
      val rightExtractor = new IdentifierExtractorListener(parser)
      walker.walk(
        rightExtractor,
        expressionStatement.expression.comma_expression.assignment_expression.assignment_expression
      )

      // Populate the UE set. (For each identifier on the right side of the
      // assignment expression, if it's not in the kill set, add it to the UE
      // set.)
      val temp = rightExtractor.identifiers -- basicBlock.killed
      basicBlock.upwardlyExposed ++= temp

      // Populate the kill set. (For each identifier on the left side of the
      // assignment expression, add it to the kill set.)
      basicBlock.killed ++= leftExtractor.identifiers
    }
    for (node <- CFG.graph.outerNodeTraverser(CFG.graph get CFG.entryBlock)) {
      node.condition match {
        case None =>
          // Nothing to do!
        case Some(expression) =>
          // TODO: Process the expression used as a condition at the end of the block.
      }
    }
  }

  /**
   * Conducts a liveness analysis on the given CFG.
   *
   * @param CFG A representation of the control flow of the program being analyzed.
   */
  def liveness(parser: DajaParser, CFG: ControlFlowGraph): Unit = {
    computeUEAndKillSets(parser, CFG)

    // Keep looping until a fixed point is reached.
    var changed = true
    while (changed) {
      changed = false
      for (node  <- CFG.graph.innerNodeTraverser(CFG.graph get CFG.entryBlock);
           successor <- node.diSuccessors) {
        val oldLive = node.live
        node.live = node.live ++
          successor.upwardlyExposed ++ (successor.live -- successor.killed)
        if (node.live != oldLive) changed = true
      }
    }
  }
}
