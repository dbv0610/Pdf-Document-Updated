package com.ag.sampleadsfirstflow.remoteconfig.config

sealed class RemoteKeys(open val remoteKey: String) {
    sealed class BooleanKey(remoteKey: String, defaultValue: Boolean) : RemoteKeys(remoteKey) {
        var defaultValue: Boolean = defaultValue
            private set
    }

    sealed class StringKey(remoteKey: String, defaultValue: String) : RemoteKeys(remoteKey) {
        var defaultValue: String = defaultValue
            private set
    }

    sealed class LongKey(remoteKey: String, defaultValue: Long) : RemoteKeys(remoteKey) {
        var defaultValue: Long = defaultValue
            private set
    }

    sealed class DoubleKey(remoteKey: String, defaultValue: Double) : RemoteKeys(remoteKey) {
        var defaultValue: Double = defaultValue
            private set
    }

    sealed class StringEnumKey<T : RemoteEnumString>(remoteKey: String, defaultValue: T) : RemoteKeys(remoteKey) {
        var defaultValue: T = defaultValue
            private set
    }

    sealed class ListIntegerKey(remoteKey: String, defaultValue: List<Int>) : RemoteKeys(remoteKey) {
        var defaultValue: List<Int> = defaultValue
            private set
    }
}

interface RemoteEnumString {
    val remoteValue: String
}
