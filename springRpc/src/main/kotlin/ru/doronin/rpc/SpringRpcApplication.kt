package ru.doronin.rpc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringRpcApplication

fun main(args: Array<String>) {
	runApplication<SpringRpcApplication>(*args)
}
