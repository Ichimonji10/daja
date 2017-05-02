package org.pchapin.daja;

import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.Interval;
import scala.collection.mutable.Set

/**
  * Extract IDENTIFIER tokens from a Daja parse tree.
  *
  * Given a parse tree, walk through all of the nodes in that parse tree. Each
  * time an IDENTIFIER token within a primary expression is encountered, place
  * the text representing that token in <code>identifiers</code>.
  */
class IdentifierExtractorListener(parser: DajaParser) extends DajaBaseListener {
  val identifiers: Set[String] = Set()

  override def enterPrimary_expression(
      ctx: DajaParser.Primary_expressionContext): Unit = {
    if (ctx.IDENTIFIER != null) {
        identifiers += ctx.IDENTIFIER.getText
    }
  }
}
