package ru.rmntim.csa4.asm

import ru.rmntim.csa4.isa.writeCode
import java.nio.file.Path

fun main(args: Array<String>) {
    val sourceFile = args[0]
    val targetFile = args[1]

    val code = translateAsm(sourceFile)
    writeCode(code, Path.of(targetFile))
}
