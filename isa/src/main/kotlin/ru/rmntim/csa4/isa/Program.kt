package ru.rmntim.csa4.isa

import kotlinx.serialization.Serializable

@Serializable
data class Program(
    val initCommand: Int,
    val program: Array<MemoryCell>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Program) return false

        if (initCommand != other.initCommand) return false
        if (!program.contentEquals(other.program)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = initCommand
        result = 31 * result + program.contentHashCode()
        return result
    }
}
