import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.google.protobuf.gradle.*;

val grpcVersion = "1.39.0"
val protobufVersion = "3.18.1"
val kotlinVersion = "1.5.31"

buildscript {
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.15")
    }
}

plugins {
	id("org.springframework.boot") version "2.5.6"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.google.protobuf") version "0.8.15"
    id ("idea")
	kotlin("jvm") version "1.5.31"
	kotlin("plugin.spring") version "1.5.31"
}

group = "ru.doronin"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

extra["springBootAdminVersion"] = "2.4.3"

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.springframework.boot:spring-boot-starter-quartz")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("de.codecentric:spring-boot-admin-starter-server")
	implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("io.grpc:grpc-protobuf:${grpcVersion}")
    implementation("io.grpc:grpc-stub:${grpcVersion}")
    implementation("io.grpc:grpc-netty:${grpcVersion}")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    api("com.google.protobuf:protobuf-java-util:${protobufVersion}")
    implementation("io.grpc:grpc-all:${grpcVersion}")
    api("io.grpc:grpc-kotlin-stub:0.2.1")
    implementation("io.grpc:protoc-gen-grpc-kotlin:0.1.5")
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.8.15")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
	imports {
		mavenBom("de.codecentric:spring-boot-admin-dependencies:${property("springBootAdminVersion")}")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

protobuf {
    protoc{
        artifact = "com.google.protobuf:protoc:${protobufVersion}"
    }
    //generatedFilesBaseDir = "$projectDir/src/generated"
    plugins {
        id("grpc"){
            artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:0.1.5"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

sourceSets["main"].java.srcDir(File(buildDir, "generated/source"))
idea {
    module {
        // Marks the already(!) added srcDir as "generated"
        generatedSourceDirs.plusAssign(File("build/generated/source"))
    }
}
