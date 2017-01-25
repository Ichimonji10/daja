package org.pchapin.daja

import java.io.FileReader

/**
 * The main class of the CommentStrip utility.
 *
 * This program implements the following finite state machine:
 *
 * {{{
 *     NORMAL -- '/' --> MAYBE_COMMENT
 *     NORMAL -- '"' --> DOUBLE_QUOTE (print character)
 *     NORMAL -- '\'' --> SINGLE_QUOTE (print character)
 *     NORMAL -- others --> NORMAL (print character)
 *
 *     MAYBE_COMMENT -- '/' --> SLASH_SLASH_COMMENT
 *     MAYBE_COMMENT -- '*' --> BLOCK_COMMENT (print a space)
 *     MAYBE_COMMENT -- '+' --> NESTING_COMMENT (print a space, increment depth counter)
 *     MAYBE_COMMENT -- '"' --> DOUBLE_QUOTE (print slash; print character)
 *     MAYBE_COMMENT -- '\'' --> SINGLE_QUOTE (print slash; print character)
 *     MAYBE_COMMENT -- others --> NORMAL (print slash; print character)
 *
 *     MAYBE_UNCOMMENT -- '/' --> NORMAL
 *     MAYBE_UNCOMMENT -- '*' --> MAYBE_UNCOMMENT
 *     MAYBE_UNCOMMENT -- others --> BLOCK_COMMENT
 *
 *     SLASH_SLASH_COMMENT -- '\n'--> NORMAL (print '\n')
 *     SLASH_SLASH_COMMENT -- others --> SLASH_SLASH_COMMENT
 *
 *     BLOCK_COMMENT -- '*' --> MAYBE_UNCOMMENT
 *     BLOCK_COMMENT -- '\n' --> BLOCK_COMMENT (print character)
 *     BLOCK_COMMENT -- others --> BLOCK_COMMENT
 *
 *     NESTING_COMMENT -- '/'    --> MAYBE_INCREMENT_NESTING
 *     NESTING_COMMENT -- '+'    --> MAYBE_DECREMENT_NESTING
 *     NESTING_COMMENT -- '\n'   --> NESTING_COMMENT (print character)
 *     NESTING_COMMENT -- others --> NESTING_COMMENT
 *
 *     MAYBE_INCREMENT_NESTING -- '/'    --> MAYBE_INCREMENT_NESTING
 *     MAYBE_INCREMENT_NESTING -- '+'    --> NESTING_COMMENT (increment depth counter)
 *     MAYBE_INCREMENT_NESTING -- others --> NESTING_COMMENT
 *
 *     MAYBE_DECREMENT_NESTING -- '+'    --> MAYBE_DECREMENT_NESTING
 *     MAYBE_DECREMENT_NESTING -- '/'    --> NESTING_COMMENT or NORMAL (decrement depth counter)
 *     MAYBE_DECREMENT_NESTING -- others --> NESTING_COMMENT
 *
 *     DOUBLE_QUOTE -- '\\' --> ESCAPE_ONE_DOUBLE (print character)
 *     DOUBLE_QUOTE -- '"' --> NORMAL (print character)
 *     DOUBLE_QUOTE -- others --> DOUBLE_QUOTE (print character)
 *
 *     SINGLE_QUOTE -- '\\' --> ESCAPE_ONE_SINGLE (print character)
 *     SINGLE_QUOTE -- '\'' --> NORMAL (print character)
 *     SINGLE_QUOTE -- others --> SINGLE_QUOTE (print character)
 *
 *     ESCAPE_ONE_DOUBLE -- others --> DOUBLE_QUOTE (print character)
 *     ESCAPE_ONE_SINGLE -- others --> SINGLE_QUOTE (print character)
 * }}}
 */
object CommentStrip {

  object StateType extends Enumeration {
    val NORMAL = Value
    val MAYBE_COMMENT = Value
    val MAYBE_UNCOMMENT = Value
    val SLASH_SLASH_COMMENT = Value
    val BLOCK_COMMENT = Value
    val NESTING_COMMENT = Value
    val MAYBE_INCREMENT_NESTING = Value
    val MAYBE_DECREMENT_NESTING = Value
    val DOUBLE_QUOTE = Value
    val SINGLE_QUOTE = Value
    val ESCAPE_ONE_DOUBLE = Value
    val ESCAPE_ONE_SINGLE = Value
  }

