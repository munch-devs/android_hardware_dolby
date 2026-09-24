package com.aosp.dolby.geq.data

data class Preset(
    var name: String,
    val bandGains: List<BandGain>,
    var isUserDefined: Boolean = false,
    var isMutated: Boolean = false
)
