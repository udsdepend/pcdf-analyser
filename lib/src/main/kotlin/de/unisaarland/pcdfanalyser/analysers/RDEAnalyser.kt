package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import de.unisaarland.pcdfanalyser.eventStream.reduce
import de.unisaarland.pcdfanalyser.model.RDEResult
import pcdfEvent.EventType
import pcdfEvent.PCDFEvent
import pcdfEvent.events.GPSEvent
import pcdfEvent.events.obdEvents.OBDCommand
import pcdfEvent.events.obdEvents.obdIntermediateEvents.OBDIntermediateEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.MAFSensorEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.reducedComponentEvents.FuelRateReducedEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.reducedComponentEvents.NOXReducedEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.*
import java.io.File

const val VERBOSITY_MODE = true

class RDEAnalyser(inputStream: EventStream): Analyser<RDEResult?>(inputStream) {

    val isLolaAvailable: Boolean

    val fuelType: String?
    val supportedPIDs = SupportedPIDsAnalyser(eventStream).analyse().availablePIDs.filter { it.mode == 1 }.map { it.id }


    private val specBody: String
    private val specHeader: String
    private val specFuelRateInput: String
    private val specFuelRateToCo2Diesel: String
    private val specFuelRateToEMFDiesel: String
    private val specFuelRateToCo2Gasoline: String
    private val specFuelRateToEMFGasoline: String
    private val specMAFToFuelRateDieselFAE: String
    private val specMAFToFuelRateDiesel: String
    private val specMAFToFuelRateGasolineFAE: String
    private val specMAFToFuelRateGasoline: String


    init {
        val lolaLocation = File("../../RTLola-Kotlin-Bridge/target/release/librde_bridge.dylib").canonicalPath
        println("Looking for RTLola: ${lolaLocation}")
        isLolaAvailable = try {
            System.load(lolaLocation)
            println("RTLola successfully loaded.")
            true
        } catch (e: UnsatisfiedLinkError) {
            println("RTLola could not be loaded from ${lolaLocation}!")
            false
        }

        var ft: String? = null
        for (event in eventStream) {
            if (event is FuelTypeEvent) {
                ft = event.fueltype
                break
            }
        }
        fuelType = ft




        specBody = javaClass.classLoader.getResource("lola-spec/spec_body.lola")?.readText()!!

        specHeader = javaClass.classLoader.getResource("lola-spec/spec_header.lola")?.readText()!!

        specFuelRateInput = javaClass.classLoader.getResource("lola-spec/spec_fuel_rate_input.lola")?.readText()!!

        specFuelRateToCo2Diesel = javaClass.classLoader.getResource("lola-spec/spec_fuel_rate_to_co2_diesel.lola")?.readText()!!

        specFuelRateToEMFDiesel = javaClass.classLoader.getResource("lola-spec/spec_fuel_rate_to_emf_diesel.lola")?.readText()!!

        specFuelRateToCo2Gasoline = javaClass.classLoader.getResource("lola-spec/spec_fuelrate_to_co2_gasoline.lola")?.readText()!!

        specFuelRateToEMFGasoline = javaClass.classLoader.getResource("lola-spec/spec_fuelrate_to_emf_gasoline.lola")?.readText()!!

        specMAFToFuelRateDieselFAE = javaClass.classLoader.getResource("lola-spec/spec_maf_to_fuel_rate_diesel_fae.lola")?.readText()!!

        specMAFToFuelRateDiesel = javaClass.classLoader.getResource("lola-spec/spec_maf_to_fuel_rate_diesel.lola")?.readText()!!

        specMAFToFuelRateGasolineFAE = javaClass.classLoader.getResource("lola-spec/spec_maf_to_fuel_rate_gasoline_fae.lola")?.readText()!!

        specMAFToFuelRateGasoline = javaClass.classLoader.getResource("lola-spec/spec_maf_to_fuel_rate_gasoline.lola")?.readText()!!

    }

    override fun analysisIsAvailable(): Boolean {
        return isLolaAvailable
    }

    override fun analyse(): RDEResult? {
        monitorOffline()
        // TODO: return result!
        return null
    }

    /**
     * Initializes the RTLola monitor with a given specification.
     *
     * @param spec RTLola specification as a string.
     * @return String "worked" if initialization went fine, or some error description if something went wrong.
     */
    private external fun initmonitor(spec: String): String

