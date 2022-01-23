package ru.doronin.rpc.server

import io.grpc.ServerBuilder
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import ru.doronin.rpc.services.analyze.KotlinPoweredAveragerServiceImpl
import ru.doronin.rpc.services.analyze.PlainAveragerServiceImpl
import ru.doronin.rpc.services.cuckoo.DummyCuckooServiceImpl
import ru.doronin.rpc.services.monitoring.SimpleMonitoringServiceImpl
import ru.doronin.rpc.services.smoothing.KotlinPoweredSmoothingServiceImpl
import ru.doronin.rpc.services.smoothing.PlainSmoothingServiceImpl
import java.time.Instant

@Component("grpcServerStarter")
class GrpcServerStarter(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) :
    ApplicationListener<ApplicationReadyEvent> {
    @Value(value = "\${rpc.server.port:50051}")
    var serverPort: Int? = null

    @Value(value = "\${service-implementations:java}")
    lateinit var implementationMode: String


    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        GlobalScope.async(dispatcher) {
            launch {
                val grpcServer = ServerBuilder.forPort(serverPort ?: 50051)
                    .addService(SimpleMonitoringServiceImpl())
                    .addService(DummyCuckooServiceImpl())
                    .addService(if ("kotlin" == implementationMode) KotlinPoweredAveragerServiceImpl() else PlainAveragerServiceImpl())
                    .addService(if ("kotlin" == implementationMode) KotlinPoweredSmoothingServiceImpl() else PlainSmoothingServiceImpl())
                    .build()
                grpcServer.start()
                println("GRPC SERVER STARTED")
                Runtime.getRuntime().addShutdownHook(Thread {
                    println("Received shutdown event at ${Instant.now()}")
                    grpcServer.shutdown()
                    println("GrpcServer stopped at ${Instant.now()}")
                })
                grpcServer.awaitTermination()
            }
        }
    }
}
