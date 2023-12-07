package lab3

data class AutomatonConfiguration(val state: String, var inputChain: String, var automatonChain: String) {
    override fun toString(): String {
        return "($state, $inputChain, $automatonChain)"
    }
}

fun main() {
    val pushdownAutomaton = PushdownAutomaton(fileName = "test1")
    pushdownAutomaton.readGramar()
    pushdownAutomaton.checkStr(readln())
}