package minimalcpu

import spinal.core._
import spinal.lib._
import minimalcpu.plugin._
import minimalcpu.Pipeline
import minimalcpu.Stage
import minimalcpu.Stageable
import scala.collection.mutable.ArrayBuffer
import minimalcpu.plugins._

// 定义 CPU 组件
class MinimalCpu(val config: MyCpuConfig) extends Component with Pipeline {
  type T = MinimalCpu;
  import config._

  // 2. 定义 Stages
  def newStage(): Stage = { val s = new Stage; stages += s; s }
  val fetch = newStage()
  val decode = newStage()
  val execute = newStage()
  // Stage 之间的连接由框架处理

  // 4. 实例化 Plugins
  val _plugins = ArrayBuffer[Plugin[MinimalCpu]](
    // 负责 PC 和取指
    new FetchPlugin(config.initialRom),
    // 负责指令译码
    new DecodePlugin(),
    // 负责寄存器文件读写
    new RegisterFilePlugin(registerCount = 4),
    // 负责 ALU 计算和写回决策
    new ExecutePlugin()
    // 注意：实际 VexRiscv 需要 PCManagerPlugin, HazardPlugins 等，这里极简化
  )

  plugins ++= _plugins.iterator

  // 5. 构建 Pipeline (将 Plugins 连接到 Pipeline)
//   build()
}

// --- 配置 (示例) ---
case class MyCpuConfig(
    initialRom: Seq[Bits] = Nil
) {

  // 由 FetchPlugin 产生 -> DecodePlugin 消耗
  object INSTRUCTION extends Stageable(Bits(8 bits))
  // 由 DecodePlugin 产生 -> ExecutePlugin 消耗
  object OPCODE extends Stageable(Bits(2 bits))
  object RD extends Stageable(UInt(2 bits)) // 目标寄存器地址
  object RS1 extends Stageable(UInt(2 bits)) // 源寄存器1地址
  object RS2 extends Stageable(UInt(2 bits)) // 源寄存器2地址
  object IMM extends Stageable(Bits(4 bits)) // 立即数
  // 由 RegFilePlugin 产生 (在 Decode 阶段读取) -> ExecutePlugin 消耗
  object RS1_DATA extends Stageable(SInt(8 bits))
  object RS2_DATA extends Stageable(SInt(8 bits))
  // 由 ExecutePlugin 产生 -> RegFilePlugin 消耗 (在 Execute 阶段写入)
  object ALU_RESULT extends Stageable(SInt(8 bits))
  object WRITE_REG extends Stageable(Bool()) // 是否写回寄存器
}

class MinimalCpuVerilog extends App {
  Config.spinal.generateVerilog(new MinimalCpu(MyCpuConfig()))
}
