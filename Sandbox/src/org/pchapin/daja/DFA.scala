package org.pchapin.daja

/**
  * A deterministic finite automata.
  *
  * A DFA differs from an NFA in two ways:
  *
  * * It has no epsilon transitions.
  * * Given a starting state and an input character, only one ending state is possible.
  *
  * In code, we can represent a DFA using two pieces of information:
  *
  * * A collection of states.
  * * A map in the form <code>Map[(state, Char), state]</code>.
  *
  * The collection of states contains all possible states in the DFA. The map documents the
  * transitions between the states.
  */
class DFA(private val nfa: NFA) {
    private val alphabet = nfa.alphabet()
    private var states = Set[DFAState]()
    private var transitions = Map[(DFAState, Char), DFAState]()

    /** Process the given NFA and populate this object's own data structures. */
    def calculate(): DFA = {
        states += DFAState(nfa.epsilon_closure(Set[Int](nfa.stateLower)), startState = true)
        var visitedStates = Set[DFAState]()
        var candidateStates = states -- visitedStates

        while(candidateStates.nonEmpty) {
          val srcState = candidateStates.head
          // Given a starting DFA state and a token, where can we go? Evaluate this question
          // for each token in the alphabet.
          for (token <- alphabet) {
              val dstState = _calculate_move_closure(srcState, token)
              if (dstState != DFAState(Set[Int]())) {
                  states += dstState
                  transitions = transitions + ((srcState, token) -> dstState)
              }
          }
          visitedStates += srcState
          candidateStates = states -- visitedStates
        }

        this
    }

    /** Given a starting DFA state and a token, calculate the next DFA state.
      *
      * If it's impossible to get anywhere with the given srcState and token, the returned
      * DFAState.nfaStates will be an emtpy set.
      */
    def _calculate_move_closure(srcState: DFAState, token: Char): DFAState = {
        var nfaStatesAfterSymbol = Set[Int]()
        for (nfaState <- srcState.nfaStates) {
            val tfArgument = TransitionFunctionArgument(nfaState, token)
            if (nfa.transitionFunction.contains(tfArgument)) {
                nfaStatesAfterSymbol ++= nfa.transitionFunction(tfArgument)
            }
        }
        val nfaStatesAfterEpsilon = nfa.epsilon_closure(nfaStatesAfterSymbol)
        DFAState(nfaStatesAfterEpsilon)
    }

    /**
      * Return a DOT graph representing this DFA.
      *
      * A DOT graph is nothing more than a plain text file. One can be compiled with e.g.
      * <code>dot -Tsvg < graph.dot > graph.svg</code>.
      */
    def toDot: String = {
        var graph = "digraph dfa {\n"
        for (state <- states) {
            graph += s"\042${state.nfaStates.toList.sorted}\042 "
            if (state.startState) {
                graph += "[style = dashed]"
            }
            graph += "\n"
        }
        for ((src, dst) <- transitions) {
            graph += s"\042${src._1.nfaStates.toList.sorted}\042 -> " +
                     s"\042${dst.nfaStates.toList.sorted}\042 " +
                     s"[label = \042${src._2}\042]\n"
        }
        graph += "}\n"
        graph
    }

    /** Return true if this DFA accepts the given text; false otherwise.*/
    def `match`(text: String): Boolean = {
        // A sanity check on the length of startStates would be good.
        val startStates = for {state <- states if state.startState} yield state
        var currentState: DFAState = startStates.head

        for (i <- 0 until text.length()) {
            val key = (currentState, text.charAt(i))
            if (!transitions.contains(key)) return false
            currentState = transitions(key)
        }

        // Are we in an accepting state at the end?
        currentState.nfaStates.contains(nfa.stateUpper)
    }
}
