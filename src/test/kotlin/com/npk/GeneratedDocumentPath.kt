package com.npk

import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.support.AnnotationSupport
import org.junit.platform.commons.util.ReflectionUtils
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(GeneratedDocumentPath.GeneratedDocPathExtension::class)
internal annotation class GeneratedDocumentPath(val value: String = "target/generated-doc") {

    class GeneratedDocPathExtension : BeforeEachCallback {

        override fun beforeEach(context: ExtensionContext) {
            context.requiredTestInstances.allInstances.forEach { instance ->
                AnnotationSupport.findAnnotatedFields(instance.javaClass, GeneratedDocumentPath::class.java).forEach { field ->
                    require(field.type == Path::class.java) { "The field '${field.name}' should be 'java.nio.file.Path' type" }
                    ReflectionUtils
                        .makeAccessible(field)
                        .set(instance, getGeneratedDocPath(field.getAnnotation(GeneratedDocumentPath::class.java).value))
                }
            }
        }

        private fun getGeneratedDocPath(value: String): Path = Paths.get(value)
            .also { path ->
                if (Files.notExists(path)) {
                    Files.createDirectory(path)
                }
            }

    }

}

fun Path.generatePdfDocument(documentFileName: String, block: (OutputStream) -> Unit): Unit =
    this.resolve(documentFileName).let { documentPath ->
        Files.deleteIfExists(documentPath)
        Files.newOutputStream(documentPath, CREATE).use(block)
    }