    /**
     * Sends an array of update values to the RTLola engine to extend the InputStreams.
     * The relevant RTLola OutputStreams are periodic streams (sampled at 1Hz), therefore the returned array of relevant
     * OutputStream-Values may be empty,if the sent event did not make the streams evaluate, or could contain a lot more
     * than one value per OutputStream (the events are ordered linearly), if the sent event makes a leap in time (e.g.
     * after a connection loss).
     *
     * @param inputs Array of extension values for the RTLola InputStreams, in the same order they are defined in the
     * given RTLola specification.
     * @return Array of relevant outputs, if there are new events.
     */
    private external fun sendevent(inputs: DoubleArray): DoubleArray







    /* MONITORING CODE FOLLOWING */

    // Last event time in seconds.
    private var time: Double = 0.0

    // The sensor profile of the car which is determined.
    var rdeProfile: MutableList<OBDCommand> = mutableListOf()

    private var fuelRateSupported = false
    private var faeSupported = false



    // Latest relevant values from OBD- and GPSSource.
    private var inputs: MutableMap<RDE_RTLOLA_INPUT_QUANTITIES, Double?> = mutableMapOf(
        RDE_RTLOLA_INPUT_QUANTITIES.VELOCITY to null,
        RDE_RTLOLA_INPUT_QUANTITIES.ALTITUDE to null,
        RDE_RTLOLA_INPUT_QUANTITIES.TEMPERATURE to null,
        RDE_RTLOLA_INPUT_QUANTITIES.NOX_PPM to null,
        RDE_RTLOLA_INPUT_QUANTITIES.MASS_AIR_FLOW to null,
        RDE_RTLOLA_INPUT_QUANTITIES.FUEL_RATE to null,
        RDE_RTLOLA_INPUT_QUANTITIES.FUEL_AIR_EQUIVALENCE to null
    )

    private val inputValues: List<Double?>
    get() = listOf(
        inputs[RDE_RTLOLA_INPUT_QUANTITIES.VELOCITY],
        inputs[RDE_RTLOLA_INPUT_QUANTITIES.ALTITUDE],
        inputs[RDE_RTLOLA_INPUT_QUANTITIES.TEMPERATURE],
        inputs[RDE_RTLOLA_INPUT_QUANTITIES.NOX_PPM],
        inputs[RDE_RTLOLA_INPUT_QUANTITIES.MASS_AIR_FLOW],
        inputs[RDE_RTLOLA_INPUT_QUANTITIES.FUEL_RATE],
        inputs[RDE_RTLOLA_INPUT_QUANTITIES.FUEL_AIR_EQUIVALENCE])


    private fun loadSpecComponents() {

    }

    /*
        Initial data is complete if we received values for all the sensors in the determined sensor profile and GPS data.
        If complete, we can start communicating with the RTLola engine.
     */
    private val initialDataComplete: Boolean
        get() {
            var countAvailable = 0
            for (pair in inputs) {
                if (pair.value != null) {
                    countAvailable++
                }
            }
            return countAvailable == rdeProfile.size + 1
        }

    /**
     * Build the specification depending on the determined sensor profile and initialize the RTLola monitor.
     */
    private fun initSpec() {
        initmonitor(
            buildSpec()
        )
    }

    private fun startRDETrack() {
        for (event in eventStream) {
            collectData(event)
        }
    }

//    private fun getSupportedPids(): List<Int> {
//        val pidAnalyser = SupportedPIDsAnalyser(eventStream).analyse().availablePIDs.filter { it.mode == 1 }.map { it.id }
//        val pidSupport = pidAnalyser
//        return pidSupport.availablePIDs.filter { it.mode == 1 }.map { it.id }
//    }

//    private fun getFuelType(): String {
//        return fuelType!!
//    }

