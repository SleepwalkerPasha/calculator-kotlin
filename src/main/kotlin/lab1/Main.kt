package lab1

fun main() {
    while (true) {
        try {
            readlnOrNull()?.let {
                println(calculate(it))
            }
        } catch (ex: Throwable) {
            println(ex)
        }
    }
}