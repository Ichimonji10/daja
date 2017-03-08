package org.pchapin.daja

/** A single state in a DFA. */
case class DFAState(nfaStates: Set[Int], startState: Boolean = false)
