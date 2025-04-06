package minimalcpu

import org.scalatest.funsuite.AnyFunSuite
import spinal.core.sim._
import minimalcpu.plugins.FetchPlugin
import spinal.core.B
import minimalcpu.plugins.RegisterFilePlugin

class MinimalCpuTest extends AnyFunSuite {
  test("sample test") {
    Config.sim.compile(new MinimalCpu(MyCpuConfig())).doSim { dut =>
      // 1. Initialize Instruction ROM
      val testProgram = Array(
        0x15, // LOADI R1, 5  (00010101)
        0x2a, // LOADI R2, 10 (00101010)
        0x76, // ADD R3, R1, R2 (01110110) R3 should be 15
        0xc0 // NOP          (11000000)
        // Add more instructions...
      )
      // Assuming direct access for simulation - real hardware needs loading mechanism
      // This part needs careful implementation based on how Mem is accessed in sim
      val fetchPlugin = dut.service(classOf[FetchPlugin]);
      fetchPlugin.instructionRom.init(testProgram.map(x => B(x)).toSeq)

      // 2. Clock Domain Setup
      dut.clockDomain.forkStimulus(period = 10) // Create clock

      // 3. Reset Sequence
      dut.clockDomain.assertReset()
      sleep(100) // Wait some time
      dut.clockDomain.deassertReset()

      // 4. Run Simulation
      var cycles = 0
      while (cycles < 50) { // Run for enough cycles
        dut.clockDomain.waitSampling()
        cycles += 1
      }

      // 5. Check Results (Needs access to RegisterFilePlugin's regFile)
      // This requires making regFile accessible from simulation, e.g.,
      // by adding simulation-specific accessors or naming it predictably.
      // Let's assume we can access it via a known path:
      val regFileSim = dut.service(classOf[RegisterFilePlugin]).regFile
      val r0 = regFileSim.readSync(0)
      val r1 = regFileSim.readSync(1)
      val r2 = regFileSim.readSync(2)
      val r3 = regFileSim.readSync(3)

      println(s"R0: $r0")
      println(s"R1: $r1")
      println(s"R2: $r2")
      println(s"R3: $r3")

      assert(r0 == 0, "R0 should always be 0")
      assert(r1 == 5, "R1 should be 5")
      assert(r2 == 10, "R2 should be 10")
      assert(r3 == 15, "R3 should be 15 (5+10)") // Check ADD result

      println("Simulation successful!")
    }
  }
}
