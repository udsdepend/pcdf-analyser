package de.unisaarland.pcdfanalyser.model

import pcdfEvent.events.obdEvents.OBDCommand

/**
 * Represents a set of supported and available parameter IDs.
 * A parameter ID is <i>supported</i> if it appears in the list of supported PIDs received from PIDs 0x00, 0x20, etc.
 * The parameter ID is available w.r.t. to some event stream, if values for this parameter ID appear in the event stream.
 * For availability, the event stream to which the availability information belongs to
 * is not stored here.
 */
class ParameterSupport(parameterRecords: List<Record> = listOf()) {
    private val records: MutableList<Record> = parameterRecords.toMutableList()

    val parameterRecords: List<Record>
    get() = records

    val supportedPIDs: List<ParameterID>
    get() = parameterRecords.filter { it.supported }.map { it.parameterID }

    val availablePIDs: List<ParameterID>
        get() = parameterRecords.filter { it.available }.map { it.parameterID }

    /**
     * Insert or update the record for [parameterID]. The new record contains information about
     * whether the PID is [supported] (according to the SupportedPIDs PID) and whether it is
     * [available] in a certain event stream (i.e., if values of this PID appear in the stream).
     */
    fun setParameterID(parameterID: ParameterID, supported: Boolean, available: Boolean) {
        records.removeIf { it.parameterID == parameterID }
        records.add(Record(parameterID, supported, available))
    }

    /**
     * Returns the internal record for [parameterID].
     */
    fun recordForParameter(parameterID: ParameterID): Record? {
        return records.find { it.parameterID == parameterID }
    }

    /**
     * @return true if [parameterID] is supported.
     */
    fun isParameterSupported(parameterID: ParameterID): Boolean {
        return recordForParameter(parameterID)?.supported ?: false
    }

    /**
     * @return true if [parameterID] is available in a certain event stream.
     */
    fun isParameterAvailable(parameterID: ParameterID): Boolean {
        return recordForParameter(parameterID)?.available ?: false
    }

    override fun equals(other: Any?): Boolean {
        if (other is ParameterSupport) {
            return records.sortedBy { it.parameterID }.equals(other.records.sortedBy { it.parameterID })
        } else {
            return false
        }
    }

    override fun toString(): String {
        return "ParameterSupport {${records.toString()}}"
    }


    /**
     * An (internal) data record for a [parameterID].
     */
    data class Record(val parameterID: ParameterID, val supported: Boolean, val available: Boolean)
}