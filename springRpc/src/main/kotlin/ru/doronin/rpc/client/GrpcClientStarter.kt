package ru.doronin.rpc.client

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import org.apache.commons.lang.RandomStringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component
import ru.doronin.rpc.analyze.*
import ru.doronin.rpc.cuckoo.DummyCuckooServiceGrpcKt
import ru.doronin.rpc.cuckoo.LifePredictionRequest
import ru.doronin.rpc.cuckoo.ShoutRequest
import ru.doronin.rpc.meters.*
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@Component
@DependsOn("grpcServerStarter")
class GrpcClientStarter(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) :
    ApplicationListener<ApplicationReadyEvent> {
    @Value(value = "\${rpc.server.port}")
    var serverPort: Int? = null


    private val observer = object : StreamObserver<Meter> {
        override fun onNext(meter: Meter) {
            println("Got message of type ${meter.type} with actual value ${meter.value}")
        }

        override fun onError(t: Throwable) {
            t.printStackTrace()
        }

        override fun onCompleted() {
            println("That's all folks!")
        }
    }

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        GlobalScope.async(dispatcher) {
            launch {
                val managedChannel = ManagedChannelBuilder.forAddress("localhost", serverPort ?: 50051)
                    .usePlaintext()
                    .build()

                configureMonitoringClient(managedChannel)
                configureCuckooClient(managedChannel)
                configurePlainAveragerClient(managedChannel)
                configureKotlinPoweredAveragerClient(managedChannel)


                configurePlainSmoother(managedChannel)
                configureKtSmoother(managedChannel)

                Runtime.getRuntime().addShutdownHook(Thread {
                    println("Received shutdown event at ${Instant.now()}")
                    managedChannel.shutdown()
                    println("GrpcClient stopped at ${Instant.now()}")
                })
                managedChannel.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)
            }
        }
    }

    private suspend fun configureKtSmoother(managedChannel: ManagedChannel) {
        val client = SmoothingServiceGrpcKt.SmoothingServiceCoroutineStub(managedChannel)
        val rawValuesFlow = flow<SmoothingRequest> {
            val randomValue = Random.nextDouble(1.0, 100.0)
            println("Passing $randomValue to kotlin smoother")
            emit(SmoothingRequest.newBuilder().setRawValue(randomValue).build())
        }.take(100)
        val responseFlow = client.runOnlineSmoothing(rawValuesFlow)
        responseFlow.collect { resultValue ->
            println("Smoothed value to kotlin flow is ${resultValue.smoothValue}")
        }
        println("Kotlin smoothing done")
    }

    private fun configurePlainSmoother(managedChannel: ManagedChannel) {
        val client = SmoothingServiceGrpc.newStub(managedChannel)
        val smoothedObserver = client.runOnlineSmoothing(object : StreamObserver<SmoothingResponse> {
            override fun onNext(value: SmoothingResponse) {
                println("Smoothed value to plain observer is ${value.smoothValue}")
            }

            override fun onError(t: Throwable) {
                TODO("Not yet implemented")
            }

            override fun onCompleted() {
                println("Execution ended")
            }
        })

        for (index in 0..100) {
            val randomValue = Random.nextDouble(1.0, 100.0)
            println("Passing $randomValue to smoother")
            smoothedObserver.onNext(SmoothingRequest.newBuilder().setRawValue(randomValue).build())
        }
        println("Smoothing transfer done")
        smoothedObserver.onCompleted()
    }

    private suspend fun configureKotlinPoweredAveragerClient(managedChannel: ManagedChannel) {
        val client = SimpleAveragerServiceGrpcKt.SimpleAveragerServiceCoroutineStub(managedChannel)
        val values = flow<AveragingValue> {
            val randomValue = Random.nextDouble(1.0, 100.0)
            println("Passing $randomValue to kotlin averager")
            emit(AveragingValue.newBuilder().setMeter(randomValue).build())
        }.take(50)
        val averageValue = client.calculateAverage(values)
        println("Kotlin result is ${averageValue.resultValue}")
    }

    private fun configurePlainAveragerClient(managedChannel: ManagedChannel) {
        val client = SimpleAveragerServiceGrpc.newStub(managedChannel)
        val valuesObserver = client.calculateAverage(object : StreamObserver<AveragingResult> {
            override fun onNext(value: AveragingResult) {
                println("Result in plain is ${value.resultValue}")
            }

            override fun onError(t: Throwable) {
                TODO("Not yet implemented")
            }

            override fun onCompleted() {
                println("Execution ended")
            }
        })
        for (index in 0..50) {
            val randomValue = Random.nextDouble(1.0, 100.0)
            println("Passing $randomValue to averager")
            valuesObserver.onNext(AveragingValue.newBuilder().setMeter(randomValue).build())
        }
        println("Transfer done")
        valuesObserver.onCompleted()
    }

    private suspend fun configureCuckooClient(managedChannel: ManagedChannel) {
        val client = DummyCuckooServiceGrpcKt.DummyCuckooServiceCoroutineStub(managedChannel)
        val name = RandomStringUtils.randomAlphabetic(10)
        val swearing = RandomStringUtils.randomAscii(10)
        val shortResponse = client.shout(ShoutRequest.newBuilder().setName(name).setSwearing(swearing).build())
        println("$name shouted $swearing\n  Cuckoo pounded ${shortResponse.text}")

        val asker = RandomStringUtils.randomAlphabetic(10)
        var counter = 0
        println("$asker asked about life remaining...")
        client.ask(LifePredictionRequest.newBuilder().setName(asker).build())
            .onEach {
                counter++
            }.collect { println("Cuckoo: $it") }
        println("Cuckoo predicted $counter years remaining for $asker")
    }

    private fun configureMonitoringClient(managedChannel: ManagedChannel) {
        val client = SimpleMonitoringServiceGrpc.newStub(managedChannel)
        println("Monitoring client configured")
        client.getActual(ActualValueRequest.newBuilder().setType(MessageType.TEMPERATURE).build(), observer)
        println("Monitoring subscription started")
        client.subscribe(SubscriptionRequest.newBuilder().setType(MessageType.WIND).build(), observer)
    }
}
