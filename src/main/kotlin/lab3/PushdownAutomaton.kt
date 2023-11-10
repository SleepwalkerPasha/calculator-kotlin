package lab3

import java.io.File
import java.util.*

class PushdownAutomaton(
    val P: MutableSet<String> = mutableSetOf(),
    val Z: MutableSet<String> = mutableSetOf(),
    val S: MutableList<String> = mutableListOf(),
    val commands: MutableMap<AutomatonConfiguration, MutableSet<AutomatonConfiguration>> = mutableMapOf(),
    val finishConfiguration: AutomatonConfiguration = AutomatonConfiguration(START_STATE, LAMBDA, LAMBDA),
    val finishCommand: AutomatonConfiguration = AutomatonConfiguration(START_STATE, LAMBDA, END_MARKER),
    val usedConfiguration: MutableMap<Int, Pair<Int, AutomatonConfiguration>> = mutableMapOf(),
    val bfs: LinkedList<Pair<Int, AutomatonConfiguration>> = LinkedList(),
    val fileName: String
) {

    lateinit var startConfiguration: AutomatonConfiguration

    var I: String


    init {
        Z.add(END_MARKER)
        S.add(START_STATE)
        I = ""
        readGramar(fileName)
    }

    private fun readGramar(fileName: String) {
        readFileDirectlyAsText("src/main/resources/$fileName.txt").forEach {
            it.replace("| |", "|~|")
            it.replace(" ", "")
            if (!it.matches(Regex("^[A-Z]>.+(|.+)+"))) {
                throw RuntimeException("Неверный формат ввода")
            }
            analyzeString(it)
        }
        makeSecondCommands()
        addNewCommand(
            AutomatonConfiguration(START_STATE, LAMBDA, END_MARKER),
            AutomatonConfiguration(START_STATE, LAMBDA, LAMBDA)
        )
        printGramatic()
    }

    private fun printGramatic() {
        commands.keys.forEach { key ->
            commands[key]!!.forEach { value ->
                println("$key -> $value")
            }
        }
    }

    private fun addNewCommand(
        keyConfiguration: AutomatonConfiguration,
        addedConfiguration: AutomatonConfiguration
    ) {
        val automatonConfigurations = commands[keyConfiguration] ?: hashSetOf()
        automatonConfigurations.add(addedConfiguration)
        commands[keyConfiguration] = automatonConfigurations
    }

    private fun makeSecondCommands() {
        P.forEach { term ->
            S.forEach { state ->
                addNewCommand(AutomatonConfiguration(state, term, term), AutomatonConfiguration(state, LAMBDA, LAMBDA))
            }
        }
    }

    private fun analyzeString(line: String) {
        val leftPart = line.substringBefore(">")
        if (I.isEmpty()) {
            I = leftPart
        }
        initPZ(leftPart)
        val rightPart = line.substringAfter(">")
        initPZ(rightPart)
        makeFirstCommands(leftPart, rightPart)
    }

    private fun makeFirstCommands(leftPart: String, rightPart: String) {
        val split = rightPart.split("|")
        S.forEach { state ->
            split.forEach {
                addNewCommand(
                    AutomatonConfiguration(state, LAMBDA, leftPart),
                    AutomatonConfiguration(state, LAMBDA, it.reversed())
                )
            }
        }
    }


    private fun initPZ(leftPart: String) =
        leftPart.forEach {
            if (it != '|') {
                if (it <= 'A' || it >= 'Z') {
                    P.add(it.toString())
                }
                Z.add(it.toString())
            }
        }

    private fun readFileDirectlyAsText(fileName: String): List<String> = File(fileName).readLines(Charsets.UTF_8)

    fun checkStr(input: String) {
        startConfiguration = AutomatonConfiguration(START_STATE, input, END_MARKER + I)
        bfs.offer(-1 to startConfiguration);
        checkRecursion(0)
        if (!bfs.isEmpty() && bfs.peek()?.second == finishCommand) {
            print("$finishConfiguration <= ")
            var now = bfs.peek()
            while (now.first >= 0) {
                print("${now.second} <= ")
                now = usedConfiguration[now.first]
            }
            println(startConfiguration)
        } else {
            println("Цепочка символов недопустима")
        }
    }

    private fun checkRecursion(count: Int) {
        if (bfs.isEmpty() || bfs.peek().second == finishCommand) {
            return
        }
        val pair = bfs.poll()
        val configuration = pair.second
        usedConfiguration[count] = pair
        if (configuration.automatonChain.length <= configuration.inputChain.length + 2) {
            if (configuration.inputChain[0] != configuration.automatonChain[configuration.automatonChain.length - 1]) {
                val first = AutomatonConfiguration(
                    START_STATE,
                    LAMBDA,
                    configuration.automatonChain.substring(configuration.automatonChain.length - 1)
                )
                if (commands.containsKey(first)) {
                    commands[first]!!.forEach {
                        val copy = configuration.copy()
                        copy.automatonChain = copy.automatonChain.substring(0, copy.automatonChain.length - 1) + it.automatonChain
                        if (findFirstBig(copy.automatonChain) > -1 && copy.automatonChain.substring(2)
                                .contentEquals(copy.inputChain.reversed())
                        ) {
                            bfs.offer(count to copy)
                        } else {
                            if (findFirstBig(copy.automatonChain) > 2) {
                                val substring = copy.automatonChain.substring(2, findFirstBig(copy.automatonChain))
                                if (substring.contentEquals(
                                        copy.inputChain.substring(copy.inputChain.length - substring.length).reversed()
                                    )
                                ) {
                                    bfs.offer(count to copy)
                                }
                            } else {
                                bfs.offer(count to copy)
                            }
                        }
                    }
                }
            } else {
                val copy = configuration.copy()
                copy.automatonChain = copy.automatonChain.substring(0, copy.automatonChain.length - 1)
                copy.inputChain = copy.inputChain.substring(1)
                if (copy.inputChain.isEmpty()) {
                    copy.inputChain = LAMBDA
                }
                bfs.offer(count to copy)
            }
        }
        checkRecursion(count + 1)
    }

    private fun findFirstBig(automatonChain: String): Int {
        for (i in automatonChain.indices) {
            if (automatonChain[i] in 'A'..'Z') {
                return i
            }
        }
        return -1
    }


    companion object {
        private const val START_STATE = "s0"
        private const val END_MARKER = "h0"
        private const val LAMBDA = "lambda"
    }
}