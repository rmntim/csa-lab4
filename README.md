# Computer Systems and Architecture Labwork №4

> `asm | stack | neum | mc | tick | binary | stream | port | pstr | prob1 | -`

Task details: https://gitlab.se.ifmo.ru/computer-systems/csa-rolling/-/blob/07ffa8338bc3f2584260beb3d0be6c05ba2955d6/lab4-task.md

---

## Содержание

* ISA & Assembly language translator
* Microcoded CPU with Data & Return stacks [(full schema)](/docs/schema.pdf)
* Execution logs contains CPU state after every
  microinstruction + Memory Dump after HALT execution
* Written in pure Kotlin/JVM.

## Table of Contents

1. [Assembly Language](#assembly-language)
2. [ISA](#isa)
3. [Assembly Translator](#assembly-translator)
4. [Computer Simulation](#computer-simulation)
5. [Tests](#tests)

## Assembly language

Syntax:

```ebnf
<line> ::= <label> <comment>? "\n"
       | <instr> <comment>? "\n"
       | <comment> "\n"

<program> ::= <line>*

<label> ::= <label_name> ":"

<instr> ::= <op0>
        | <op1> " " <label_name>
        | <op1> " " <positive_integer>

<op0> ::= "nop"
      | "load"
      | "store"
      | "add"
      | "sub"
      | "mul"
      | "div"
      | "inc"
      | "dec"
      | "drop"
      | "dup"
      | "swap"
      | "over"
      | "and"
      | "or"
      | "xor"
      | "jz"
      | "jn"
      | "jmp"
      | "call"
      | "ret"
      | "in"
      | "out"
      | "halt"

<op1> ::= "lit"
      | "word"
      | "buf"

<positive_integer> ::= [0-9]+
<integer> ::= "-"? <positive_integer>

<lowercase_letter> ::= [a-z]
<uppercase_letter> ::= [A-Z]
<letter> ::= <lowercase_letter> | <uppercase_letter>

<letter_or_number> ::= <letter> | <integer>
<letter_or_number_with_underscore> ::= <letter_or_number> | "_"

<label_name> ::= <letter> <letter_or_number_with_underscore>*

<any_letter> ::= <letter_or_number_with_underscore> | " "

<comment> ::= " "* ";" " "* <letter_or_number_with_underscore>*
```

The Program completes sequentially, one instruction after another.
Example of a program that calculates a factorial:

```asm
res:
        word 0      ; result accumulator
fac:
        dup         ; Stack: arg arg
        lit 1       ; Stack: arg arg 1
        sub         ; Stack: arg 0/pos_num
        lit break   ; Stack: arg 0/pos_num break
        swap        ; Stack: arg break 0/pos_num
        jz          ; Stack: arg
        dup         ; Stack: arg arg
        dec         ; Stack: arg (arg - 1) -> arg
        lit fac     ; Stack: [...] arg fac
        call        ; Stack: [...] res
        mul         ; Stack: res
break:
        ret         ; Stack: arg/res

start:
        lit 11      ; Stack: 11
        lit fac     ; Stack: 11 fac
        call        ; Stack: 11!
        lit res     ; Stack: 11! res_addr
        store       ; Stack: <empty>
        halt        ; halted
```

## ISA

Assembly-only instructions:

* `WORD <literal>` – define a variable in memory.
* `BUF <amount>` – define a zero-buffer in memory.

Computer/Assembly instructions:

* `NOP` – no operation.
* `LIT <literal>` – push literal on top of the stack.
* `LOAD { address }` – load value in memory by address.
* `STORE { address, element }` – push value in memory by address.
* `ADD { e1, e2 }` – push the result of the addition operation
  onto the stack e2 + e1.
* `SUB { e1, e2 }` – push the result of the subtraction operation
  onto the stack e2 – e1.
* `MUL { e1, e2 }` – push the result of the multiplication operation
  onto the stack e2 * e1.
* `DIV { e1, e2 }` – push the result of the division operation
  onto the stack e2 / e1.
* `MOD { e1, e2 }` – push the result of the mod operation
  onto the stack e2 % e1.
* `INC { element }` – increment top of the stack.
* `DEC { element }` – decrement top of the stack.
* `DROP { element }` – remove element from stack.
* `DUP { element }` – duplicate the first element (tos) on stack.
* `SWAP { e1, e2 }` – swap 2 elements.
* `OVER { e1 } [ e2 ]` – duplicate the first element
  on the stack through the second.
  If there is only one element on the stack, the behavior is undefined.
* `AND { e1, e2 }` – push the result of a logical "AND" operation
  onto the stack e2 & e1.
* `OR { e1, e2 }` – push the result of a logical "OR" operation
  onto the stack e2 | e1.
* `XOR { e1, e2 }` – push the result of a logical "XOR" operation
  onto the stack e2 ^ e1.
* `JZ { element, address }` – if the element is 0, start executing instructions
  at the specified address.
  A type of conditional jump.
* `JN { element, address }` – if the element is negative, start executing
  instructions at the specified address.
  A type of conditional jump.
* `JUMP { address }` – proceed an unconditional transition
  to the specified address.
* `CALL { address }` – start execution of the procedure
  by the specified address.
* `RET` – return from a procedure.
* `IN { port }` – receive data from an external device by a specified port.
* `OUT { port, value }` – receive data to an external device
  by a specified port.
* `HALT` – stop clock generator and modeling process.

## Assembly translator

CLI: `java -jar asm-1.0.jar <input_file> <target_file>`

Implemented in [asm](/asm) module.  
Two passes:

1) Generation of machine code without jump addresses
   and calculation of jump label values.
   Assembly mnemonics are translated one-to-one
   into machine instructions; except for the WORD mnemonics.
   In its case, a variable is initialized in memory without any opcode.
   However, WORD, along with instructions, also supports labels.
2) Substitution of transition marks in instructions.

## Computer simulation

CLI:

```bash
java -jar comp-1.0.jar [-h | --help]
```

Implemented in [comp](/comp) module.

Processor schema's available [here](/docs/schema.pdf)

## Tests

To run golden tests do:

```shell
cd python
poetry run pytest . -v
```