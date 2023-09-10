import java.lang.RuntimeException

class IllegalExpressionException(message: String?, override val cause: Throwable? = null) : RuntimeException(message) {
}