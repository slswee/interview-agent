package com.sallyli

import io.ktor.server.application.*
import io.ktor.server.netty.*
import com.sallyli.plugins.configureSecurity
import com.sallyli.plugins.configureSerialization
import com.sallyli.plugins.configureRouting
import java.lang.System.getenv

fun main(args: Array<String>) {
    // Read the PORT environment variable, default to 8080 if not set
    val port = getenv("PORT")?.toIntOrNull() ?: 8080

    // Start the Ktor server using Netty, listening on the determined port.
    // The 'module' function will be automatically discovered by EngineMain.
    // We pass arguments to override configuration if needed, like the port.
    EngineMain.main(args + arrayOf("-port", port.toString()))
}

fun Application.module() {
    // These functions are extensions defined in other files (e.g., Security.kt, Routing.kt)
    // and should be visible if they are in the same package or imported correctly.
    configureSecurity()
    configureSerialization()
    configureRouting()
}
