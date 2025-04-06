package minimalcpu.plugins

import spinal.core._
import spinal.lib._
import minimalcpu.plugin._
import minimalcpu.Pipeline
import minimalcpu.Stage
import minimalcpu.Stageable
import scala.collection.mutable.ArrayBuffer
import minimalcpu.MinimalCpu

// 执行插件
class ExecutePlugin extends Plugin[MinimalCpu] {
  override def build(pipeline: MinimalCpu): Unit = {
    import pipeline._
    import pipeline.config._

    execute plug new Area {
      import execute._

      // 从 Decode 阶段获取数据
      val opcode = input(OPCODE)
      val imm = input(IMM)
      val rs1Data = input(RS1_DATA)
      val rs2Data = input(RS2_DATA)

      // ALU 逻辑
      var result = S(0, 8 bits)
      var writeReg = False // 默认不写回

      switch(opcode) {
        is(B"00") { // LOADI
          result := imm.asSInt
          writeReg := True
        }
        is(B"01") { // ADD
          result := rs1Data + rs2Data
          writeReg := True
        }
        is(B"10") { // SUB
          result := rs1Data - rs2Data
          writeReg := True
        }
        is(B"11") { // NOP
          // Do nothing
          writeReg := False
        }
      }

      // 将计算结果和写回信号放入 Stageables
      // ALU_RESULT 会被 RegFilePlugin 在同一阶段消耗用于写回
      output(ALU_RESULT) := result
      output(WRITE_REG) := writeReg

      // RD 信号也需要传递给 RegFilePlugin 的写端口逻辑
      // (RD 是从 Decode 阶段传递过来的 input(RD))
    }
  }
}
