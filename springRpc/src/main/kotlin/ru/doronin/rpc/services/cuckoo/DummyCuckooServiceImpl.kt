package ru.doronin.rpc.services.cuckoo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.apache.commons.lang.RandomStringUtils
import ru.doronin.rpc.cuckoo.*

class DummyCuckooServiceImpl : DummyCuckooServiceGrpcKt.DummyCuckooServiceCoroutineImplBase() {

    override suspend fun shout(request: ShoutRequest): ShoutResponse {
        return ShoutResponse.newBuilder().setText(RandomStringUtils.randomAlphanumeric(30)).build()
    }


    override fun ask(request: LifePredictionRequest): Flow<LifePredictionResponse> {
        return flow {
            var word = RandomStringUtils.randomNumeric(5)
            while (!word.endsWith("90")) {
                emit(LifePredictionResponse.newBuilder().setText(word).build())
                word = RandomStringUtils.randomNumeric(5)
            }
        }
    }
}
