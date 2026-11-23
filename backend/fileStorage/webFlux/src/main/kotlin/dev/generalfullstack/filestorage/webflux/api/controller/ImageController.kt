package dev.generalfullstack.filestorage.webflux.api.controller

import dev.generalfullstack.filestorage.webflux.api.dto.CompleteImageUploadResponse
import dev.generalfullstack.filestorage.webflux.api.dto.CreateImageDownloadResponse
import dev.generalfullstack.filestorage.webflux.api.dto.CreateImageUploadRequest
import dev.generalfullstack.filestorage.webflux.api.dto.CreateImageUploadResponse
import dev.generalfullstack.filestorage.webflux.api.dto.ShareImageRequest
import dev.generalfullstack.filestorage.webflux.service.ImageStorageService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.security.Principal
import java.util.UUID

@RestController
@RequestMapping("/images")
class ImageController(
    private val imageStorageService: ImageStorageService,
) {
    @PostMapping("/uploads")
    fun createUpload(
        @Valid @RequestBody request: CreateImageUploadRequest,
        principal: Principal,
    ): Mono<CreateImageUploadResponse> = imageStorageService.createUpload(principal.name, request)

    @PostMapping("/{imageId}/complete")
    fun completeUpload(
        @PathVariable imageId: UUID,
        principal: Principal,
    ): Mono<CompleteImageUploadResponse> = imageStorageService.completeUpload(principal.name, imageId)

    @GetMapping("/{imageId}/download")
    fun createDownload(
        @PathVariable imageId: UUID,
        principal: Principal?,
    ): Mono<CreateImageDownloadResponse> = imageStorageService.createDownload(imageId, principal?.name)

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteImage(
        @PathVariable imageId: UUID,
        principal: Principal,
    ): Mono<Void> = imageStorageService.deleteImage(principal.name, imageId)

    @PostMapping("/{imageId}/viewers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun shareImage(
        @PathVariable imageId: UUID,
        @Valid @RequestBody request: ShareImageRequest,
        principal: Principal,
    ): Mono<Void> = imageStorageService.grantViewer(principal.name, imageId, request.recipientUserId)

    @DeleteMapping("/{imageId}/viewers/{recipientUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeShare(
        @PathVariable imageId: UUID,
        @PathVariable recipientUserId: UUID,
        principal: Principal,
    ): Mono<Void> = imageStorageService.revokeViewer(principal.name, imageId, recipientUserId)
}
