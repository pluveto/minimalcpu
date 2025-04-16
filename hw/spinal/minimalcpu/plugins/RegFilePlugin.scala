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

class RegFilePlugin(registerCount: Int) extends Plugin[MinimalCpu] {
  var regFile: Vec[SInt] = null

  override def setup(pipeline: MinimalCpu): Unit = {}

  override def build(pipeline: MinimalCpu): Unit = {
    import pipeline._
    import pipeline.config._

    regFile = Vec(Reg(SInt(8 bits)) init (0), registerCount)

    // --- Decode/Execute Stage Logic (with Forwarding) ---
    decex plug new Area {
      import decex._

      val rs1Addr = input(RS1)
      val rs2Addr = input(RS2)

      // --- Forwarding Logic ---
      val wbWillWrite = wb.input(WRITE_REG) // From WB stage's input register
      val wbRd = wb.input(RD) // From WB stage's input register
      val wbResult = wb.input(ALU_RESULT) // From WB stage's input register
      val wbIsValid = wb.arbitration.isValid // WB stage input register validity

      // IMPORTANT: Check if WB *stage itself* is valid AND *will perform* a meaningful write
      val wbIsValidAndWillWrite = wbIsValid && wbWillWrite && (wbRd =/= 0)

      val rs1RegFileData = regFile(rs1Addr)
      val rs2RegFileData = regFile(rs2Addr)

      val forwardFromWbToRs1 = wbIsValidAndWillWrite && (wbRd === rs1Addr)
      val forwardFromWbToRs2 = wbIsValidAndWillWrite && (wbRd === rs2Addr)

      val finalRs1Data = Mux(
        rs1Addr === 0,
        S(0, 8 bits),
        Mux(forwardFromWbToRs1, wbResult, rs1RegFileData)
      )

      val finalRs2Data = Mux(
        rs2Addr === 0,
        S(0, 8 bits),
        Mux(forwardFromWbToRs2, wbResult, rs2RegFileData)
      )

      insert(RS1_DATA) := finalRs1Data
      insert(RS2_DATA) := finalRs2Data

      // --- Reporting ---
      when(decex.arbitration.isValid) {
        // Optional: Report forwarding source validity
        // report(L"[RegFwd] WB state for forwarding check: isValid=${wbIsValid}, willWrite=${wbWillWrite}, wbRd=${wbRd}")

        when(rs1Addr =/= 0) {
          when(forwardFromWbToRs1) {
            report(L"[RegFwd] Forwarding WB->DecEx for RS1(${rs1Addr}). Value: ${wbResult}")
          } otherwise {
            // report(L"[Reg] Reading RS1(${rs1Addr}) from Register File. Value: ${rs1RegFileData}")
          }
        }
        when(rs2Addr =/= 0) {
          when(forwardFromWbToRs2) {
            report(L"[RegFwd] Forwarding WB->DecEx for RS2(${rs2Addr}). Value: ${wbResult}")
          } otherwise {
            // report(L"[Reg] Reading RS2(${rs2Addr}) from Register File. Value: ${rs2RegFileData}")
          }
        }
        report(L"[Reg] Providing RS1_DATA=${finalRs1Data}, RS2_DATA=${finalRs2Data} to ALU")
      }
    }

    // --- Writeback Stage Logic ---
    wb plug new Area {
      import wb._

      // Read inputs coming into the WB stage *unconditionally* for debugging
      val rawWriteEn = input(WRITE_REG)
      val rawWriteAddr = input(RD)
      val rawWriteData = input(ALU_RESULT)
      val wbStageIsValid = wb.arbitration.isValid // Is the data currently in WB valid?

      // Report status *before* the conditional write
      report(
        L"[RegWB-Debug] WB Stage Status: isValid=${wbStageIsValid}, rawWriteEn=${rawWriteEn}, rawWriteAddr=${rawWriteAddr}, rawWriteData=${rawWriteData}"
      )

      // Perform the write *only if* the stage is valid and it's a meaningful write
      when(wbStageIsValid && rawWriteEn && rawWriteAddr =/= 0) {
        report(L"[Reg] Write ${rawWriteData} to register ${rawWriteAddr}")
        regFile(rawWriteAddr) := rawWriteData
      }
      // Report ignored writes to R0 only when the stage is valid
      when(wbStageIsValid && rawWriteEn && rawWriteAddr === 0) {
        report(L"[Reg] Attempting to write to R0 (ignored)")
      }
      // Optional: Report when WB is valid but not writing
      when(wbStageIsValid && !rawWriteEn) {
        report(L"[RegWB-Debug] WB Stage valid but not writing (writeEn=False)")
      }
      // Optional: Report when WB is simply not valid
      // when(!wbStageIsValid) {
      //     report(L"[RegWB-Debug] WB Stage not valid")
      // }
    }
  }
}
