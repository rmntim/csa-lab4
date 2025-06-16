package ru.rmntim.csa4.isa

import kotlinx.serialization.Serializable

@Serializable
sealed class MemoryCell {
    @Serializable
    data class Instruction(
        val opcode: Opcode,
    ) : MemoryCell()

    @Serializable
    data class OperandInstruction(
        val opcode: Opcode,
        val operand: Int = 0,
    ) : MemoryCell()

    @Serializable
    data class Data(
        var value: Int = 0,
    ) : MemoryCell()
}
