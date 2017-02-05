package org.pchapin.daja

import org.antlr.v4.runtime.tree.TerminalNode
import org.pchapin.daja.DajaParser.{
  Add_expressionContext,
  Postfix_expressionContext,
  Primary_expressionContext
}

import scala.collection.JavaConverters._

/**
 * Class to do semantic analysis of Daja programs.
 */
class SemanticAnalyzer(
  private val symbolTable: BasicSymbolTable,
  private val reporter   : Reporter) extends DajaBaseVisitor[TypeRep.Rep] {

  private var expressionLevel = 0

  override def visitModule(ctx: DajaParser.ModuleContext): TypeRep.Rep = {
    val name: TerminalNode = ctx.IDENTIFIER()
    if (name.getText != "main") {
      reporter.reportError(
        name.getSymbol.getLine,
        name.getSymbol.getCharPositionInLine + 1,
        "Main function must be named 'main'")
    }
    visit(ctx.block_statement)
  }


  override def visitDeclaration(ctx: DajaParser.DeclarationContext): TypeRep.Rep = {
    val initDeclarators = ctx.init_declarator.asScala
    val basicType = ctx.basic_type.getText
    val basicTypeRep = basicType match {
      case "int"    => TypeRep.IntRep
      case "bool"   => TypeRep.BoolRep
      case "double" => TypeRep.DoubleRep
    }
    for (initDeclarator <- initDeclarators) {
      val identifierName = initDeclarator.IDENTIFIER.getText
      if (ctx.array_declarator.size == 0) {
        // We're dealing with the declaration of a single variable.
        // TODO: Type check the initialization expression (if present).
        symbolTable.addObjectName(identifierName, basicTypeRep)
      } else {
        // We're dealing with an array declaration.
        // TODO: Type check the array dimension expression.
        // TODO: Verify that the array dimension is a constant expression.
        // TODO: Arrange to add the size of the array to the symbol table.
        symbolTable.addObjectName(identifierName, TypeRep.ArrayRep(basicTypeRep))
      }
    }
    TypeRep.NoTypeRep
  }


  override def visitExpression(ctx: DajaParser.ExpressionContext): TypeRep.Rep = {
    // Keep track of the number of nested open expressions.
    expressionLevel += 1
    val expressionType = visit(ctx.assignment_expression)
    expressionLevel -= 1
    expressionType
  }


  override def visitAdd_expression(ctx: Add_expressionContext): TypeRep.Rep = {
    // Is multiplication or division happening? If so, defer to another method.
    val multExpressionType = visit(ctx.multiply_expression)
    if (ctx.add_expression == null) {
      return multExpressionType
    }

    // Addition or subtraction is happening.
    val addExpressionType = visit(ctx.add_expression)
    if (addExpressionType == multExpressionType) {
      return addExpressionType  // arbitrary choice
    }

    // The types of the two operands differ. Try implicit type conversions.
    val types = List(addExpressionType, multExpressionType)
    if (types.contains(TypeRep.DoubleRep)) {
      return TypeRep.DoubleRep
    } else if (types.contains(TypeRep.IntRep)) {
      return TypeRep.IntRep
    }

    // Type conversion failed. We've encountered an error.
    var operand = ctx.MINUS()
    if (operand == null) {
      operand = ctx.PLUS()
    }
    reporter.reportError(
      operand.getSymbol.getLine,
      operand.getSymbol.getCharPositionInLine + 1,
      f"Operands are incompatible: $addExpressionType $operand $multExpressionType"
    )
    TypeRep.NoTypeRep
  }

  override def visitPostfix_expression(ctx: Postfix_expressionContext): TypeRep.Rep = {
    // If array braces are absent...
    if (ctx.primary_expression != null) {
      val primaryExpressionType = visit(ctx.primary_expression)
      return primaryExpressionType
    }

    // Array braces are present. If the term to the left of the braces isn't an array type,
    // then...
    val postfixExpressionType = visit(ctx.postfix_expression)
    if (!postfixExpressionType.isInstanceOf[TypeRep.ArrayRep]){
      reporter.reportError(
        ctx.array_declarator.LBRACKET.getSymbol.getLine,
        ctx.array_declarator.LBRACKET.getSymbol.getCharPositionInLine + 1,
        f"${ctx.postfix_expression.getText} is being accessed as if it is an array, but it " +
        f"has type $postfixExpressionType"
      )
    }

    // If the expression inside the array braces isn't an integer, then...
    val arrayDeclaratorExpressionType = visit(ctx.array_declarator.expression)
    if (arrayDeclaratorExpressionType != TypeRep.IntRep) {
      reporter.reportError(
        ctx.array_declarator.LBRACKET.getSymbol.getLine,
        ctx.array_declarator.LBRACKET.getSymbol.getCharPositionInLine + 1,
        f"${ctx.array_declarator.getText} must contain an integer expression, but it " +
        f"contains an expression of type $arrayDeclaratorExpressionType"
      )
    }
    return postfixExpressionType
  }

  override def visitPrimary_expression(ctx: Primary_expressionContext): TypeRep.Rep = {
    if (ctx.LPARENS == null) {
      visitChildren(ctx)
    }
    else {
      visitExpression(ctx.expression)
    }
  }


  override def visitTerminal(node: TerminalNode): TypeRep.Rep = {
    if (expressionLevel == 0)
      TypeRep.NoTypeRep
    else {
      // We are only concerned about identifiers in expressions.
      node.getSymbol.getType match {
        // Currently the only numeric literals supported are integer literals.
        case DajaLexer.NUMERIC_LITERAL =>
          TypeRep.IntRep

        case DajaLexer.TRUE =>
          TypeRep.BoolRep

        case DajaLexer.FALSE =>
          TypeRep.BoolRep

        case DajaLexer.IDENTIFIER =>
          try {
            symbolTable.getObjectType(node.getText)
          }
          catch {
            case  ex: SymbolTable.UnknownObjectNameException =>
              reporter.reportError(
                node.getSymbol.getLine,
                node.getSymbol.getCharPositionInLine + 1,
                "Undefined identifier: " + node.getText)
              TypeRep.NoTypeRep
          }

        // In an expression, things that are not identifiers don't have a type.
        case _ =>
          TypeRep.NoTypeRep
      }
    }
  }

}

// vim:set ts=2 sw=2 et:
