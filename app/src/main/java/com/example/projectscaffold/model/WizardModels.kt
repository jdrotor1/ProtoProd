package com.example.projectscaffold.model

/**
 * Linear wizard — 34 screens (33 questions + 1 review).
 * Mirrors v3 PWA's QUESTIONS array exactly.
 */
sealed class WizardQuestion {
    abstract val idx: Int

    data class Text(
        override val idx: Int,
        val key: String,
        val label: String,
        val help: String = "",
        val required: Boolean = false
    ) : WizardQuestion()

    data class Spec(
        override val idx: Int,
        val key: String,
        val label: String,
        val multi: Boolean,
        val options: List<String>
    ) : WizardQuestion()

    data class CategoryQ(
        override val idx: Int,
        val catKey: String,
        val name: String,
        val multi: Boolean,
        val parts: Boolean = true
    ) : WizardQuestion()

    data class Review(override val idx: Int) : WizardQuestion()
}

object Questions {

    val ALL: List<WizardQuestion> = buildList {
        var i = 0
        // Basics
        add(WizardQuestion.Text(i++, "project_name", "Project Name", "e.g., AcmeWidget v2", required = true))
        add(WizardQuestion.Text(i++, "tooling", "Tooling", "e.g., KiCad 8 + Zephyr 3.7 + Android Studio"))
        add(WizardQuestion.Text(i++, "connectivity_summary", "Connectivity (one-line summary)", "e.g., BLE 5 to companion app + Wi-Fi to MQTT broker"))
        // Production-defining specs
        add(WizardQuestion.Spec(i++, "target_production_volume", "Target Production Volume", false,
            listOf("One-off (1)","Tens (10s)","Hundreds (100s)","Thousands (1k)","Tens of thousands (10k+)","[User Defined]")))
        add(WizardQuestion.Spec(i++, "bom_cost_target", "BOM Cost Target (per unit)", false,
            listOf("< $10","$10–50","$50–200","$200–1k","> $1k","[User Defined]")))
        add(WizardQuestion.Spec(i++, "operating_environment", "Operating Environment", false,
            listOf("Indoor controlled","Indoor uncontrolled","Outdoor sheltered","Outdoor exposed","Automotive cabin","Industrial","[User Defined]")))
        add(WizardQuestion.Spec(i++, "environmental_specs", "Environmental Specs", true,
            listOf("IP54","IP65","IP67","IP68","NEMA 4X","Temp -40 to 85°C","Temp 0 to 70°C","Humidity 95% non-condensing","[User Defined]")))
        add(WizardQuestion.Spec(i++, "power_source", "Power Source", true,
            listOf("USB-C","Barrel jack","LiPo single-cell","LiPo multi-cell","Li-ion 18650","Alkaline AA/AAA","Mains AC","PoE","Solar","[User Defined]")))
        add(WizardQuestion.Spec(i++, "emc_emi_class", "EMC/EMI Target Class", false,
            listOf("None","FCC Class A (industrial)","FCC Class B (residential)","CISPR 22 Class A","CISPR 22 Class B","Automotive (CISPR 25)","[User Defined]")))
        add(WizardQuestion.Spec(i++, "compliance_markings", "Compliance Markings", true,
            listOf("None","FCC Part 15","CE","UKCA","RoHS","REACH","UL","CSA","[User Defined]")))
        // Catalog categories
        add(WizardQuestion.CategoryQ(i++, "CONNECTIVITY",   "Connectivity",          multi = true))
        add(WizardQuestion.CategoryQ(i++, "POWER",          "Power",                 multi = true))
        add(WizardQuestion.CategoryQ(i++, "SENSING",        "Sensing",               multi = true))
        add(WizardQuestion.CategoryQ(i++, "ACTUATION",      "Actuation",             multi = true))
        add(WizardQuestion.CategoryQ(i++, "COMPUTE_MCU",    "Compute/MCU",           multi = false))
        add(WizardQuestion.CategoryQ(i++, "MECH_ENCLOSURE", "Mechanical/Enclosure",  multi = false))
        add(WizardQuestion.CategoryQ(i++, "FASTENERS",      "Fasteners",             multi = true))
        add(WizardQuestion.CategoryQ(i++, "DISPLAY_UI",     "Display/UI",            multi = true))
        add(WizardQuestion.CategoryQ(i++, "STORAGE",        "Storage",               multi = true))
        add(WizardQuestion.CategoryQ(i++, "COMMS_PROTOCOL", "Comms-Protocol",        multi = true,  parts = false))
        add(WizardQuestion.CategoryQ(i++, "AUTOMOTIVE",     "Automotive",            multi = true))
        add(WizardQuestion.CategoryQ(i++, "MECHATRONICS",   "Mechatronics",          multi = true))
        add(WizardQuestion.CategoryQ(i++, "HAZMAT_SDS",     "Hazmat/SDS",            multi = true))
        add(WizardQuestion.CategoryQ(i++, "EDA_NETLIST",    "EDA / Netlist Format",  multi = false, parts = false))
        // Output-shaping specs
        add(WizardQuestion.Spec(i++, "cad_export_format", "CAD Export Format", false,
            listOf("STEP","STL","IGES","Parasolid","FreeCAD native","Fusion 360 native","[User Defined]")))
        add(WizardQuestion.Spec(i++, "pcb_output_format", "PCB Output Format", true,
            listOf("Gerber RS-274X","Gerber X2","ODB++","IPC-2581","[User Defined]")))
        add(WizardQuestion.Spec(i++, "diagnostic_interface", "Diagnostic Interface", true,
            listOf("None","JTAG","SWD","USB CDC console","UART debug","[User Defined]")))
        // Process specs
        add(WizardQuestion.Spec(i++, "version_control_host", "Version Control Host", false,
            listOf("GitHub","GitLab","Bitbucket","Self-hosted Gitea","Self-hosted GitLab","[User Defined]")))
        add(WizardQuestion.Spec(i++, "cicd_pipeline", "CI/CD Pipeline", false,
            listOf("GitHub Actions","GitLab CI","Jenkins","CircleCI","None","[User Defined]")))
        add(WizardQuestion.Spec(i++, "build_reproducibility", "Build Reproducibility", false,
            listOf("Loose (host toolchain)","Pinned toolchain (versions noted)","Containerized (Docker)","Nix flake","[User Defined]")))
        add(WizardQuestion.Spec(i++, "ota_strategy", "OTA Update Strategy", false,
            listOf("None","MCUboot","ESP-IDF OTA","Zephyr MCUmgr","Custom dual-bank","Cloud-managed (AWS/Azure)","[User Defined]")))
        add(WizardQuestion.Spec(i++, "logging_telemetry", "Logging / Telemetry", true,
            listOf("None","Serial console only","On-device file log","Cloud telemetry","Crash dumps","[User Defined]")))
        add(WizardQuestion.Spec(i++, "test_strategy", "Test Strategy", true,
            listOf("Bench-only","Functional fixture","ICT bed-of-nails","Boundary scan","[User Defined]")))
        // Review
        add(WizardQuestion.Review(i))
    }

    val LAST_BUILD_STEP_IDX: Int = ALL.indexOfLast { it !is WizardQuestion.Review }
    val TOTAL: Int = ALL.size
}

/** Full wizard state — gathered linearly. */
data class WizardState(
    val idx: Int = 0,
    val texts: Map<String, String> = emptyMap(),
    val catSel: Map<String, Set<String>> = emptyMap(),
    val specSel: Map<String, Set<String>> = emptyMap(),
    val udText: Map<String, String> = emptyMap()
)
