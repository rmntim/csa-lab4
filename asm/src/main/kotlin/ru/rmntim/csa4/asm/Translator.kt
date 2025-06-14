package ru.rmntim.csa4.asm

import ru.rmntim.csa4.isa.MemoryCell
import ru.rmntim.csa4.isa.Opcode
import ru.rmntim.csa4.isa.Program
import java.io.File

/**
 * Token types for lexical analysis
 */
sealed class Token {
    data class Label(val name: String, val line: Int) : Token()
    data class Instruction(val opcode: Opcode, val operand: String?, val line: Int) : Token()
    data class Comment(val text: String, val line: Int) : Token()
    object Empty : Token()
}

/**
 * Represents different types of assembly instructions with their operand requirements
 */
enum class InstructionType {
    NO_OPERAND,        // nop, load, store, add, etc.
    OPERAND_REQUIRED,  // lit, word, buf
    DATA_DECLARATION   // string
}

/**
 * Configuration for instruction parsing
 */
data class InstructionConfig(
    val opcode: Opcode,
    val type: InstructionType,
    val validator: (String) -> Boolean = { true }
)

/**
 * Registry of all supported instructions with their configurations
 */
private val INSTRUCTION_REGISTRY = mapOf(
    // No operand instructions
    "nop" to InstructionConfig(Opcode.NOP, InstructionType.NO_OPERAND),
    "load" to InstructionConfig(Opcode.LOAD, InstructionType.NO_OPERAND),
    "store" to InstructionConfig(Opcode.STORE, InstructionType.NO_OPERAND),
    "add" to InstructionConfig(Opcode.ADD, InstructionType.NO_OPERAND),
    "sub" to InstructionConfig(Opcode.SUB, InstructionType.NO_OPERAND),
    "mul" to InstructionConfig(Opcode.MUL, InstructionType.NO_OPERAND),
    "div" to InstructionConfig(Opcode.DIV, InstructionType.NO_OPERAND),
    "mod" to InstructionConfig(Opcode.MOD, InstructionType.NO_OPERAND),
    "inc" to InstructionConfig(Opcode.INC, InstructionType.NO_OPERAND),
    "dec" to InstructionConfig(Opcode.DEC, InstructionType.NO_OPERAND),
    "drop" to InstructionConfig(Opcode.DROP, InstructionType.NO_OPERAND),
    "dup" to InstructionConfig(Opcode.DUP, InstructionType.NO_OPERAND),
    "swap" to InstructionConfig(Opcode.SWAP, InstructionType.NO_OPERAND),
    "over" to InstructionConfig(Opcode.OVER, InstructionType.NO_OPERAND),
    "and" to InstructionConfig(Opcode.AND, InstructionType.NO_OPERAND),
    "or" to InstructionConfig(Opcode.OR, InstructionType.NO_OPERAND),
    "xor" to InstructionConfig(Opcode.XOR, InstructionType.NO_OPERAND),
    "jz" to InstructionConfig(Opcode.JZ, InstructionType.NO_OPERAND),
    "jn" to InstructionConfig(Opcode.JN, InstructionType.NO_OPERAND),
    "jump" to InstructionConfig(Opcode.JUMP, InstructionType.NO_OPERAND),
    "call" to InstructionConfig(Opcode.CALL, InstructionType.NO_OPERAND),
    "ret" to InstructionConfig(Opcode.RET, InstructionType.NO_OPERAND),
    "in" to InstructionConfig(Opcode.IN, InstructionType.NO_OPERAND),
    "out" to InstructionConfig(Opcode.OUT, InstructionType.NO_OPERAND),
    "halt" to InstructionConfig(Opcode.HALT, InstructionType.NO_OPERAND),
    
    // Operand required instructions
    "lit" to InstructionConfig(Opcode.LIT, InstructionType.OPERAND_REQUIRED) { operand ->
        operand.toIntOrNull() != null || isValidLabel(operand)
    },
    "word" to InstructionConfig(Opcode.WORD, InstructionType.OPERAND_REQUIRED) { operand ->
        operand.toIntOrNull() != null || isValidLabel(operand)
    },
    "buf" to InstructionConfig(Opcode.BUF, InstructionType.OPERAND_REQUIRED) { operand ->
        operand.toIntOrNull() != null && operand.toInt() > 0
    },
    
    // Data declarations
    "string" to InstructionConfig(Opcode.STRING, InstructionType.DATA_DECLARATION) { operand ->
        operand.matches("\".*\"".toRegex())
    }
)

