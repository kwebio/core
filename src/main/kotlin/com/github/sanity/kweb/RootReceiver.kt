package com.github.sanity.kweb

import com.github.sanity.kweb.dom.Document
import com.github.sanity.kweb.plugins.KWebPlugin
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import org.jetbrains.ktor.util.ValuesMap
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

class RootReceiver(private val clientId: String, val httpRequestInfo: HttpRequestInfo, internal val cc: Kweb, val response: String? = null) {
    private val plugins: Map<KClass<out KWebPlugin>, KWebPlugin> by lazy {
        cc.appliedPlugins.map { it::class to it }.toMap()
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <P : KWebPlugin> plugin(plugin : KClass<out P>) : P {
        return (plugins[plugin] ?: throw RuntimeException("Plugin $plugin is missing")) as P
    }

    internal fun require(vararg requiredPlugins: KClass<out KWebPlugin>) {
        val missing = java.util.HashSet<String>()
        for (requiredPlugin in requiredPlugins) {
            if (!plugins.contains(requiredPlugin)) missing.add(requiredPlugin.simpleName ?: requiredPlugin.jvmName)
        }
        if (missing.isNotEmpty()) {
            throw RuntimeException("Plugin(s) ${missing.joinToString(separator = ", ")} required but not passed to Kweb constructor")
        }
    }

    fun execute(js: String) = async(CommonPool){
        cc.execute(clientId, js)
    }

    fun executeWithCallback(js: String, callbackId: Int, callback: (String) -> Unit) = async(CommonPool){
        cc.executeWithCallback(clientId, js, callbackId, callback)
    }

    fun evaluate(js: String): CompletableFuture<String> {
        val cf = CompletableFuture<String>()
        evaluateWithCallback(js) {
            cf.complete(response)
            false
        }
        return cf
    }


    fun evaluateWithCallback(js: String, rh: RootReceiver.() -> Boolean) = async(CommonPool){
        cc.evaluate(clientId, js, { rh.invoke(RootReceiver(clientId, httpRequestInfo, cc, it)) })
    }

    val doc = Document(this)
}

// TODO: Not sure if this should be a separate property of RootReceiver, or passed in
// TODO: some other way.
data class HttpRequestInfo(val visitedUrl : String, val headers : ValuesMap) {
    val referer : String? get() = headers["Referer"]
}