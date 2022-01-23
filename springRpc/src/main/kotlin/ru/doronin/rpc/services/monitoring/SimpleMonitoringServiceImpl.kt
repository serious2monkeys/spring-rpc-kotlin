package ru.doronin.rpc.services.monitoring

import io.grpc.stub.StreamObserver
import ru.doronin.rpc.meters.*
import java.lang.Thread.sleep
import kotlin.random.Random

class SimpleMonitoringServiceImpl : SimpleMonitoringServiceGrpc.SimpleMonitoringServiceImplBase() {
    override fun subscribe(request: SubscriptionRequest, responseObserver: StreamObserver<Meter>) {
        for (index in 0..100) {
            randomizer(request.type, responseObserver)
            sleep(500)
        }
        responseObserver.onCompleted()
    }

    override fun getActual(request: ActualValueRequest, responseObserver: StreamObserver<Meter>) {
        randomizer(request.type, responseObserver)
        responseObserver.onCompleted()
    }

    private fun randomizer(type: MessageType, responseObserver: StreamObserver<Meter>) {
        when (type) {
            MessageType.TEMPERATURE -> responseObserver.onNext(
                Meter.newBuilder().setType(type).setValue(Random.nextDouble(-40.0, 40.0)).build()
            )
            MessageType.WIND -> responseObserver.onNext(
                Meter.newBuilder().setType(type).setValue(Random.nextDouble(0.0, 30.0)).build()
            )
            else -> throw IllegalArgumentException("Unknown measurement type: $type")
        }
    }
}
