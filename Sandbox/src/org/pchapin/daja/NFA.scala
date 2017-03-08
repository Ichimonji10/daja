package org.pchapin.daja

import scala.collection.mutable

/**
  * A non-deterministic finite automata.
  *
  * The NFA's states are identified by integers. They range from stateLower to stateUpper,
  * inclusive, and are contiguous. The NFA's starting state is stateLower, and its ending state
  * (i.e. accepting state) is stateUpper.
  *
  * NFAs represented by the class contain only a single final state. While this is a restriction
  * over the formal definition of an NFA, it is all that is necessary in this application. The
  * restriction does not restrict expressiveness; for an NFA with multiple final states, a new
  * NFA could be built that has a single final state reachable from the original final states
  * via epsilon transitions.
  */
class NFA(val stateLower: Int,
          val stateUpper: Int,
          val transitionFunction: Map[TransitionFunctionArgument, Set[Int]]) {

    /**
      * Returns an NFA that is the concatenation of this NFA followed by the other NFA.
      *
      * Neither NFA input to this operation is modified.
      */
    def concatenate(other: NFA): NFA = {
        var newTransition = Map[TransitionFunctionArgument, Set[Int]]()

        // ordering: lowerLower lowerUpper upperLower upperUpper
        val lowerLower = 0
        val lowerUpper = stateUpper - stateLower
        val upperLower = lowerUpper + 1
        val upperUpper = upperLower + other.stateUpper - other.stateLower

        // The first NFA to be subsumed (this object) must be translated so that it has state
        // numbers ranging from lowerLower to lowerUpper, inclusive. To do this, we calculate an
        // offset, and add that offset to ech state number in the NFA being subsumed. A similar
        // procedure is executed for the second NFA.
        //
        // offset       = targetLower - actualLower
        val lowerOffset = lowerLower  - stateLower
        val upperOffset = upperLower  - other.stateLower

        for ((oldArgument, oldResult) <- transitionFunction) {
            val newArgument = TransitionFunctionArgument(
                oldArgument.state + lowerOffset,
                oldArgument.inputCharacter
            )
            val newResult = oldResult map { state => state + lowerOffset }
            newTransition = newTransition + (newArgument -> newResult)
        }

        for ((oldArgument, oldResult) <- other.transitionFunction) {
            val newArgument = TransitionFunctionArgument(
                oldArgument.state + upperOffset,
                oldArgument.inputCharacter
            )
            val newResult = oldResult map { state => state + upperOffset }
            newTransition = newTransition + (newArgument -> newResult)
        }

        // Add an epsilon transition between the original final state and the other start state.
        val extraArgument = TransitionFunctionArgument(lowerUpper, '\u0000')
        var extraResult = newTransition.getOrElse(extraArgument, Set[Int]())
        extraResult += upperLower
        newTransition = newTransition + (extraArgument -> extraResult)

        // Create the new NFA.
        new NFA(lowerLower, upperUpper, newTransition)
    }

    /**
      * Return an NFA that is the union of this NFA followed and the other NFA.
      *
      * Neither NFA input to this operation is modified.
      *
      * Our goal is to create an NFA with the following structure:
      *
      * <pre>
      * stateLower -> a -> b -> stateUpper
      *            '> c -> d '
      * </pre>
      *
      * stateLower and stateUpper are brand new states. The transitions from stateLower to a
      * and c are epsilon transitions, as are the transitions from b and d to stateUpper.
      */
    def union(other: NFA): NFA = {
        val newLower = 0
        val newUpper = (stateUpper - stateLower) + (other.stateUpper - other.stateLower) + 3
        var newTransition = Map[TransitionFunctionArgument, Set[Int]]()

        // The two NFAs subsumed into newTransition (this object and `other`) must be translated
        // so that they have state numbers ranging from stateLower + 1 to stateUpper - 1,
        // inclusive. To subsume an NFA, we calculate an offset, and then add that offset to
        // each state number in the NFA being subsumed.
        //
        //  offset       = targetLower                            - actualLower
        val firstOffset  = newLower + 1                           - stateLower
        val secondOffset = newLower + 2 + stateUpper - stateLower - other.stateLower

        for ((oldArgument, oldResult) <- transitionFunction) {
            val newArgument = TransitionFunctionArgument(
                oldArgument.state + firstOffset,
                oldArgument.inputCharacter
            )
            val newResult = oldResult map { state => state + firstOffset }
            newTransition = newTransition + (newArgument -> newResult)
        }

        for ((oldArgument, oldResult) <- other.transitionFunction) {
            val newArgument = TransitionFunctionArgument(
                oldArgument.state + secondOffset,
                oldArgument.inputCharacter
            )
            val newResult = oldResult map { state => state + secondOffset }
            newTransition = newTransition + (newArgument -> newResult)
        }

        // Add epsilon transitions from newLower...
        var extraArgument = TransitionFunctionArgument(newLower, '\u0000')
        var extraResult = Set[Int](newLower + 1, newLower + stateUpper - stateLower + 2)
        newTransition = newTransition + (extraArgument -> extraResult)

        // ...and add epsilon transitions to newUpper.
        extraArgument = TransitionFunctionArgument(newLower + stateUpper - stateLower + 1, '\u0000')
        extraResult = newTransition.getOrElse(extraArgument, Set[Int]())
        extraResult += newUpper
        newTransition = newTransition + (extraArgument -> extraResult)

        extraArgument = TransitionFunctionArgument(newUpper - 1, '\u0000')
        extraResult = newTransition.getOrElse(extraArgument, Set[Int]())
        extraResult += newUpper
        newTransition = newTransition + (extraArgument -> extraResult)

        new NFA(newLower, newUpper, newTransition)
    }

    /**
      * Return an NFA that is the Kleene closure of this NFA. This NFA is not modified.
      *
      * Our goal is to create an NFA with the following structure:
      *
      * <pre>
      *          .--------------.
      *          |    v----.    v
      * stateLower -> a -> b -> stateUpper
      * </pre>
      *
      * stateLower and stateUpper are brand new states. The following transitions are epsilon
      * transitions:
      *
      * * stateLower to a
      * * stateLower to stateUpper
      * * b to a
      * * b to stateUpper
      */
    def kleeneClosure(): NFA = {
        val newLower = 0
        val newUpper = (stateUpper - stateLower) + 2
        var newTransition = Map[TransitionFunctionArgument, Set[Int]]()

        // The NFA subsumed into newTransition (this object) must be translated so that it has
        // state numbers ranging from stateLower + 1 to stateUpper - 1, inclusive. To subsume an
        // NFA, we calculate an offset, and then add that offset to each state number in the NFA
        // being subsumed.
        //
        // offset = targetLower   - actualLower
        val offset = newLower + 1 - stateLower

        for ((oldArgument, oldResult) <- transitionFunction) {
            val newArgument = TransitionFunctionArgument(
                oldArgument.state + offset,
                oldArgument.inputCharacter
            )
            val newResult = oldResult map { state => state + offset }
            newTransition = newTransition + (newArgument -> newResult)
        }

        // Add epsilon transitions from newLower...
        var extraArgument = TransitionFunctionArgument(newLower, '\u0000')
        var extraResult = Set[Int](newLower + 1, newUpper)
        newTransition = newTransition + (extraArgument -> extraResult)

        // ...and add epsilon transitions from b.
        extraArgument = TransitionFunctionArgument(newUpper - 1, '\u0000')
        extraResult = newTransition.getOrElse(extraArgument, Set[Int]())
        extraResult += newLower + 1
        extraResult += newUpper
        newTransition = newTransition + (extraArgument -> extraResult)

        new NFA(newLower, newUpper, newTransition)
    }

    /**
      * Return a DOT graph representing this NFA.
      *
      * A DOT graph is nothing more than a plain text file. One can be compiled with e.g.
      * <code>dot -Tsvg < graph.dot > graph.svg</code>.
      */
    def toDot: String = {
        var graph = "digraph nfa {\n"
        graph += "rankdir=LR\n"
        graph += s"$stateLower [ style = dashed ]\n"
        graph += s"$stateUpper [ style = dashed ]\n"
        for ((argument, results) <- transitionFunction) {
            for (result <- results) {
                graph += s"${argument.state} -> $result " +
                         s"[ label = \042${argument.inputCharacter}\042 ]\n"
            }
        }
        graph += "}\n"
        graph
    }

    /**From the given starting states, find all states reachable using epsilon transitions. */
    def epsilon_closure(starting_states: Set[Int]): Set[Int] = {
        // The goal is to make several independent copies of starting_states. This seems like an
        // OK way to do things, given that both copies will be extensively modified.
        val src_states: mutable.Set[Int] = mutable.Set[Int]()
        for (state <- starting_states) { src_states += state }
        val dst_states = src_states.clone()

        // Where are all the places we can get to from each of the src_states, following only
        // epsilon transitions?
        while (src_states.nonEmpty) {
            val src_state = src_states.head
            src_states -= src_state

            val key = TransitionFunctionArgument(src_state, '\u0000')
            if (transitionFunction.contains(key)) {
                src_states ++= transitionFunction(key)
                dst_states ++= transitionFunction(key)
            }
        }
        dst_states.toSet  // make immutable
    }

    /** Return all the characters in this NFA's alphabet, as a set.
      *
      * Omit the character reserved for use as the epsilon character.
      */
    def alphabet(): Set[Char] = {
        var chars = Set[Char]()
        for ((tfArgument, _) <- transitionFunction) {
            chars += tfArgument.inputCharacter
        }
        if (chars.contains('\u0000')) {
            chars -= '\u0000'
        }
        chars
    }
}