  private def print(ch: Int): Unit = {
    System.out.print(ch.asInstanceOf[Char])
  }

  def main(args: Array[String]): Unit = {
    var state = StateType.NORMAL
    val input = new FileReader(args(0))
    var ch: Int = 0

    // Each time a nesting comment starts or ends, this var is updated.
    var depth: Int = 0

    // Each time a character is read, these vars are updated.
    var row: Int = 1
    var col: Int = 0

    // Each time the state machine transitions from MAYBE_COMMENT to
    // NESTING_COMMENT, these vars are updated.
    var row_start: Int = 0
    var col_start: Int = 0

    while ({ ch = input.read(); ch != -1 }) {
      if (ch == '\n') {
        row += 1
        col = 0
      } else {
        col += 1
      }
      state match {
        case StateType.NORMAL =>
          ch match {
            case '/'  =>            state = StateType.MAYBE_COMMENT
            case '"'  => print(ch); state = StateType.DOUBLE_QUOTE
            case '\'' => print(ch); state = StateType.SINGLE_QUOTE
            case _    => print(ch)
          }

        case StateType.MAYBE_COMMENT =>
          ch match {
            case '/' =>                         state = StateType.SLASH_SLASH_COMMENT
            case '*' => print(' ');             state = StateType.BLOCK_COMMENT
            case '+' =>
              print(' ')
              depth += 1
              row_start = row
              col_start = col
              state = StateType.NESTING_COMMENT
            case '"' => print('/'); print(ch);  state = StateType.DOUBLE_QUOTE
            case '\''=> print('/'); print(ch);  state = StateType.SINGLE_QUOTE
            case _   => print('/'); print(ch);  state = StateType.NORMAL
          }

        case StateType.MAYBE_UNCOMMENT =>
          ch match {
            case '/' => state = StateType.NORMAL
            case '*' =>
            case _   => state = StateType.BLOCK_COMMENT
          }

        case StateType.SLASH_SLASH_COMMENT =>
          if (ch == '\n') { print(ch); state = StateType.NORMAL }

        case StateType.BLOCK_COMMENT =>
          ch match {
            case '*'  =>            state = StateType.MAYBE_UNCOMMENT
            case '\n' => print(ch)
            case _    =>
          }

        case StateType.NESTING_COMMENT =>
          ch match {
            case '/'  =>            state = StateType.MAYBE_INCREMENT_NESTING
            case '+'  =>            state = StateType.MAYBE_DECREMENT_NESTING
            case '\n' => print(ch); state = StateType.NESTING_COMMENT
            case _    =>            state = StateType.NESTING_COMMENT
          }

        case StateType.MAYBE_INCREMENT_NESTING =>
          ch match {
            case '/' =>             state = StateType.MAYBE_INCREMENT_NESTING
            case '+' => depth += 1; state = StateType.NESTING_COMMENT
            case _   =>             state = StateType.NESTING_COMMENT
          }

        case StateType.MAYBE_DECREMENT_NESTING =>
          ch match {
            case '+' => state = StateType.MAYBE_DECREMENT_NESTING
            case '/' =>
              depth -= 1
              if (depth > 0) {
                state = StateType.NESTING_COMMENT
              } else {
                state = StateType.NORMAL
              }
            case _ => state = StateType.NESTING_COMMENT
          }

        case StateType.DOUBLE_QUOTE =>
          ch match {
            case '\\' => print(ch); state = StateType.ESCAPE_ONE_DOUBLE
            case '"'  => print(ch); state = StateType.NORMAL
            case _    => print(ch)
          }

        case StateType.SINGLE_QUOTE =>
          ch match {
            case '\\' => print(ch); state = StateType.ESCAPE_ONE_SINGLE
            case '\'' => print(ch); state = StateType.NORMAL
            case _    => print(ch)
          }

        case StateType.ESCAPE_ONE_DOUBLE =>
          print(ch)
          state = StateType.DOUBLE_QUOTE

        case StateType.ESCAPE_ONE_SINGLE =>
          print(ch)
          state = StateType.SINGLE_QUOTE
      }
    }

    if (depth != 0) {
      System.out.println(
        s"Error: the nested comment starting at row $row_start, column " +
        s"${col_start - 1} is unclosed."
      )
    }
  }
}

// vim:set ts=2 sw=2 et:
