package dev.generalfullstack.auth.api.controller

import dev.generalfullstack.auth.api.dto.CsrfTokenResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class CsrfController {
    @GetMapping("/csrf")
    fun csrfToken(
        @RequestAttribute("_csrf") csrfToken: CsrfToken,
        request: HttpServletRequest,
    ): CsrfTokenResponse {
        request.session
        return CsrfTokenResponse(
            headerName = csrfToken.headerName,
            token = csrfToken.token,
        )
    }
}
