package de.unisaarland.pcdfanalyser.model

data class RDEResult (
    val duration: Int, // in seconds
    val distance: Double,   // in km
    val constraintsSatisfied: Boolean,
    val emittedNOx: Double, // in mg/km
    //TODO: more
        ) {

}