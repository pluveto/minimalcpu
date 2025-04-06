package minimalcpu.plugins

import spinal.core._
import spinal.lib._
import minimalcpu.MinimalCpu
import minimalcpu.plugin._
import minimalcpu.Pipeline
import minimalcpu.Stage
import minimalcpu._
import minimalcpu.Stageable
import scala.collection.mutable.ArrayBuffer
import minimalcpu.MyCpuConfig

// 寄存器文件插件
class RegisterFilePlugin(registerCount: Int) extends Plugin[MinimalCpu] {
  var regFile: Mem[Bits] = null

  override def setup(pipeline: MinimalCpu): Unit = {
    // 提供读写服务 (实际 VexRiscv 中更复杂)
  }

  override def build(pipeline: MinimalCpu): Unit = {
    import pipeline._
    import pipeline.config._

    // 创建寄存器文件物理存储
    regFile = Mem(Bits(8 bits), wordCount = registerCount)

    // --- 读端口 (在 Decode 阶段) ---
    decode plug new Area {
      import decode._

      val rs1Addr = input(RS1)
      val rs2Addr = input(RS2)

      // 读取寄存器 (R0 硬连线为 0)
      // 注意：VexRiscv 有专门的 RegFile API 处理读写端口和旁路，这里简化
      val rs1Data = (rs1Addr === 0) ? S(0, 8 bits) | regFile.readAsync(rs1Addr).asSInt
      val rs2Data = (rs2Addr === 0) ? S(0, 8 bits) | regFile.readAsync(rs2Addr).asSInt

      // 将读取的数据放入 Stageables，传递给 Execute 阶段
      insert(RS1_DATA) := rs1Data
      insert(RS2_DATA) := rs2Data
    }

    // --- 写端口 (在 Execute 阶段) ---
    execute plug new Area {
      import execute._

      val writeEn = input(WRITE_REG)
      val writeAddr = input(RD)
      val writeData = input(ALU_RESULT) // 或其他来源

      // 同步写回寄存器文件 (当指令在 Execute 阶段完成时)
      // R0 不能被写入
      when(writeEn && writeAddr =/= 0) {
        regFile.write(writeAddr, writeData.asBits)
      }
    }
  }
}