/**
 * Validates if a string is a valid label name according to EBNF grammar
 */
private fun isValidLabel(name: String): Boolean {
    if (name.isEmpty()) return false
    if (!name[0].isLetter()) return false
    return name.all { it.isLetterOrDigit() || it == '_' }
}

/**
 * Lexer for tokenizing assembly source code
 */
class AssemblyLexer {
    fun tokenize(source: String): List<Token> {
        val tokens = mutableListOf<Token>()
        
        source.lines().forEachIndexed { lineIndex, line ->
            val lineNumber = lineIndex + 1
            val token = tokenizeLine(line, lineNumber)
            if (token != Token.Empty) {
                tokens.add(token)
            }
        }
        
        return tokens
    }
    
    private fun tokenizeLine(line: String, lineNumber: Int): Token {
        // Remove comments and trim
        val meaningfulPart = line.split(";")[0].trim()
        
        if (meaningfulPart.isEmpty()) {
            return Token.Empty
        }
        
        // Check if it's a label
        if (meaningfulPart.endsWith(":")) {
            val labelName = meaningfulPart.substringBefore(":").trim()
            if (!isValidLabel(labelName)) {
                throw SyntaxError("Invalid label name '$labelName'", lineNumber)
            }
            return Token.Label(labelName.lowercase(), lineNumber)
        }
        
        // Parse instruction
        return parseInstruction(meaningfulPart, lineNumber)
    }
    
    private fun parseInstruction(instruction: String, lineNumber: Int): Token {
        val parts = instruction.split(" ", limit = 2)
        val mnemonic = parts[0].lowercase()
        val operand = if (parts.size > 1) parts[1] else null
        
        val config = INSTRUCTION_REGISTRY[mnemonic]
            ?: throw SyntaxError("Unknown instruction '$mnemonic'", lineNumber)
        
        // Validate operand requirements
        when (config.type) {
            InstructionType.NO_OPERAND -> {
                if (operand != null) {
                    throw SyntaxError("Instruction '$mnemonic' does not accept operands", lineNumber)
                }
            }
            InstructionType.OPERAND_REQUIRED, InstructionType.DATA_DECLARATION -> {
                if (operand.isNullOrBlank()) {
                    throw SyntaxError("Instruction '$mnemonic' requires an operand", lineNumber)
                }
                if (!config.validator(operand)) {
                    throw SyntaxError("Invalid operand '$operand' for instruction '$mnemonic'", lineNumber)
                }
            }
        }
        
        return Token.Instruction(config.opcode, operand, lineNumber)
    }
}

/**
 * Represents an instruction with optional label reference for second pass
 */
data class LabelInstruction(val instruction: MemoryCell, val label: String = "")

/**
 * Instruction generator for converting tokens to machine code
 */
class InstructionGenerator {
    fun generateInstruction(token: Token.Instruction): List<LabelInstruction> {
        return when (token.opcode) {
            Opcode.WORD -> generateWordInstruction(token.operand!!)
            Opcode.BUF -> generateBufferInstruction(token.operand!!)
            Opcode.STRING -> generateStringInstruction(token.operand!!)
            Opcode.LIT -> generateLiteralInstruction(token.operand!!)
            else -> generateSimpleInstruction(token.opcode)
        }
    }
    
    private fun generateWordInstruction(operand: String): List<LabelInstruction> {
        return if (operand.toIntOrNull() != null) {
            listOf(LabelInstruction(MemoryCell.Data(operand.toInt())))
        } else {
            listOf(LabelInstruction(MemoryCell.Data(), operand.lowercase()))
        }
    }
    
    private fun generateBufferInstruction(operand: String): List<LabelInstruction> {
        val size = operand.toInt()
        return List(size) { LabelInstruction(MemoryCell.Data(0)) }
    }
    
    private fun generateStringInstruction(operand: String): List<LabelInstruction> {
        // Remove quotes and process escape sequences
        var stringValue = operand.drop(1).dropLast(1)
        stringValue = stringValue.replace("\\n", "\n")
        
        val instructions = mutableListOf<LabelInstruction>()
        // First cell contains string length (Pascal string format)
        instructions.add(LabelInstruction(MemoryCell.Data(stringValue.length)))
        // Following cells contain character codes
        instructions.addAll(stringValue.map { 
            LabelInstruction(MemoryCell.Data(it.code)) 
        })
        
        return instructions
    }
    
