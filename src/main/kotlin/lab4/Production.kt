import java.lang.StringBuilder

class Production(
    var isSynchroError: Boolean = false
) {
    val symbols = mutableListOf<Symbol>()

    fun addSymbolToProduction(symbol: Symbol) {
        symbols.add(symbol)
    }

    private fun getEmpty(): Symbol? {
        if (symbols.size != 1) {
            return null
        }
        val eps = first()
        if (eps.value == "") {
            return eps
        }
        return null
    }

    fun isEps(): Boolean = getEmpty() != null
    fun first(): Symbol = symbols[0]

    override fun toString(): String {
        if (isSynchroError) {
            return "SYNCHRO_ERROR"
        }
        val retVal = StringBuilder()
        for (symbol in symbols) {
            retVal.append(symbol.toString())
        }
        return retVal.toString()
    }
}
