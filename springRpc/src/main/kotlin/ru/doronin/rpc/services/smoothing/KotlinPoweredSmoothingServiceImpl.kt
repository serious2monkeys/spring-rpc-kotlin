package ru.doronin.rpc.services.smoothing

import com.mchange.util.impl.CircularList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import ru.doronin.rpc.analyze.SmoothingRequest
import ru.doronin.rpc.analyze.SmoothingResponse
import ru.doronin.rpc.analyze.SmoothingServiceGrpcKt

class KotlinPoweredSmoothingServiceImpl : SmoothingServiceGrpcKt.SmoothingServiceCoroutineImplBase() {
    override fun runOnlineSmoothing(requests: Flow<SmoothingRequest>): Flow<SmoothingResponse> {
        val window = CircularList()
        return flow {
            requests.collect { value ->
                println("Received ${value.rawValue} in Kotlin implementation")
                window.appendElement(value.rawValue)
                if (window.size() > 5) {
                    window.removeFirstElement()
                }
                if (window.size() == 5) {
                    val smoothedValue = window.elements(true, true).asSequence()
                        .map { it as Double }
                        .average()
                    println("Emitting $smoothedValue from Kotlin implementation")
                    emit(SmoothingResponse.newBuilder().setSmoothValue(smoothedValue).build())
                } else {
                    println("Smoothing skipped")
                }
            }
        }
    }
}
