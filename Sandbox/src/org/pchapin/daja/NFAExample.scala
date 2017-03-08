package org.pchapin.daja

object NFAExample {
    def main(args: Array[String]): Unit = {
        // Let's build an NFA! Consider the RE `(a|b)*c`. We start by creating primitive NFAs
        // for the strings "a", "b", and "c".
        val primitiveA = new NFA(0, 1, Map(TransitionFunctionArgument(0, 'a') -> Set(1)))
        val primitiveB = new NFA(0, 1, Map(TransitionFunctionArgument(0, 'b') -> Set(1)))
        val primitiveC = new NFA(0, 1, Map(TransitionFunctionArgument(0, 'c') -> Set(1)))

        // Next, we combine the primitive NFAs into a complex one using Thompson's Construction.
        val result: NFA = primitiveA.union(primitiveB).kleeneClosure().concatenate(primitiveC)
        println(result.toDot)

        // Next, we convert the NFA to a DFA using subset construction.
        val dfa = new DFA(result).calculate()
        println(dfa.toDot)

        // We could minimize the resultDFA here using Hopcroft's Algorithm, but we don't.

        // Finally, we test the regex.
        val testStrings = List[String]("a", "b", "c", "abbaabc", "ca", "cac")
        for (testString <- testStrings) {
            if (dfa.`match`(testString)) {
                println(s"match:    $testString")
            } else {
                println(s"no match: $testString")
            }
        }
    }
}
