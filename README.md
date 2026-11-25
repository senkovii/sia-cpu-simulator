# SIA CPU Simulator

A bit-level CPU simulator written in Java that implements the **Simple Instruction Architecture (SIA)**, a compact RISC-style instruction set with about 20 instructions. The project was built incrementally for ICSI 404 (Computer Architecture) at the University at Albany, starting from a single `Bit` class and finishing with a working processor, an assembler, and a two-level cache hierarchy.

Every arithmetic and logical operation in the simulator is performed "in bits." The adder, multiplier, shifter, and ALU never convert operands to Java `int` or `float` values, so the simulator behaves much more like real hardware than a typical interpreter would.

## Highlights

- **Built from the ground up:** bits, then words, then arithmetic units, then the ALU and memory, then an assembler, a processor, and finally caching.
- **Pure bit-level arithmetic:** ripple-carry addition, two's complement subtraction, shift-and-add multiplication, and barrel-style shifting implemented on custom `Bit` and `Word32` types.
- **Two-pass toolchain:** assembly source is translated into 16-bit machine instructions, which are packed two per 32-bit memory word and loaded into simulated DRAM.
- **Classic processor pipeline stages:** `fetch()`, `decode()`, `execute()`, and `store()` run in a loop until a `halt` instruction is reached.
- **Two-level cache hierarchy:** an L1 instruction cache and a shared L2 cache with cycle-cost accounting, used to compare the performance of different memory access patterns.
- **Tested with JUnit**, including cycle-cost tests for the cache hierarchy and benchmark programs that compare memory access patterns.

## The SIA Architecture

| Feature | Details |
| --- | --- |
| Registers | 32 general-purpose registers (`r0` to `r31`), 32 bits each, signed. `r0` is not hardwired to zero. |
| Hidden registers | A call stack (used by `call` and `return`) and a status register (set by `compare`, read by branches). |
| Instruction width | 16 bits, packed two per 32-bit memory word. |
| Memory | 1000 addressable 32-bit words, addressed by word rather than by byte. |
| Branch addressing | Branch and call offsets are relative to the memory word address, not the instruction index. The target is computed as `(PC / 2 + immediate - 1) * 2`. |

### Instruction Formats

**2R** (register to register)

| 5 bits | 1 bit | 5 bits | 5 bits |
| --- | --- | --- | --- |
| Opcode | Format (0) | Source register | Destination register |

**Immediate**

| 5 bits | 1 bit | 5 bits | 5 bits |
| --- | --- | --- | --- |
| Opcode | Format (1) | Signed immediate | Destination register |

**Call/Return**

| 5 bits | 11 bits |
| --- | --- |
| Opcode | Signed immediate |

### Instruction Set

| Opcode | Mnemonic | Description |
| --- | --- | --- |
| 0 | `halt` | Stops the processor |
| 1 | `add` | destination = destination + source |
| 2 | `and` | Bitwise AND |
| 3 | `multiply` | destination = destination * source |
| 4 | `leftshift` | Shift destination left by source bits |
| 5 | `subtract` | destination = destination - source |
| 6 | `or` | Bitwise OR |
| 7 | `rightshift` | Shift destination right by source bits |
| 8 | `syscall` | Kernel call (`0` prints registers, `1` prints memory) |
| 9 | `call` | Push return address and jump relative to the PC |
| 10 | `return` | Pop the call stack into the PC |
| 11 | `compare` | Compare source to destination and set status flags |
| 12 to 17 | `ble`, `blt`, `bge`, `bgt`, `beq`, `bne` | Conditional relative branches based on status flags |
| 18 | `load` | Load from memory into a register (2R or base + immediate) |
| 19 | `store` | Store a register value into memory |
| 20 | `copy` | Copy source into destination |

## Project Structure

