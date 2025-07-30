package com.marlow.configuration

import com.sksamuel.cohort.Cohort
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureMonitoring() {
//    val openTelemetry = getOpenTelemetry(serviceName = "opentelemetry-ktor-marlow-simulation")

//    install(KtorServerTelemetry) {
////        setOpenTelemetry(openTelemetry)
//
//        capturedRequestHeaders(HttpHeaders.UserAgent)
//
//        spanKindExtractor {
//            if (httpMethod == HttpMethod.Post) {
//                SpanKind.PRODUCER
//            } else {
//                SpanKind.CLIENT
//            }
//        }
//
//        attributesExtractor {
//            onStart {
//                attributes.put("start-time", System.currentTimeMillis())
//            }
//            onEnd {
//                attributes.put("end-time", System.currentTimeMillis())
//            }
//        }
//    }
//    install(DropwizardMetrics) {
//        Slf4jReporter.forRegistry(registry)
//            .outputTo(this@configureMonitoring.log)
//            .convertRatesTo(TimeUnit.SECONDS)
//            .convertDurationsTo(TimeUnit.MILLISECONDS)
//            .build()
//            .start(10, TimeUnit.SECONDS)
//    }

//    val healthchecks = HealthCheckRegistry(Dispatchers.Default) {
//        register(FreememHealthCheck.mb(250), 10.seconds, 10.seconds)
//        register(ProcessCpuHealthCheck(0.8), 10.seconds, 10.seconds)
//    }

    install(Cohort) {

        // enable an endpoint to display operating system name and version
        operatingSystem = true

        // enable runtime JVM information such as vm options and vendor name
        jvmInfo = true

        // show current system properties
        sysprops = true

        // enable an endpoint to dump the heap in hprof format
        heapDump = true

        // enable an endpoint to dump threads
        threadDump = true

        // set to true to return the detailed status of the healthcheck response
        verboseHealthCheckResponse = true

        // enable healthchecks for kubernetes
//        healthcheck("/health", healthchecks)
    }
    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
    install(CallLogging) {
        callIdMdc("call-id")
    }
    routing {
        get("/hello") {
            call.respondText("Hello World!")
        }

        post("/post") {
            val postData = call.receiveText()
            call.respondText("Received: $postData")
        }
    }
}