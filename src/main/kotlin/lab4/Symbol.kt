class Symbol(
    val value: String,
    val isTerminal: Boolean
) {
    override fun toString(): String =
        if (!isTerminal) {
            "<$value>"
        } else if (value == "") {
            "$"
        } else value

    override fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other == null || other !is Symbol) {
            false
        } else {
            isTerminal == other.isTerminal && value == other.value
        }
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + isTerminal.hashCode()
        return result
    }
}