    /**
     * Main function for handling an received PCDFEvent and communicating with the RTLola engine.
     * Updates the [inputs], adds the current time (highest received timestamp) and sends inputs to the RTLola monitor.
     */
    private fun collectData(event: PCDFEvent): DoubleArray {
        if (event.type == EventType.GPS) {
            inputs[RDE_RTLOLA_INPUT_QUANTITIES.ALTITUDE] = (event as GPSEvent).altitude
        } else if (event.type == EventType.OBD_RESPONSE) {
            // Reduces the event if possible (e.g. NOx or FuelRate events) using the PCDFCore library.
            collectOBDEvent(event.reduce().toIntermediate() as OBDIntermediateEvent)
        }

        // Check whether we have received data for every input needed and that we are not paused (bluetooth disconnected).
        if (initialDataComplete) {
            val inputsToSend = mutableListOf<Double>()

            for (input in this.inputs.values) {
                if (input != null) {
                    inputsToSend.add(input)
                }
            }
            // Prevent time from going backwards
            time = maxOf(time, event.timestamp.toDouble() / 1_000_000_000.0)
            inputsToSend.add(time)
            val inputsArray = inputsToSend.toDoubleArray()

            if (VERBOSITY_MODE) {
                println("Sending(Lola): ${inputsArray.joinToString()}")
            }
            // Send latest received inputs to the RTLola monitor to update our streams, in return we receive an array of
            // values of selected OutputStreams (see: lola-rust-bridge) which we send to the outputchannel (e.g. the UI).
            val lolaResult = sendevent(inputsArray)
            if (VERBOSITY_MODE) {
                println("Receiving(Lola): ${lolaResult.joinToString()}")
            }
            // The result may be empty, since we are referring to periodic streams (1 Hz). So we receive updated results
            // every full second.
            if (lolaResult.isNotEmpty()) {
                // TODO: Use lola result!!
            }
            return lolaResult
        }

        return doubleArrayOf()
    }

    /**
     * Checks an received OBDEvent for relevance for our RDE track and updates the input values for the RTLola engine
     * accordingly.
     * @param event [PCDFEvent] to be collected.
     */
    private fun collectOBDEvent(event: OBDIntermediateEvent) {
        when (event) {
            is SpeedEvent -> {
                inputs[RDE_RTLOLA_INPUT_QUANTITIES.VELOCITY] = event.speed.toDouble()
            }
            is AmbientAirTemperatureEvent -> {
                inputs[RDE_RTLOLA_INPUT_QUANTITIES.TEMPERATURE] = event.temperature.toDouble() + 273.15 // C -> K
            }
            is MAFAirFlowRateEvent -> {
                inputs[RDE_RTLOLA_INPUT_QUANTITIES.MASS_AIR_FLOW] = event.rate
            }
            is MAFSensorEvent -> {
                inputs[RDE_RTLOLA_INPUT_QUANTITIES.MASS_AIR_FLOW] = event.mafSensorA // TODO: add reduction for this one
            }
            is NOXReducedEvent -> {
                inputs[RDE_RTLOLA_INPUT_QUANTITIES.NOX_PPM] = event.nox_ppm.toDouble()
            }
            is FuelRateReducedEvent -> {
                inputs[RDE_RTLOLA_INPUT_QUANTITIES.FUEL_RATE] = event.fuelRate
            }
            is FuelAirEquivalenceRatioEvent -> {
                inputs[RDE_RTLOLA_INPUT_QUANTITIES.FUEL_AIR_EQUIVALENCE] = event.ratio
            }
        }
    }

