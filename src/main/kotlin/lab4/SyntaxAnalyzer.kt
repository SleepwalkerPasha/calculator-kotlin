import java.io.IOException
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Stack
import java.util.function.Predicate

class SyntaxAnalyzer {
    private var startSymbol: Symbol? = null
    private val grammars: MutableMap<Symbol, Set<Production>> = mutableMapOf()
    private val nonTerminals = mutableSetOf<Symbol>()
    private val terminals = mutableSetOf<Symbol>()

    private val firsts: MutableMap<Symbol, MutableSet<Symbol>> = mutableMapOf()
    private val follows: MutableMap<Symbol, MutableSet<Symbol>> = mutableMapOf()

    private val mMatrix: MutableMap<Symbol, MutableMap<Symbol, Production>> = mutableMapOf()
    private val synchroMap: MutableMap<Symbol, MutableSet<Symbol>> = mutableMapOf()

    fun readGrammarRules(path: String) {
        try {
            val lines = Files.readAllLines(Paths.get(path))
            for (line in lines) {
                var bufferSymbol = ""
                var symType: SymType? = null
                var grammarKey: Symbol? = null
                val grammarValue: MutableSet<Production> = mutableSetOf()
                var production = Production()

                for (char in line.chars()) {
                    when (char.toChar()) {
                        '<' -> {
                            if (symType == null || symType == SymType.SPEC) {
                                symType = SymType.NON_TER
                            } else if (symType == SymType.TER) {
                                bufferSymbol += "<"
                            }
                        }

                        '>' -> {
                            if (symType != null && symType == SymType.NON_TER) {
                                val symbol = Symbol(bufferSymbol, false)
                                nonTerminals.add(symbol)
                                if (startSymbol == null) {
                                    startSymbol = symbol
                                }
                                if (grammarKey == null) {
                                    grammarKey = symbol
                                } else {
                                    production.addSymbolToProduction(symbol)
                                }
                                symType = null
                                bufferSymbol = ""
                            } else if (symType != null && symType == SymType.TER) {
                                bufferSymbol += ">"
                            }
                        }

                        ':' -> {
                            if (symType == null) {
                                symType = SymType.SPEC
                            } else if (symType == SymType.TER) {
                                bufferSymbol += ":"
                            }
                        }

                        '\'' -> {
                            if (symType == null || symType == SymType.SPEC) {
                                symType = SymType.TER
                            } else if (symType == SymType.TER) {
                                val symbol = Symbol(bufferSymbol, true)
                                terminals.add(symbol)
                                production.addSymbolToProduction(symbol)
                                bufferSymbol = ""
                                symType = null
                            }
                        }

                        ' ' -> {
                            val symbol = Symbol(" ", true)
                            terminals.add(symbol)
                            production.addSymbolToProduction(symbol)
                            bufferSymbol = ""
                            symType = null
                        }

                        '|' -> {
                            if (symType != null && symType == SymType.SPEC) {
                                val symbol = Symbol("", true)
                                production.addSymbolToProduction(symbol)
                                bufferSymbol = ""
                                symType = null
                            }
                            grammarValue.add(production)
                            production = Production()
                        }

                        else -> {
                            bufferSymbol += char.toChar()
                        }
                    }
                }
                grammarValue.add(production)
                grammars[grammarKey!!] = grammarValue

            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        for (entry in grammars.entries) {
            val nonTerminal = entry.key
            val firstSet = first(nonTerminal)
            firsts[nonTerminal] = firstSet
        }
        follow()
        fillMatrix()
        fillSynchroMap()
    }

    private fun first(symbol: Symbol): MutableSet<Symbol> {
        val eps = Symbol("", true)
        var hasEps = false
        var hasIncluded = false
        val resultSet: MutableSet<Symbol> = mutableSetOf()

        if (symbol.isTerminal) {
            resultSet.add(symbol)
            return resultSet
        }

        for (prod in grammars[symbol]!!) {
            val firstSymbol = prod.first()
            if (firstSymbol.isTerminal) {
                resultSet.add(firstSymbol)
                if (firstSymbol == eps) {
                    hasEps = false
                }
            } else {
                val firstFromFirstSymbol: MutableSet<Symbol> = first(firstSymbol)
                val sizeBeforeRemove = firstFromFirstSymbol.size
                firstFromFirstSymbol.removeIf(Predicate.isEqual(eps)) // todo посмотреть как правильно
                if (sizeBeforeRemove == firstFromFirstSymbol.size) {
                    hasEps = false
                } else if (!hasIncluded) {
                    hasEps = true
                }
                hasIncluded = true
                resultSet.addAll(firstFromFirstSymbol)
            }
        }
        if (hasEps) { // todo возможно здесь наоборот если не содержит????
            resultSet.add(eps)
        }
        return resultSet
    }

    private fun follow() {
        val eps = Symbol("", true)
        for (entry in grammars.entries) {
            val productionSet: MutableSet<Production> = entry.value.toMutableSet()
            for (prod in productionSet) {
                var i = 0
                while (i < prod.symbols.size - 1) {
                    val currentSymbol = prod.symbols[i]
                    val nextSymbol = prod.symbols[i + 1]

                    if (!follows.containsKey(currentSymbol)) {
                        follows[currentSymbol] = mutableSetOf()
                        if (currentSymbol == startSymbol) {
                            follows[currentSymbol]?.add(eps)
                        }
                    }

                    val nextSymbolFirsts = mutableSetOf<Symbol>()
                    if (nextSymbol.isTerminal && nextSymbol != eps) {
                        nextSymbolFirsts.add(nextSymbol)
                    } else {
                        nextSymbolFirsts.addAll(firsts[nextSymbol]!!)
                        nextSymbolFirsts.remove(eps)
                    }
                    follows[currentSymbol]!!.addAll(nextSymbolFirsts)
                    ++i
                }
            }
        }

        for (entry in grammars.entries) {
            val nonTerminal = entry.key
            if (!follows.containsKey(nonTerminal)) {
                follows[nonTerminal] = mutableSetOf()
                if (nonTerminal == startSymbol) {
                    follows[nonTerminal]!!.add(eps)
                }
            }

            val otherSynchroTokens = mutableSetOf(
                Symbol(")", true),
                Symbol("}", true),
                Symbol(";", true)
            )
            follows[nonTerminal]!!.addAll(otherSynchroTokens)
          //  println("Символ: '$nonTerminal'")
          //  println(follows[nonTerminal]!!)

            val productionSet = entry.value
            for (prod in productionSet) {
                var i = 0
                while (i < prod.symbols.size) {
                    val currentSymbol = prod.symbols[i]
                    if (!follows.containsKey(currentSymbol)) {
                        follows[currentSymbol] = mutableSetOf()
                        if (currentSymbol == startSymbol) {
                            follows[currentSymbol]!!.add(eps)
                        }
                    }
                    var nextSymbolFirsts: MutableSet<Symbol> = mutableSetOf()
                    if (i != prod.symbols.size - 1) {
                        val nextSymbol = prod.symbols[i + 1]
                        if (nextSymbol.isTerminal) {
                            nextSymbolFirsts = mutableSetOf()
                            nextSymbolFirsts.add(nextSymbol)
                        } else {
                            nextSymbolFirsts = firsts[nextSymbol]!!
                        }
                    }
                    if (i == prod.symbols.size - 1 || nextSymbolFirsts.contains(eps)) {
                        follows[currentSymbol]!!.addAll(follows[nonTerminal]!!)
                    }
                    ++i
                }
            }
        }
    }

    private fun fillMatrix() {
        val eps = Symbol("", true)
        for (nonTerminal in nonTerminals) {
            mMatrix[nonTerminal] = mutableMapOf()
        }
        for (entry in grammars.entries) {
            val nonTerminal = entry.key
            val productions = entry.value
            for (prod in productions) {
                for (terminal in first(prod.first())) {
                    mMatrix[nonTerminal]!![terminal] = prod
                    if (prod.symbols.contains(eps)) {
                        for (b in follows[nonTerminal]!!) {
                            mMatrix[nonTerminal]!![b] = prod
                        }
                    }
                }
            }
            for (follow in follows[nonTerminal]!!) {
                if (mMatrix[nonTerminal]!![follow] == null) {
                    mMatrix[nonTerminal]!![follow] = Production(true)
                }
            }
        }
    }

    private fun fillSynchroMap() {
        for (nonTerminal in nonTerminals) {
            synchroMap[nonTerminal] = HashSet()
            synchroMap[nonTerminal]!!.addAll(follows[nonTerminal]!!)
            synchroMap[nonTerminal]!!.addAll(firsts[nonTerminal]!!)
        }
    }

    private fun getTerminalFromStr(str: String): Symbol? {
        var terminal: Symbol? = null
        for (symbol in terminals) {
            if (str.startsWith(symbol.value) && symbol.value != "") {
                if (terminal == null || symbol.value.length > terminal.value.length) {
                    terminal = symbol
                }
            }
        }
        return terminal
    }


    fun analyzeFile(analyzedFile: String) {
        val writer = PrintWriter("table.csv", StandardCharsets.UTF_8)
        writer.println("Cтек,Вход,Примечание")
        val path = Paths.get(analyzedFile)
        var data = Files.readAllLines(path).joinToString("")
        val stack = Stack<Symbol>()
        stack.push(startSymbol)
        var terminal: Symbol?
        var log: String
        var errorNum = 1

        while (stack.isNotEmpty()) {
            log = "\"${printStackSymbols(stack)}\",\"$data\""
            terminal = getTerminalFromStr(data)
            if (stack.peek().value == " ") {
                stack.pop()
            }

            if (terminal != null) {
                if (terminal.value == " ") {
                    data = data.substring(terminal.value.length)
                    continue
                }
                if (stack.peek().isTerminal && stack.peek().equals(terminal)) {
                    stack.pop()
                    data = data.substring(terminal.value.length)
                    writer.println(log)
                } else {
                    if (stack.peek().isTerminal && !stack.peek().equals(terminal)) {
                        log += ",\"Ошибка: несоответствие терминалов в стеке и строке\""
                        writer.println(log)
                        printError(data, terminal.value, stack, errorNum)
                        data = data.substring(1)
                        ++errorNum
                        continue
                    }

                    val matrixCell = mMatrix[stack.peek()]!![terminal]
                    if (matrixCell == null) {
                        var hasEmptyTransition = false
                        for (prod in grammars[stack.peek()]!!) {
                            if (prod.isEps()) {
                                hasEmptyTransition = true
                            }
                        }
                        log += ", \"Ошибка: "
                        if (hasEmptyTransition) {
                            log += "снимаем ${stack.peek()} со стека\""
                            writer.println(log)
                            stack.pop()
                        } else {
                            while (!synchroMap[stack.peek()]!!.contains(terminal)) {
                                log += "Синхронизирующее множество для ${stack.peek()} не содержит терминала" +
                                    " ${terminal!!.value}. Удаляем со входного потока: ${terminal.value}\""
                                println(synchroMap[stack.peek()])
                                writer.println(log)
                                printError(data, terminal.value, stack, errorNum)
                                ++errorNum
                                data = data.substring(terminal.value.length)
                                terminal = getTerminalFromStr(data)
                                if (terminal!!.value == " ") {
                                    data = "${firsts[stack.peek()]!!.toList()[0]}$data"
                                }
                                break
                            }
                        }

                    } else {
                        if (matrixCell.isSynchroError) {
                            if (stack.size == 1) {
                                log += "\"Ошибка: пропускаем ${terminal.value}\""
                                writer.println(log)
                                printError(data, terminal.value, stack, errorNum)
                                ++errorNum
                                data = data.substring(terminal.value.length)
                            } else {
                                log += ",\"Ошибка: M[${stack.peek()}, ${terminal.value}] = Synch\""
                                writer.println(log)

                                printError(data, terminal.value, stack, errorNum)
                                ++errorNum
                                stack.pop()
                            }
                        } else {
                            writer.println(log)
                            stack.pop()
                            if (!matrixCell.isEps()) {
                                val iterator: ListIterator<Symbol> =
                                    matrixCell.symbols.listIterator(matrixCell.symbols.size)
                                while (iterator.hasPrevious()) {
                                    stack.push(iterator.previous())
                                }
                            }
                        }
                    }
                }
            } else {
                if (data.isNotEmpty()) {
                    log += ",\"Ошибка: неопознанный символ ${
                        data.substring(
                            0,
                            1
                        )
                    }\" в файле. Такого символа нет в алфавите терминалов"
                    writer.println(log)
                    printError(data, data.substring(0, 1), stack, errorNum)
                    ++errorNum
                    data = data.substring(1)
                } else {
                    writer.println(log)
                    stack.pop()
                }
            }
        }
        writer.close()
    }

    private fun printError(data: String, terminalInStr: String, stack: Stack<Symbol>, errorNum: Int) {
        println("\nОшибка №$errorNum")
        if (stack.peek().isTerminal) {
            println(
                "Ошибка в символе '$terminalInStr' в начале '$data' ожидается значение '${stack.peek().value}' " +
                    "\nStack: ${printStackSymbols(stack)}"
            )
        } else {
            println(
                "Ошибка в символе '$terminalInStr' в начале '$data'. Ожидается одно из ${firsts[stack.peek()]}" +
                    "\nStack: ${printStackSymbols(stack)}."
            )
        }
    }

    private fun printStackSymbols(stack: Stack<Symbol>): String {
        //    stack.reverse()
        return stack.joinToString("")
    }

}

enum class SymType {
    TER,
    NON_TER,
    SPEC
}