package minimalcpu.plugins

import spinal.core._
import spinal.lib._
import minimalcpu.plugin._
import minimalcpu.Pipeline
import minimalcpu.Stage
import minimalcpu.Stageable
import scala.collection.mutable.ArrayBuffer
import minimalcpu.MinimalCpu

// --- Plugin 实现示例 ---

// 取指插件
class FetchPlugin(initialRom: Seq[Bits] = Nil) extends Plugin[MinimalCpu] {
  val pc = Reg(UInt(8 bits)).init(0)

  val instructionRom = Mem(Bits(8 bits), wordCount = 8)

  override def setup(pipeline: MinimalCpu): Unit = {
    if (initialRom.nonEmpty) {
      // instructionRom.init(initialRom)
    }
  }

  override def build(pipeline: MinimalCpu): Unit = {
    import pipeline._
    import pipeline.config._

    fetch plug new Area { // 在 fetch 阶段插入逻辑
      import fetch._ // 导入 fetch stage 的上下文

      // 从 ROM 读取指令
      val instruction = instructionRom.readSync(pc)
      // 将指令放入 Stageable，传递给下一阶段 (Decode)
      insert(INSTRUCTION) := instruction

      // 更新 PC (简单自增，无跳转)
      pc := pc + 1

      // 默认情况下，指令有效，不冲刷
      fetch.arbitration.isValid := True
    }
  }
}
