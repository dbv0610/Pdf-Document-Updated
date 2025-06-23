package com.ag.sampleadsfirstflow.model

data class FeatureModel(
    val icon: Int,
    val packageName: String,
    val name: String,
    var isSelected: Boolean = false,
)
