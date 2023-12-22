import java.io.IOException

fun main() {
    val syntaxAnalyzer = SyntaxAnalyzer()
    syntaxAnalyzer.readGrammarRules("src/main/resources/grammar.txt")
    try {
        syntaxAnalyzer.analyzeFile("src/main/resources/program.txt")
      //  syntaxAnalyzer.analyze("src/main/resources/program.txt")
    } catch (e: IOException) {
        e.printStackTrace()
    }
}