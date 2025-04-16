package minimalcpu.plugins

import spinal.core._
import spinal.lib._
import minimalcpu.plugin._
import minimalcpu.Pipeline
import minimalcpu.Stage
import minimalcpu.Stageable
import scala.collection.mutable.ArrayBuffer
import minimalcpu.MinimalCpu
import minimalcpu.MyCpuConfig // Ensure needed Stageables are here

class DecExPlugin extends Plugin[MinimalCpu] {
  override def build(pipeline: MinimalCpu): Unit = {
    import pipeline._
    import pipeline.config._

    decex plug new Area {
      import decex._

      val instruction = input(INSTRUCTION)

      // --- Instruction Fields ---
      val opcode = instruction(7 downto 6)
      val rd_raw = instruction(5 downto 4).asUInt
      // Raw fields potentially used differently based on opcode
      val field32_raw = instruction(3 downto 2).asUInt
      val field10_raw = instruction(1 downto 0).asUInt
      val imm4_raw = instruction(3 downto 0).asUInt

      // --- Control Signals ---
      val isLoadI = opcode === B"00"
      val isAdd = opcode === B"01"
      val isSub = opcode === B"10"
      val isNop = opcode === B"11" // Or any other invalid/unrecognized opcode

      // --- Decode based on Opcode ---
      // Default to 0 or safe values
      // Declare outputs as val
      val decoded_rs1 = UInt(2 bits)
      val decoded_rs2 = UInt(2 bits)
      val decoded_rd = UInt(2 bits)
      val decoded_imm = UInt(4 bits)
      val writeReg = Bool()

      // --- Assign default values BEFORE the switch ---
      // These defaults will be used if a condition isn't met,
      // preventing latches. Choose safe/NOP defaults.

      switch(opcode) {
        is(B"00") { // LOADI Rd, Imm
          decoded_rd := rd_raw
          decoded_imm := imm4_raw
          decoded_rs1 := U(0) // Not used
          decoded_rs2 := U(0) // Not used
          writeReg := True
        }
        is(B"01") { // ADD Rd, Rs1, Rs2
          decoded_rd := rd_raw
          decoded_rs1 := field32_raw // Use bits 3-2 as RS1
          decoded_rs2 := field10_raw // Use bits 1-0 as RS2
          decoded_imm := U(0) // Not used
          writeReg := True
        }
        is(B"10") { // SUB Rd, Rs1, Rs2
          decoded_rd := rd_raw
          decoded_rs1 := field32_raw // Use bits 3-2 as RS1
          decoded_rs2 := field10_raw // Use bits 1-0 as RS2
          decoded_imm := U(0) // Not used
          writeReg := True
        }
        is(B"11") { // NOP or others
          // Keep defaults (rd=0, rs1=0, rs2=0, imm=0, writeReg=False)
          decoded_rd := U(0)
          decoded_rs1 := U(0)
          decoded_rs2 := U(0)
          decoded_imm := U(0)
          writeReg := False
        }
      }

      insert(RS1) := decoded_rs1
      insert(RS2) := decoded_rs2
      insert(RD) := decoded_rd
      insert(WRITE_REG) := writeReg // Insert the determined write enable

      report(
        L"[Dec] isNop: ${isNop}, opcode: ${opcode}, rd: ${decoded_rd}, rs1: ${decoded_rs1}, rs2: ${decoded_rs2}, imm: ${decoded_imm}, writeReg: ${writeReg}"
      )

      // --- Execute ---
      val rs1Data = input(RS1_DATA)
      val rs2Data = input(RS2_DATA)

      // Use SInt for arithmetic if registers hold signed values
      var result = S(0, 8 bits)

      // Calculate result based on opcode
      switch(opcode) {
        is(B"00") { result := decoded_imm.resize(8 bits).asSInt } // LOADI
        is(B"01") { result := rs1Data + rs2Data } // ADD
        is(B"10") { result := rs1Data - rs2Data } // SUB
        is(B"11") { /* NOP - result remains 0 */ }
      }

      insert(ALU_RESULT) := result

      report(
        L"[Exe] result: ${result}, opcode: ${opcode}, imm(decoded): ${decoded_imm}, rs1Data: ${rs1Data}, rs2Data: ${rs2Data}, writeReg(from decode): ${writeReg}"
      )
    }
  }
}
