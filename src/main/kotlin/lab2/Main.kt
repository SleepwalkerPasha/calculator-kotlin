package lab2

fun main() {

    val tests = listOf(
        listOf("ab+cd   * eeeeee=357", "abc", "aaab"),
        listOf("accex", "abchy", "accey", "abbchx"),
        listOf("acccccccccccdd", "aeb", "aebf", "aa", "abc"),
        listOf("abhp", "afn", "aehm", "adaaaaa", "beeeeeeeeeeeeehm", "ceefn", "ackhpo")
    )
    val listVariant = listOf("var1", "var2", "var3_nd", "var4")

    for (i in tests.indices) {
        variant(tests[i], listVariant[i])
        println()
    }
}

fun variant(tests: List<String>, variant: String) {
    val stateMachine = StateMachine(variant)
    stateMachine.checkTerministicState()
    tests.forEach {
        stateMachine.checkWord(it)
    }
}
