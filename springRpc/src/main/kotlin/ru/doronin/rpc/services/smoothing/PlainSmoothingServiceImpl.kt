package ru.doronin.rpc.services.smoothing

import com.mchange.util.impl.CircularList
import io.grpc.stub.StreamObserver
import ru.doronin.rpc.analyze.SmoothingRequest
import ru.doronin.rpc.analyze.SmoothingResponse
import ru.doronin.rpc.analyze.SmoothingServiceGrpc

class PlainSmoothingServiceImpl: SmoothingServiceGrpc.SmoothingServiceImplBase() {

    override fun runOnlineSmoothing(responseObserver: StreamObserver<SmoothingResponse>): StreamObserver<SmoothingRequest> =
        object: StreamObserver<SmoothingRequest> {
            val window = CircularList()
            override fun onNext(value: SmoothingRequest) {
                println("Received ${value.rawValue} in plain implementation")
                window.appendElement(value.rawValue)
                if (window.size() > 5) {
                    window.removeFirstElement()
                }
                if (window.size() == 5) {
                    val smoothedValue = window.elements(true, true).asSequence()
                        .map { it as Double }
                        .average()
                    println("Emitting $smoothedValue from Plain implementation")
                    responseObserver.onNext(SmoothingResponse.newBuilder().setSmoothValue(smoothedValue).build())
                } else {
                    println("Smoothing skipped")
                }
            }

            override fun onError(t: Throwable?) {
                TODO("Not yet implemented")
            }

            override fun onCompleted() {
                responseObserver.onCompleted()
            }
        }
}
