package minimalcpu

import org.scalatest.funsuite.AnyFunSuite
import spinal.core.sim._
import minimalcpu.plugins.FetchPlugin
import spinal.core.B
import minimalcpu.plugins.RegFilePlugin
import spinal.core.BitCount

class MinimalCpuTest extends AnyFunSuite {
  test("sample test") {

    val compiled = SimConfig.withWave.compile {
      val dut = new MinimalCpu(
        MyCpuConfig()
      )
      dut.service(classOf[RegFilePlugin]).regFile.simPublic()
      dut
    }

    compiled.doSim { dut =>
      // 1. Initialize Instruction ROM
      // 2. Clock Domain Setup
      dut.clockDomain.forkStimulus(period = 10) // Create clock

      // 3. Reset Sequence
      dut.clockDomain.assertReset()
      sleep(100) // Wait some time
      dut.clockDomain.deassertReset()

      // 4. Run Simulation
      var cycles = 0
      while (cycles < 8 + 3) { // Run for enough cycles
        dut.clockDomain.waitSampling()
        cycles += 1
      }

      // 5. Check Results
      // Assuming you have simulation access to the register file:
      val regFileSim = dut.service(classOf[RegFilePlugin]).regFile
      val r0 = regFileSim(0).toInt
      val r1 = regFileSim(1).toInt
      val r2 = regFileSim(2).toInt
      val r3 = regFileSim(3).toInt

      println(s"R0: $r0, R1: $r1, R2: $r2, R3: $r3")

      assert(r0 == 0, "R0 should always be 0")
      assert(r1 == 5, "R1 should be 5")
      assert(r2 == 10, "R2 should be 10")
      assert(r3 == 15, "R3 should be 15 (5+10)")

      println("Simulation successful!")
    }
  }
}