    private fun generateLiteralInstruction(operand: String): List<LabelInstruction> {
        return if (operand.toIntOrNull() != null) {
            listOf(LabelInstruction(MemoryCell.OperandInstruction(Opcode.LIT, operand.toInt())))
        } else {
            listOf(LabelInstruction(MemoryCell.Instruction(Opcode.LIT), operand.lowercase()))
        }
    }
    
    private fun generateSimpleInstruction(opcode: Opcode): List<LabelInstruction> {
        return listOf(LabelInstruction(MemoryCell.Instruction(opcode)))
    }
}

/**
 * Parser for converting tokens to intermediate representation
 */
class AssemblyParser {
    private val instructionGenerator = InstructionGenerator()
    
    fun parse(tokens: List<Token>): ParseResult {
        val labels = mutableMapOf<String, Int>()
        val instructions = mutableListOf<LabelInstruction>()
        var currentAddress = 0
        
        for (token in tokens) {
            when (token) {
                is Token.Label -> {
                    if (labels.containsKey(token.name)) {
                        throw SyntaxError("Duplicate label '${token.name}'", token.line)
                    }
                    labels[token.name] = currentAddress
                }
                is Token.Instruction -> {
                    val generated = instructionGenerator.generateInstruction(token)
                    instructions.addAll(generated)
                    currentAddress += generated.size
                }
                else -> { /* Skip comments and empty tokens */ }
            }
        }
        
        return ParseResult(labels, instructions)
    }
}

/**
 * Result of the first parsing pass
 */
data class ParseResult(
    val labels: Map<String, Int>,
    val instructions: List<LabelInstruction>
)

/**
 * Code generator for the second pass - resolves labels to addresses
 */
class CodeGenerator {
    fun generate(parseResult: ParseResult): Program {
        val resolvedInstructions = mutableListOf<MemoryCell>()
        
        for (labelInstruction in parseResult.instructions) {
            if (labelInstruction.label.isEmpty()) {
                resolvedInstructions.add(labelInstruction.instruction)
            } else {
                val resolvedInstruction = resolveLabelReference(
                    labelInstruction,
                    parseResult.labels
                )
                resolvedInstructions.add(resolvedInstruction)
            }
        }
        
        val startAddress = parseResult.labels["start"]
            ?: throw SyntaxError("Program must have a 'start:' label", 0)
        
        return Program(startAddress, resolvedInstructions.toTypedArray())
    }
    
    private fun resolveLabelReference(
        labelInstruction: LabelInstruction,
        labels: Map<String, Int>
    ): MemoryCell {
        val address = labels[labelInstruction.label]
            ?: throw NoSuchLabelException(labelInstruction.label)
        
        return when (val memoryCell = labelInstruction.instruction) {
            is MemoryCell.Instruction -> {
                MemoryCell.OperandInstruction(memoryCell.opcode, address)
            }
            is MemoryCell.Data -> {
                MemoryCell.Data(address)
            }
            else -> memoryCell
        }
    }
}

/**
 * Main translator class that orchestrates the translation process
 */
class AssemblyTranslator {
    private val lexer = AssemblyLexer()
    private val parser = AssemblyParser()
    private val codeGenerator = CodeGenerator()
    
    fun translate(source: String): Program {
        try {
            val tokens = lexer.tokenize(source)
            val parseResult = parser.parse(tokens)
            return codeGenerator.generate(parseResult)
        } catch (e: SyntaxError) {
            throw TranslationException("Syntax error at line ${e.line}: ${e.message}")
        } catch (e: NoSuchLabelException) {
            throw TranslationException("Undefined label: ${e.label}")
        }
    }
}

/**
 * Exception classes for better error reporting
 */
class SyntaxError(message: String, val line: Int) : Exception(message)
class TranslationException(message: String) : Exception(message)
class NoSuchLabelException(val label: String) : Exception("No such label: $label")

/**
 * This function translates code provided in {filename} file
 * to machine code for stack computer
 *
 * Please, remember to put the label "start:" before first instruction!
 */
fun translateAsm(filename: String): Program {
    val source = File(filename).readText(Charsets.UTF_8)
    val translator = AssemblyTranslator()
    return translator.translate(source)
}