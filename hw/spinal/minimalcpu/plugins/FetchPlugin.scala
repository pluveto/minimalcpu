package minimalcpu.plugins

import spinal.core._
import spinal.lib._
import minimalcpu.plugin._
import minimalcpu.Pipeline
import minimalcpu.Stage
import minimalcpu.Stageable
import scala.collection.mutable.ArrayBuffer
import minimalcpu.MinimalCpu

class FetchPlugin() extends Plugin[MinimalCpu] {
  val pc = Reg(UInt(8 bits)).init(0)

  val instructionRom = Mem(
    UInt(8 bits),
    initialContent = Array[UInt](
      0x15, // LOADI R1, 5  (00010101)
      0x2a, // LOADI R2, 10 (00101010)
      0x76, // ADD R3, R1, R2 (01110110) R3 should be 15
      0xc0, // NOP (11000000)
      0xc0, // NOP (11000000)
      0xc0, // NOP (11000000)
      0xc0, // NOP (11000000)
      0xc0 // NOP (11000000)
    )
  )

  override def setup(pipeline: MinimalCpu): Unit = {}

  override def build(pipeline: MinimalCpu): Unit = {
    import pipeline._
    import pipeline.config._

    fetch plug new Area {
      import fetch._

      val pcAtEnd = pc >= instructionRom.wordCount

      val instruction = pcAtEnd ? U(0xc0) | instructionRom.readAsync(pc(2 downto 0))
      insert(INSTRUCTION) := instruction.asBits

      when(!pcAtEnd) {
        pc := pc + 1
      }

      when(pcAtEnd) {
        fetch.arbitration.haltItself := True
      }

      report(L"[Fetch] pc=${pc}, instruction=${instruction}")
    }
  }
}