    /**
     * Checks the connected car's supported sensor profile for RDE test sufficiency and determines which available sensors
     * should be used.
     * Generates the [rdeProfile] of PIDs to be used.
     *
     * @param supportedPIDs The list of the car's supported PIDs, from [getSupportedPids].
     * @param fuelType The car's fuel type, from [getFuelType].
     * @return If an RDE test is possible with the connected car.
     */
    private fun checkSupportedPids(): Boolean {
        // If the car is not a diesel or gasoline, the RDE test is not possible since there are no corresponding
        // specifications.
        if (fuelType != "Diesel" && fuelType != "Gasoline") {
            println("RDEAnalyser: Incompatible for RDE: Fuel type unknown or invalid ('${fuelType}')")
            return false
        }

        // Velocity information to determine acceleration, distance travelled and to calculate the driving dynamics.
        if (supportedPIDs.contains(0x0D)) {
            rdeProfile.add(OBDCommand.SPEED)
        } else {
            println("RDEAnalyser: Incompatible for RDE: Speed data not provided by the car.")
            return false
        }

        // Ambient air temperature for checking compliance with the environmental constraints.
        if (supportedPIDs.contains(0x46)) {
            rdeProfile.add(OBDCommand.AMBIENT_AIR_TEMPERATURE)
        } else {
            println("RDEAnalyser: Incompatible for RDE: Ambient air temperature not provided by the car.")
            return false
        }

        // NOx sensor(s) to check for violation of the EU regulations.
        when {
            supportedPIDs.contains(0x83) -> {
                rdeProfile.add(OBDCommand.NOX_SENSOR)
            }
            supportedPIDs.contains(0xA1) -> {
                rdeProfile.add(OBDCommand.NOX_SENSOR_CORRECTED)
            }
            supportedPIDs.contains(0xA7) -> {
                rdeProfile.add(OBDCommand.NOX_SENSOR_ALTERNATIVE)
            }
            supportedPIDs.contains(0xA8) -> {
                rdeProfile.add(OBDCommand.NOX_SENSOR_CORRECTED_ALTERNATIVE)
            } else -> {
            println("RDEAnalyser: Incompatible for RDE: NOx sensor not provided by the car.")
            return false
        }
        }

        // Fuelrate sensors for calculation of the exhaust mass flow. Can be replaced through MAF.
        // TODO: ask Maxi for the EMF PID
        fuelRateSupported = when {
            supportedPIDs.contains(0x5E) -> {
                rdeProfile.add(OBDCommand.ENGINE_FUEL_RATE)
                true
            }
            supportedPIDs.contains(0x9D) -> {
                rdeProfile.add(OBDCommand.ENGINE_FUEL_RATE_MULTI)
                true
            } else -> {
                println("RDEAnalyser: Fuel rate not provided by the car.")
                false
            }
        }

        // Mass air flow rate for the calcuation of the exhaust mass flow.
        when {
            supportedPIDs.contains(0x10) -> {
                rdeProfile.add(OBDCommand.MAF_AIR_FLOW_RATE)
            }
            supportedPIDs.contains(0x66) -> {
                rdeProfile.add(OBDCommand.MAF_AIR_FLOW_RATE_SENSOR)
            } else -> {
            println("RDEAnalyser: Incompatible for RDE: Mass air flow not provided by the car.")
            return false
        }
        }

        // Fuel air equivalence ratio for a more precise calculation of the fuel rate with MAF.
        faeSupported = if (supportedPIDs.contains(0x44) && !fuelRateSupported) {
            rdeProfile.add(OBDCommand.FUEL_AIR_EQUIVALENCE_RATIO)
            true
        } else {
            println("RDEAnalyser: Fuel air equivalence ratio not provided by the car.")
            false
        }

        println("RDEAnalyser: Car compatible for RDE tests.")

        return true
    }

    /**
     * Builds the RTLola specification to be used for the RDE test, depending on the car's
     * determined sensor profile from [checkSupportedPids].
     * @return The RTLola specification for initialization of the RTLola monitor.
     */
    private fun buildSpec(): String {
        val s = StringBuilder()
        s.append(specHeader)

        if (fuelRateSupported) {
            s.append(specFuelRateInput)
        } else {
            when (fuelType) {
                "Diesel" -> {
                    if (faeSupported) {
                        s.append(specMAFToFuelRateDieselFAE)
                    } else {
                        s.append(specMAFToFuelRateDiesel)
                    }
                }
                "Gasoline" -> {
                    if (faeSupported) {
                        s.append(specMAFToFuelRateGasolineFAE)
                    } else {
                        s.append(specMAFToFuelRateGasoline)
                    }
                }
            }
        }
        when (fuelType) {
            "Diesel" -> {
                s.append(specFuelRateToCo2Diesel)
                s.append(specFuelRateToEMFDiesel)
            }
            "Gasoline" -> {
                s.append(specFuelRateToCo2Gasoline)
                s.append(specFuelRateToEMFGasoline)
            }
        }
        s.append(specBody)
        val tempFile = File("temp_spec.lola")
        tempFile.writeText(s.toString())
        return s.toString()
    }

    private fun monitorOffline(): DoubleArray {
        if (supportedPIDs.isEmpty() || fuelType == null || fuelType.isBlank()) {
            throw IllegalStateException()
        }

        // Check Supported PIDs
        val supported = checkSupportedPids()
        if (!supported) {
            throw IllegalStateException()
        }

        // Setup RTLola Monitor
        initmonitor(
            buildSpec()
        )

        var result = doubleArrayOf()
        val dataIterator = eventStream.iterator()

        // Collect events, similar to online monitoring.
        while (dataIterator.hasNext()) {
            val event = dataIterator.next()
            val lolaResult = collectData(event)
            result = if (lolaResult.isNotEmpty()) { lolaResult } else { result }
        }
        return result
    }














    enum class RDE_RTLOLA_INPUT_QUANTITIES {
        VELOCITY,
        ALTITUDE,
        TEMPERATURE,
        NOX_PPM,
        MASS_AIR_FLOW,
        FUEL_RATE,
        FUEL_AIR_EQUIVALENCE
    }


}