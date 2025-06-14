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
    data class Directive(val name: String, val operand: String?, val line: Int) : Token()
    data class MacroDefinition(val name: String, val parameters: List<String>, val line: Int) : Token()
    data class MacroCall(val name: String, val arguments: List<String>, val line: Int) : Token()
    data class MacroEnd(val line: Int) : Token()
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
 * Represents assembly sections
 */
enum class Section {
    TEXT,  // Code section
    DATA   // Data section
}

/**
 * Represents a macro definition
 */
data class MacroDefinition(
    val name: String,
    val parameters: List<String>,
    val body: List<String> // Raw lines of the macro body
)

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
        operand.toIntOrNull() != null || isValidLabel(operand) || isValidCharacterLiteral(operand)
    },
    "word" to InstructionConfig(Opcode.WORD, InstructionType.OPERAND_REQUIRED) { operand ->
        operand.toIntOrNull() != null || isValidLabel(operand) || isValidCharacterLiteral(operand)
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
 * Validates if a string is a valid character literal and returns its ASCII value
 */
private fun parseCharacterLiteral(literal: String): Int? {
    if (!literal.startsWith('\'') || !literal.endsWith('\'')) {
        return null
    }
    
    val content = literal.substring(1, literal.length - 1)
    
    return when {
        content.isEmpty() -> null
        content.length == 1 -> content[0].code
        content.length == 2 && content.startsWith('\\') -> {
            when (content[1]) {
                'n' -> 10  // newline
                't' -> 9   // tab
                'r' -> 13  // carriage return
                '\\' -> 92 // backslash
                '\'' -> 39 // single quote
                '0' -> 0   // null character
                else -> null
            }
        }
        else -> null
    }
}

/**
 * Checks if a string is a valid character literal
 */
private fun isValidCharacterLiteral(literal: String): Boolean {
    return parseCharacterLiteral(literal) != null
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
        
        // Check if it's a directive
        if (meaningfulPart.startsWith(".")) {
            return parseDirective(meaningfulPart, lineNumber)
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
        val parts = instruction.split(" ")
        val mnemonic = parts[0].lowercase()
        val arguments = if (parts.size > 1) parts.drop(1) else emptyList()
        
        val config = INSTRUCTION_REGISTRY[mnemonic]
        
        if (config == null) {
            // If it's not a known instruction, it might be a macro call
            return Token.MacroCall(mnemonic, arguments, lineNumber)
        }
        
        // Handle regular instructions
        val operand = if (arguments.isNotEmpty()) arguments.joinToString(" ") else null
        
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
    
    private fun parseDirective(directive: String, lineNumber: Int): Token {
        val parts = directive.split(" ", limit = 2)
        val directiveName = parts[0].lowercase()
        val operand = if (parts.size > 1) parts[1] else null
        
        when (directiveName) {
            ".org" -> {
                if (operand.isNullOrBlank()) {
                    throw SyntaxError("Directive '.org' requires an address operand", lineNumber)
                }
                val address = operand.toIntOrNull()
                if (address == null || address < 0) {
                    throw SyntaxError("Invalid address '$operand' for .org directive", lineNumber)
                }
            }
            ".data", ".text" -> {
                if (operand != null) {
                    throw SyntaxError("Directive '$directiveName' does not accept operands", lineNumber)
                }
            }
            ".macro" -> {
                if (operand.isNullOrBlank()) {
                    throw SyntaxError("Directive '.macro' requires a name and optional parameters", lineNumber)
                }
                return parseMacroDefinition(operand, lineNumber)
            }
            ".endmacro" -> {
                if (operand != null) {
                    throw SyntaxError("Directive '.endmacro' does not accept operands", lineNumber)
                }
                return Token.MacroEnd(lineNumber)
            }
            else -> {
                throw SyntaxError("Unknown directive '$directiveName'", lineNumber)
            }
        }
        
        return Token.Directive(directiveName, operand, lineNumber)
    }
    
    private fun parseMacroDefinition(operand: String, lineNumber: Int): Token {
        val parts = operand.split(" ")
        val macroName = parts[0]
        val parameters = if (parts.size > 1) parts.drop(1) else emptyList()
        
        if (!isValidLabel(macroName)) {
            throw SyntaxError("Invalid macro name '$macroName'", lineNumber)
        }
        
        for (param in parameters) {
            if (!isValidLabel(param)) {
                throw SyntaxError("Invalid parameter name '$param'", lineNumber)
            }
        }
        
        return Token.MacroDefinition(macroName, parameters, lineNumber)
    }
}

/**
 * Represents an instruction with optional label reference for second pass
 */
data class LabelInstruction(val instruction: MemoryCell, val label: String = "")

/**
 * Represents a memory segment with an address and instructions
 */
data class MemorySegment(
    val startAddress: Int,
    val instructions: MutableList<LabelInstruction> = mutableListOf()
)

/**
 * Macro preprocessor for expanding macro calls
 */
class MacroPreprocessor {
    private val macros = mutableMapOf<String, MacroDefinition>()
    
    fun preprocess(tokens: List<Token>): List<Token> {
        val processedTokens = mutableListOf<Token>()
        var i = 0
        
        while (i < tokens.size) {
            when (val token = tokens[i]) {
                is Token.MacroDefinition -> {
                    // Collect macro body until .endmacro
                    val macroBody = mutableListOf<String>()
                    i++ // Skip the macro definition token
                    
                    while (i < tokens.size) {
                        val bodyToken = tokens[i]
                        if (bodyToken is Token.MacroEnd) {
                            break
                        }
                        // Convert token back to source line for storage
                        macroBody.add(tokenToSourceLine(bodyToken))
                        i++
                    }
                    
                    if (i >= tokens.size) {
                        throw SyntaxError("Macro '${token.name}' not closed with .endmacro", token.line)
                    }
                    
                    macros[token.name] = MacroDefinition(token.name, token.parameters, macroBody)
                }
                is Token.MacroCall -> {
                    val macro = macros[token.name]
                        ?: throw SyntaxError("Undefined macro '${token.name}'", token.line)
                    
                    if (macro.parameters.size != token.arguments.size) {
                        throw SyntaxError("Macro '${token.name}' expects ${macro.parameters.size} arguments, got ${token.arguments.size}", token.line)
                    }
                    
                    // Expand macro
                    val expandedLines = expandMacro(macro, token.arguments)
                    val lexer = AssemblyLexer()
                    val expandedTokens = lexer.tokenize(expandedLines.joinToString("\n"))
                    processedTokens.addAll(expandedTokens)
                }
                is Token.MacroEnd -> {
                    throw SyntaxError("Unexpected .endmacro without matching .macro", token.line)
                }
                else -> {
                    processedTokens.add(token)
                }
            }
            i++
        }
        
        return processedTokens
    }
    
    private fun expandMacro(macro: MacroDefinition, arguments: List<String>): List<String> {
        val paramMap = macro.parameters.zip(arguments).toMap()
        
        return macro.body.map { line ->
            var expandedLine = line
            for ((param, arg) in paramMap) {
                expandedLine = expandedLine.replace("\\b$param\\b".toRegex(), arg)
            }
            expandedLine
        }
    }
    
    private fun tokenToSourceLine(token: Token): String {
        return when (token) {
            is Token.Label -> "${token.name}:"
            is Token.Instruction -> "${token.opcode.name.lowercase()}${if (token.operand != null) " ${token.operand}" else ""}"
            is Token.Directive -> "${token.name}${if (token.operand != null) " ${token.operand}" else ""}"
            else -> ""
        }
    }
}

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
        return when {
            operand.toIntOrNull() != null -> {
                listOf(LabelInstruction(MemoryCell.Data(operand.toInt())))
            }
            isValidCharacterLiteral(operand) -> {
                val charValue = parseCharacterLiteral(operand)!!
                listOf(LabelInstruction(MemoryCell.Data(charValue)))
            }
            else -> {
                listOf(LabelInstruction(MemoryCell.Data(), operand.lowercase()))
            }
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
        return when {
            operand.toIntOrNull() != null -> {
                listOf(LabelInstruction(MemoryCell.OperandInstruction(Opcode.LIT, operand.toInt())))
            }
            isValidCharacterLiteral(operand) -> {
                val charValue = parseCharacterLiteral(operand)!!
                listOf(LabelInstruction(MemoryCell.OperandInstruction(Opcode.LIT, charValue)))
            }
            else -> {
                listOf(LabelInstruction(MemoryCell.Instruction(Opcode.LIT), operand.lowercase()))
            }
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
        val segments = mutableListOf<MemorySegment>()
        
        var currentSection = Section.TEXT
        var currentAddress = 0
        var currentSegment = MemorySegment(0)
        
        for (token in tokens) {
            when (token) {
                is Token.Directive -> {
                    when (token.name) {
                        ".org" -> {
                            // Save current segment if it has instructions
                            if (currentSegment.instructions.isNotEmpty()) {
                                segments.add(currentSegment)
                            }
                            // Start new segment at specified address
                            currentAddress = token.operand!!.toInt()
                            currentSegment = MemorySegment(currentAddress)
                        }
                        ".data" -> {
                            currentSection = Section.DATA
                        }
                        ".text" -> {
                            currentSection = Section.TEXT
                        }
                    }
                }
                is Token.Label -> {
                    if (labels.containsKey(token.name)) {
                        throw SyntaxError("Duplicate label '${token.name}'", token.line)
                    }
                    labels[token.name] = currentAddress
                }
                is Token.Instruction -> {
                    validateInstructionPlacement(token, currentSection)
                    
                    val generated = instructionGenerator.generateInstruction(token)
                    currentSegment.instructions.addAll(generated)
                    currentAddress += generated.size
                }
                is Token.MacroDefinition, is Token.MacroCall, is Token.MacroEnd -> {
                    throw SyntaxError("Macro tokens should have been preprocessed", 0)
                }
                else -> { /* Skip comments and empty tokens */ }
            }
        }
        
        // Add the final segment
        if (currentSegment.instructions.isNotEmpty()) {
            segments.add(currentSegment)
        }
        
        return ParseResult(labels, segments)
    }
    
    /**
     * Validates that instructions are placed in appropriate sections
     */
    private fun validateInstructionPlacement(instruction: Token.Instruction, currentSection: Section) {
        val config = INSTRUCTION_REGISTRY[instruction.opcode.name.lowercase()]
            ?: throw SyntaxError("Unknown instruction '${instruction.opcode}'", instruction.line)
        
        when (currentSection) {
            Section.TEXT -> {
                // In text section, only allow actual instructions, not data declarations
                if (config.type == InstructionType.DATA_DECLARATION || 
                    instruction.opcode == Opcode.WORD || 
                    instruction.opcode == Opcode.BUF) {
                    throw SyntaxError("Data declaration '${instruction.opcode.name.lowercase()}' not allowed in .text section", instruction.line)
                }
            }
            Section.DATA -> {
                // In data section, only allow data declarations and data-related instructions
                if (config.type != InstructionType.DATA_DECLARATION && 
                    instruction.opcode != Opcode.WORD && 
                    instruction.opcode != Opcode.BUF) {
                    throw SyntaxError("Instruction '${instruction.opcode.name.lowercase()}' not allowed in .data section", instruction.line)
                }
            }
        }
    }
}

/**
 * Result of the first parsing pass
 */
data class ParseResult(
    val labels: Map<String, Int>,
    val segments: List<MemorySegment>
)

/**
 * Code generator for the second pass - resolves labels to addresses
 */
class CodeGenerator {
    fun generate(parseResult: ParseResult): Program {
        // Find the maximum address needed to determine memory size
        var maxAddress = 0
        for (segment in parseResult.segments) {
            val segmentEnd = segment.startAddress + segment.instructions.size
            if (segmentEnd > maxAddress) {
                maxAddress = segmentEnd
            }
        }
        
        // Initialize memory with default data cells
        val memory = Array<MemoryCell>(maxAddress) { MemoryCell.Data(0) }
        
        // Place instructions from all segments
        for (segment in parseResult.segments) {
            var currentAddress = segment.startAddress
            for (labelInstruction in segment.instructions) {
                val resolvedInstruction = if (labelInstruction.label.isEmpty()) {
                    labelInstruction.instruction
                } else {
                    resolveLabelReference(labelInstruction, parseResult.labels)
                }
                memory[currentAddress] = resolvedInstruction
                currentAddress++
            }
        }
        
        val startAddress = parseResult.labels["start"]
            ?: throw SyntaxError("Program must have a 'start:' label", 0)
        
        return Program(startAddress, memory)
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
    private val macroPreprocessor = MacroPreprocessor()
    private val parser = AssemblyParser()
    private val codeGenerator = CodeGenerator()
    
    fun translate(source: String): Program {
        try {
            val tokens = lexer.tokenize(source)
            val preprocessedTokens = macroPreprocessor.preprocess(tokens)
            val parseResult = parser.parse(preprocessedTokens)
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
    val program = translator.translate(source)
    
    // Generate debug output
    val debugOutputFile = filename.replaceAfterLast('.', "debug")
    generateDebugOutput(program, debugOutputFile)
    
    return program
}

/**
 * Generates debug output file with instruction details
 */
private fun generateDebugOutput(program: Program, outputFile: String) {
    val debugOutput = mutableListOf<String>()
    
    for (address in program.program.indices) {
        val instruction = program.program[address]
        val hexCode = formatInstructionAsHex(instruction)
        val mnemonic = formatInstructionAsMnemonic(instruction)
        debugOutput.add("$address - $hexCode - $mnemonic")
    }
    
    File(outputFile).writeText(debugOutput.joinToString("\n"))
}

/**
 * Formats instruction as hexadecimal string
 */
private fun formatInstructionAsHex(instruction: MemoryCell): String {
    return when (instruction) {
        is MemoryCell.Instruction -> {
            val opcodeValue = instruction.opcode.ordinal
            String.format("%08X", opcodeValue)
        }
        is MemoryCell.OperandInstruction -> {
            val opcodeValue = instruction.opcode.ordinal
            val operandValue = instruction.operand
            String.format("%08X %08X", opcodeValue, operandValue)
        }
        is MemoryCell.Data -> {
            String.format("%08X", instruction.value)
        }
    }
}

/**
 * Formats instruction as mnemonic string
 */
private fun formatInstructionAsMnemonic(instruction: MemoryCell): String {
    return when (instruction) {
        is MemoryCell.Instruction -> {
            instruction.opcode.name.lowercase()
        }
        is MemoryCell.OperandInstruction -> {
            val opcodeName = instruction.opcode.name.lowercase()
            "${opcodeName} ${instruction.operand}"
        }
        is MemoryCell.Data -> {
            "data ${instruction.value}"
        }
    }
}