| Layer | Classes | Role |
| --- | --- | --- |
| Data types | `Bit`, `Word16`, `Word32` | Boolean-backed bit storage and the logic operations (AND, OR, XOR, NOT) that everything else is built on |
| Arithmetic | `Adder`, `Multiplier`, `Shifter` | Bit-level addition, subtraction, multiplication, and shifting |
| Execution | `ALU` | Dispatches on the current opcode, produces a result, and sets the less/equal flags used by `compare` |
| Storage | `Memory` | Word-addressed simulated DRAM with `read()`, `write()`, and `load()` for program images |
| Toolchain | `Assembler` | Converts assembly text into 16-bit instructions and merges them into 32-bit memory lines |
| Control | `Processor` | Runs the fetch, decode, execute, and store loop, manages the PC, registers, call stack, and status flags, and tracks the total clock cycle count |
| Caching | `InstructionCache`, `L2Cache`, `CacheLine` | L1 instruction cache and shared L2 cache placed between the processor and memory; `CacheLine` holds a valid bit, a tag (the line's base address), and 8 data words |
| Testing utilities | `TestConverter` | Converts between `Word32` and `int` for tests and debug output only, never for CPU functionality |

## Memory Hierarchy

The final stage of the project places a two-level cache between the processor and main memory, with a cycle cost attached to every access so that programs can be compared by performance and not only by correctness.

```
Processor ──fetch──> L1 InstructionCache ──miss──> L2Cache ──miss──> Memory
          ──load/store──────────────────────────> L2Cache ──────────> Memory
```

| Component | Configuration |
| --- | --- |
| L1 instruction cache | Single line of 8 words. On a miss, the entire line is filled from L2 in one `fillLine()` request rather than 8 separate reads. |
| L2 cache | 4 lines of 8 words each with round-robin replacement, shared by instruction fetch and data loads/stores. Stores use a write-through, no-write-allocate policy: a write hit updates the cached copy and memory, and a write miss goes straight to memory without filling a line. |

Each instruction fetch reads the 32-bit word at address `PC / 2` through the L1 cache, then takes the top or bottom half depending on whether the PC is even or odd. This keeps the two-instructions-per-word packing transparent to the rest of the pipeline.

| Event | Cycle cost |
| --- | --- |
| L1 instruction hit | 10 |
| L1 miss, line found in L2 | 20 |
| L1 miss, L2 miss (line filled from memory) | 370 |
| L2 data read or write hit | 20 |
| L2 data read or write miss | 360 |
| ALU operation (add, subtract, logic, shift, compare) | 2 |
| Multiply | 10 |

The running total is exposed as the public `clockCycle` field on `Processor` and is printed when the program halts.

### Benchmarks

`PerformanceTest` runs three programs that do similar arithmetic work but access memory in very different ways, then prints their cycle counts side by side:

| Program | Memory behavior |
| --- | --- |
| Loop sum (1 to 100) | Register-only loop body; after the first iteration, every fetch is an L1 hit |
| Array sum | Writes 100 values to addresses 200 to 299, then loads them back sequentially, so each L2 line fill serves 8 consecutive loads |
| Linked list sum | Builds 100 two-word nodes (value, next pointer) at addresses 400 to 599, then follows the pointers, mixing pointer and value loads |

`testCompareAll` reports each program's cycle count and its ratio to the register-only loop, which shows how much of a program's cost comes from memory access rather than computation.

## Example Program

The following SIA program computes the first 17 Fibonacci numbers, stores them in memory starting at address 20, and then prints memory with a system call.

```asm
copy 10 r9          ; base address
leftshift 1 r9      ; r9 = 20
copy 15 r0          ; loop counter
store 0 r9          ; f(0)
add 1 r9
store 1 r9          ; f(1)
add 1 r9
copy r9 r2
copy r9 r3
load -1 r2          ; f(n-1)
load -2 r3          ; f(n-2)
add r2 r3           ; f(n)
store r3 r9
subtract 1 r0
compare 0 r0
bne -4              ; loop until the counter reaches 0
syscall 1           ; print memory
halt
```

Running a program goes through the full toolchain:

```java
String[] assembled = Assembler.assemble(program);
String[] image = Assembler.finalOutput(assembled);   // two instructions per 32-bit line

Memory memory = new Memory();
memory.load(image);

Processor cpu = new Processor(memory);
cpu.run();                                           // runs until halt
System.out.println(cpu.clockCycle);                  // total cycles spent
```

## Building and Testing

Requirements: Java 17 or later and JUnit 5.

The simplest way to run the project is to open it in IntelliJ IDEA (or another Java IDE), add JUnit 5 to the classpath, and run the test classes. To run from the command line with the JUnit console launcher:

```bash
javac -d out -cp junit-platform-console-standalone.jar *.java
java -jar junit-platform-console-standalone.jar -cp out --scan-classpath
```

| Test class | Covers |
| --- | --- |
| `LogicTest` | Bit truth tables, Word16/Word32 logic operations, bit access, half extraction for instruction fetch, and copy semantics (no aliasing) |
| `ArithmeticTest` | Bit-level addition, subtraction, multiplication, and shifting, including negative values, overflow, and 5-bit shift masking |
| `ALUOperationsTest` | Opcode dispatch for every ALU operation and the less/equal flags set by `compare` |
| `MemorySystemTest` | Reads, writes, program loading, input validation, and value copying in simulated DRAM |
| `AssemblerEncodingTest` | Encoding of 2R, immediate, and call/return formats, negative immediates, and packing into 32-bit lines |
| `ProgramExecutionTest` | Complete programs (loops, calls, conditional branches, loads and stores) plus exact cycle counts for cache hits and misses |
| `CacheTest` | Cycle accounting for ALU and multiply instructions and cold-start misses through both cache levels |
| `PerformanceTest` | Cycle-count benchmarks for the loop, array, and linked list programs, plus a side-by-side comparison (`testCompareAll`) |

For example, `ProgramExecutionTest` checks that a five-instruction program costs exactly 418 cycles: one cold miss through both caches (370), four L1 hits (40), and four ALU operations (8).

## Design Constraints

The course placed deliberate restrictions on the implementation to keep the simulator faithful to hardware:

- No Java arithmetic or logical operators for CPU functionality; all math happens on `Bit` objects.
- `Bit` logic is implemented with conditionals, not `&`, `|`, `&&`, or `||`.
- Values move between words with `assign()` and `copy()` rather than Java reference assignment, which avoids aliasing between registers, memory, and cache lines.
- `TestConverter` exists only for tests and debug printing.

## What I Learned

- How a processor's fetch logic must account for instruction packing, and why branch targets have to be computed relative to memory word addresses once two instructions share each 32-bit word.
- How sign extension, two's complement, and fixed-width immediates shape what an instruction set can express.
- Why reference aliasing is so dangerous in a hardware simulator, and how to trace corrupted values between known-good checkpoints when debugging.
- How cache size, line size, and replacement policy change the real cost of the same program depending on its memory access pattern.

## Acknowledgments

- Course: ICSI 404 Computer Architecture, University at Albany, SUNY
- Reference text: Patterson and Hennessy, *Computer Organization and Design: The Hardware/Software Interface*