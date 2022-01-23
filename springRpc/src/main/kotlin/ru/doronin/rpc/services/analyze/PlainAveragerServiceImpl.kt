package ru.doronin.rpc.services.analyze

import io.grpc.stub.StreamObserver
import ru.doronin.rpc.analyze.AveragingResult
import ru.doronin.rpc.analyze.AveragingValue
import ru.doronin.rpc.analyze.SimpleAveragerServiceGrpc

class PlainAveragerServiceImpl : SimpleAveragerServiceGrpc.SimpleAveragerServiceImplBase() {
    override fun calculateAverage(responseObserver: StreamObserver<AveragingResult>): StreamObserver<AveragingValue> =
        object : StreamObserver<AveragingValue> {
            val container = mutableListOf<Double>()
            override fun onNext(value: AveragingValue) {
                println("Received ${value.meter} in Plain")
                container.add(value.meter)
            }

            override fun onError(t: Throwable) {
                TODO("Not yet implemented")
            }

            override fun onCompleted() {
                responseObserver.onNext(AveragingResult.newBuilder().setResultValue(container.average()).build())
                responseObserver.onCompleted()
            }
        }
}
