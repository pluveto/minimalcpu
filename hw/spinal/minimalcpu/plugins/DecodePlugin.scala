package minimalcpu.plugins

import spinal.core._
import spinal.lib._
import minimalcpu._
import minimalcpu.plugin._
import minimalcpu.Pipeline
import minimalcpu.Stage
import minimalcpu.Stageable
import scala.collection.mutable.ArrayBuffer

// 译码插件
class DecodePlugin extends Plugin[MinimalCpu] {
  override def build(pipeline: MinimalCpu): Unit = {
    import pipeline._
    import pipeline.config._

    new implicitsStage(decode).plug(new Area {
      import decode._ // 导入 decode stage 的上下文

      // 从上一阶段获取指令
      val instruction = input(INSTRUCTION)

      // 译码逻辑
      val opcode = instruction(7 downto 6)
      val rd = instruction(5 downto 4).asUInt
      val rs1 = instruction(3 downto 2).asUInt // 对 LOADI 无效
      val rs2 = instruction(1 downto 0).asUInt // 对 LOADI 无效
      val imm = instruction(3 downto 0)

      // 将译码结果放入 Stageables，传递给下一阶段 (Execute)
      output(OPCODE) := opcode
      output(RD) := rd
      output(RS1) := rs1
      output(RS2) := rs2
      output(IMM) := imm.resize(8) // 符号扩展或零扩展 (简单起见零扩展)
    })
  }
}
