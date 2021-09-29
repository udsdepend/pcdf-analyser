package de.unisaarland.pcdfanalyser.model

import pcdfEvent.events.obdEvents.OBDCommand

/**
 * Represents parameter IDs, i.e., pairs of a mode (or service) and the (numeric) parameter ID.
 */
class ParameterID(val id: Int, val mode: Int = 1) : Comparable<ParameterID> {

    constructor(obdCommand: OBDCommand): this(obdCommand.pid, obdCommand.mode)

    override fun compareTo(other: ParameterID): Int {
        val modeCompare = mode.compareTo(other.mode)
        return if (modeCompare == 0) {
            id.compareTo(other.id)
        } else {
            modeCompare
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is ParameterID) {
            return id == other.id && mode == other.mode
        } else {
            return false
        }
    }

    override fun toString(): String {
        val command = OBDCommand.getCommand(mode, id)
        return if (command != null) {
            command.name
        } else {
            "0x${id.toString(16)}"
        }
    }

    override fun hashCode(): Int {
        return mode + 10 * id
    }

}