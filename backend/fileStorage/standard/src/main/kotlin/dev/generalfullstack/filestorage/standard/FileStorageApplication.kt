package dev.generalfullstack.filestorage.standard

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FileStorageApplication

fun main(args: Array<String>) {
    runApplication<FileStorageApplication>(*args)
}
