package lab2

import java.io.File
import java.util.*

class StateMachine(fileName: String) {
    private var states: MutableMap<String, State> = mutableMapOf()
    private var maxStateCount = 0
    private var finalStates: MutableList<String> = mutableListOf()
    private var inputs: MutableList<String> = mutableListOf()
    private var nonDeterministicStates: Deque<Pair<String, State>> = ArrayDeque()

    var isDeterministic = true

    init {
        readStateMachine(fileName)
    }


    private fun readStateMachine(fileName: String) {
        readFileDirectlyAsText("src/main/resources/$fileName.txt").forEach {
            if (!it.matches(Regex("q[0-9]+,.=[qf][0-9]+"))) {
                throw UnsupportedOperationException("ошибки в записи конечного автомата")
            }
            if (it.isNotBlank()) {
                val stateFrom = it.substringBefore(",") // состояние, например q0
                val stringWithoutKey = it.removePrefix("$stateFrom,")
                val letter = stringWithoutKey.substringBeforeLast("=") // символ перехода, например a,b,c
                val stateTo =
                    stringWithoutKey.substringAfterLast("=") // следующее состояние после перехода например q1, q2

                if (!inputs.contains(letter)) {
                    inputs.add(letter)
                }

                if (states.containsKey(stateFrom)) {
                    if (states[stateFrom]!!.add(letter, stateTo)) {
                        isDeterministic = false
                    }
                } else {
                    val state = if (stateFrom.contains("f")) {
                        finalStates.add(stateFrom)
                        State(isFinal = true)
                    } else {
                        State(isFinal = false)
                    }
                    state.add(letter, stateTo)
                    ++maxStateCount
                    states[stateFrom] = state
                }
            }
        }
        states.forEach {
            println(it)
        }
    }

    fun checkWord(word: String) =
        println("Возможность разобрать строку \"$word\": ${parseWordStateMachine(word)}")

    private fun parseWordStateMachine(word: String): Any {
        var firstState = "q0"
        word.forEach {
            if (states[firstState] != null) {
                if (states[firstState]!!.containsInput(it.toString())) {
                    firstState = states[firstState]!!.getFirstLetter(it.toString())
                } else return false
            } else return false
        }
        return firstState.contains("f")
    }

    fun checkTerministicState() {
        if (!isDeterministic) {
            println("Автомат недетерменирован")
            nonDeterministicStates.addAll(states.map { it.toPair() })
            states = determinateStateMachine()
            isDeterministic = true
            printDeterministicStateMachine()
        } else {
            println("Автомат детерменирован")
        }
    }

    private fun printDeterministicStateMachine() {
        states.forEach { state ->
            state.value.inputs.forEach {
                println("${state.key},${it.value}=${it.value[0]}")
            }
        }
    }

    private fun determinateStateMachine(): MutableMap<String, State> {
        val deterministicStates: MutableMap<String, State> = mutableMapOf()
        while (nonDeterministicStates.isNotEmpty()) {
            val element = nonDeterministicStates.removeFirst()

            val inputs = element.second.inputs

            val newInputs: MutableMap<String, MutableList<String>> = mutableMapOf()

            inputs.forEach {
                if (it.value.size > 1) {
                    val newStateList = it.value
                    val newStateName = newStateList.joinToString("")

                    newInputs[it.key] = mutableListOf(newStateName)

                    if (!deterministicStates.containsKey(newStateName) && deterministicStates.size != 1) {
                        nonDeterministicStates.offerLast(
                            newStateName to State(isFinal = isFinal(newStateName), addInputsToNewState(newStateList))
                        )
                    }
                } else {
                    newInputs[it.key] = it.value
                }
            }
            deterministicStates[element.first] = State(element.second.isFinal, newInputs)
        }
        return deterministicStates
    }

    private fun addInputsToNewState(newStateArray: MutableList<String>): MutableMap<String, MutableList<String>> {
        val newInputs: MutableMap<String, MutableList<String>> = mutableMapOf()
        for (input in inputs) {
            val union: MutableSet<String> = mutableSetOf()
            newStateArray.forEach {
                if (states[it] != null) {
                    if (states[it]!!.containsInput(input)) {
                        union.addAll(states[it]!!.getPaths(input)!!)
                    }
                }
            }

            if (union.size > 0) {
                newInputs[input] = union.toMutableList()
            }

        }
        return newInputs
    }

    private fun isFinal(stateName: String): Boolean {
        for (final in finalStates) {
            if (stateName.contains(final)) {
                return true
            }
        }
        return false
    }

    private fun readFileDirectlyAsText(fileName: String): List<String> = File(fileName).readLines(Charsets.UTF_8)
}


class State(
    val isFinal: Boolean,
    val inputs: MutableMap<String, MutableList<String>> = mutableMapOf()
) {

    fun add(letter: String, stateTo: String): Boolean =
        if (containsInput(letter)) {
            inputs[letter]?.add(stateTo)
            true
        } else {
            inputs[letter] = mutableListOf(stateTo)
            false
        }

    fun containsInput(letter: String): Boolean = inputs.containsKey(letter)

    fun getFirstLetter(letter: String): String = getPaths(letter)!![0]

    fun getPaths(letter: String): MutableList<String>? = inputs[letter]
    override fun toString(): String {
        return "State(isFinal=$isFinal, inputs=$inputs)"
    }
}