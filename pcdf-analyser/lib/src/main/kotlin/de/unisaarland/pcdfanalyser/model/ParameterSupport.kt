package de.unisaarland.pcdfanalyser.model

class ParameterSupport(parameterRecords: List<Record> = listOf()) {
    private val records: MutableList<Record> = parameterRecords.toMutableList()

    val parameterRecords: List<Record>
    get() = records

    fun setParameterID(parameterID: ParameterID, supported: Boolean, available: Boolean) {
        records.removeIf { it.parameterID == parameterID }
        records.add(Record(parameterID, supported, available))
    }

    fun recordForParameter(parameterID: ParameterID): Record? {
        return records.find { it.parameterID == parameterID }
    }

    fun isParameterSupported(parameterID: ParameterID): Boolean {
        return recordForParameter(parameterID)?.supported ?: false
    }

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



    data class Record(val parameterID: ParameterID, val supported: Boolean, val available: Boolean)
}