# Image storage with Spring WebFlux

This service implements the same upload, download, delete, and viewer-sharing routes as the [Spring MVC version](../standard/README.md). Both use presigned URLs for browser-to-S3 transfers. The API itself reads the staged image during upload completion to validate it before publishing.

## Run it locally

Follow the MVC README to start the support services and configure OpenFGA and the JWT issuer. The modules use the same OpenFGA model and default port (`8083`). Stop the MVC app before starting WebFlux on that port.

```powershell
.\gradlew.bat -p backend/fileStorage/webFlux bootRun
.\gradlew.bat -p backend/fileStorage/webFlux test
```

## Compare the two implementations

| Area | Spring MVC | Spring WebFlux |
| --- | --- | --- |
| HTTP handling | The servlet request thread waits for the controller to return a result. | The controller returns a `Mono`, and Spring subscribes and writes the response when the result is ready. Each endpoint has at most one result, so `Mono` is sufficient. |
| Storage and permissions | Synchronous `S3Client` calls and `CompletableFuture.get()` for OpenFGA. | `S3AsyncClient` and OpenFGA futures are adapted with `Mono.fromFuture { ... }`. The supplier starts the operation at subscription time. |
| Ordering and errors | A statement finishes before the next one runs. An exception exits the method. | Use `flatMap` when the next async step needs the current value and `then` when it only needs successful completion. An error skips subsequent steps and can be handled by `@RestControllerAdvice`. |
| Authentication | Servlet `SecurityFilterChain` and `HttpSecurity`. | Reactive `SecurityWebFilterChain` and `ServerHttpSecurity`. Both expose the validated principal to the controller. |

Start with `ImageStorageService.completeUpload`. `requireRelation(...).then(headObject(key))` starts the S3 metadata request only after authorization succeeds. The first `flatMap` uses the metadata to validate the staged upload and build a conditional S3 read. A ranged GET caps the response at the declared size plus one byte so the service can detect an oversized object. If S3 reports that the object changed between `HEAD` and `GET`, the API returns `409 Conflict` and skips publishing.

`Mono.fromCallable` defers synchronous work. With `subscribeOn`, image decoding runs on a dedicated pool with two workers and eight queued tasks per worker. ImageIO uses an in-memory input stream. A full decode queue returns `503 Service Unavailable`. S3 call setup and URL signing use Reactor's shared bounded-elastic scheduler because AWS may do synchronous work before returning a future. Bucket setup waits for S3 on the application startup thread. S3 attempts time out after 10 seconds, with a 30-second limit for the full call including retries.

WebFlux can serve other requests while waiting for S3 or OpenFGA. Image decoding still takes CPU and memory, and upload completion still spans multiple operations. If publishing succeeds but staging cleanup fails, the app has no transaction that rolls back the publish. The same workflow limitation exists in MVC.
