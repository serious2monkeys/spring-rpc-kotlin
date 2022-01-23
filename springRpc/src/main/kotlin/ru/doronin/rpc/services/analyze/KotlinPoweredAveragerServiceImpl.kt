package ru.doronin.rpc.services.analyze

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.reduce
import ru.doronin.rpc.analyze.AveragingResult
import ru.doronin.rpc.analyze.AveragingValue
import ru.doronin.rpc.analyze.SimpleAveragerServiceGrpcKt

class KotlinPoweredAveragerServiceImpl : SimpleAveragerServiceGrpcKt.SimpleAveragerServiceCoroutineImplBase() {

    override suspend fun calculateAverage(requests: Flow<AveragingValue>): AveragingResult {
        var counter = 0
        val actualValue = requests.map { request -> request.meter }
            .onEach {
                println("Received $it in KotlinPowered")
                counter++
            }
            .reduce { accumulator, value -> accumulator + value }
        return AveragingResult.newBuilder().setResultValue(actualValue / counter).build()
    }
}
