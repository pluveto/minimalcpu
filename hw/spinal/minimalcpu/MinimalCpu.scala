package minimalcpu

import spinal.core._
import spinal.lib._
import minimalcpu.plugin._
import scala.collection.mutable.ArrayBuffer
import minimalcpu.plugins._

case class MinimalCpu(val config: MyCpuConfig) extends Component with Pipeline {
  type T = MinimalCpu;
  import config._

  def newStage(): Stage = { val s = new Stage; stages += s; s }
  val fetch = newStage()
  val decex = newStage()
  val wb = newStage()

  plugins ++= Seq(
    new FetchPlugin(),
    new DecExPlugin(),
    new RegFilePlugin(registerCount = 4)
  )
}

case class MyCpuConfig() {

  object INSTRUCTION extends Stageable(Bits(8 bits))
  object OPCODE extends Stageable(Bits(2 bits))
  object RD extends Stageable(UInt(2 bits))
  object RS1 extends Stageable(UInt(2 bits))
  object RS2 extends Stageable(UInt(2 bits))
  object IMM extends Stageable(UInt(4 bits))
  object RS1_DATA extends Stageable(SInt(8 bits))
  object RS2_DATA extends Stageable(SInt(8 bits))
  object ALU_RESULT extends Stageable(SInt(8 bits))
  object WRITE_REG extends Stageable(Bool())
}

object MinimalCpuVerilog extends App {
  Config.spinal.generateVerilog(new MinimalCpu(MyCpuConfig()))
}
