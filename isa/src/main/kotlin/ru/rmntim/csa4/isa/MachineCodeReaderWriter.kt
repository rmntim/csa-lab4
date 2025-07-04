package ru.rmntim.csa4.isa

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path

fun readCode(filename: Path): Program = ProgramBinarySerializer.deserialize(filename.toFile().inputStream())

fun writeCode(
    program: Program,
    filename: Path,
) = ProgramBinarySerializer.serialize(program, filename.toFile().apply { createNewFile() }.outputStream())

object ProgramBinarySerializer {
    private const val MAGIC_NUMBER = 0x43534134 // "CSA4" in ASCII
    private const val VERSION = 1

    private const val TYPE_INSTRUCTION = 0x01.toByte()
    private const val TYPE_OPERAND_INSTRUCTION = 0x02.toByte()
    private const val TYPE_DATA = 0x03.toByte()

    fun serialize(
        program: Program,
        outputStream: OutputStream,
    ) {
        DataOutputStream(outputStream).use { dos ->
            // Write header
            dos.writeInt(MAGIC_NUMBER)
            dos.writeInt(VERSION)

            // Write program metadata
            dos.writeInt(program.initCommand)
            dos.writeInt(program.program.size)

            // Write memory cells
            program.program.forEach { memoryCell ->
                writeMemoryCell(dos, memoryCell)
            }
        }
    }

    fun serialize(program: Program): ByteArray {
        val baos = ByteArrayOutputStream()
        serialize(program, baos)
        return baos.toByteArray()
    }

    fun deserialize(inputStream: InputStream): Program {
        DataInputStream(inputStream).use { dis ->
            // Read and validate header
            val magic = dis.readInt()
            require(magic == MAGIC_NUMBER) { "Invalid file format: magic number mismatch" }

            val version = dis.readInt()
            require(version == VERSION) { "Unsupported version: $version" }

            // Read program metadata
            val initCommand = dis.readInt()
            val programSize = dis.readInt()

            require(programSize >= 0) { "Invalid program size: $programSize" }

            // Read memory cells
            val memoryCells = Array(programSize) { readMemoryCell(dis) }

            return Program(initCommand, memoryCells)
        }
    }

    fun deserialize(data: ByteArray): Program = deserialize(ByteArrayInputStream(data))

    private fun writeMemoryCell(
        dos: DataOutputStream,
        memoryCell: MemoryCell,
    ) {
        when (memoryCell) {
            is MemoryCell.Instruction -> {
                dos.writeByte(TYPE_INSTRUCTION.toInt())
                dos.writeInt(memoryCell.opcode.ordinal)
            }

            is MemoryCell.OperandInstruction -> {
                dos.writeByte(TYPE_OPERAND_INSTRUCTION.toInt())
                dos.writeInt(memoryCell.opcode.ordinal)
                dos.writeInt(memoryCell.operand)
            }

            is MemoryCell.Data -> {
                dos.writeByte(TYPE_DATA.toInt())
                dos.writeInt(memoryCell.value)
            }
        }
    }

    private fun readMemoryCell(dis: DataInputStream): MemoryCell {
        val type = dis.readByte()

        return when (type) {
            TYPE_INSTRUCTION -> {
                val opcodeOrdinal = dis.readInt()
                val opcode = getOpcodeByOrdinal(opcodeOrdinal)
                MemoryCell.Instruction(opcode)
            }

            TYPE_OPERAND_INSTRUCTION -> {
                val opcodeOrdinal = dis.readInt()
                val opcode = getOpcodeByOrdinal(opcodeOrdinal)
                val operand = dis.readInt()
                MemoryCell.OperandInstruction(opcode, operand)
            }

            TYPE_DATA -> {
                val value = dis.readInt()
                MemoryCell.Data(value)
            }

            else -> throw IllegalArgumentException("Unknown memory cell type: $type")
        }
    }

    private fun getOpcodeByOrdinal(ordinal: Int): Opcode {
        val opcodes = Opcode.entries.toTypedArray()
        require(ordinal > 0 && ordinal < opcodes.size) { "Invalid opcode ordinal: $ordinal" }
        return opcodes[ordinal]
    }
}
