package com.pentakill.ssok.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private static final String SECRET = "jdbc:postgresql://secret-db:5432";

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @ParameterizedTest
    @CsvSource({
            "/bad-request, 400, BAD_REQUEST",
            "/not-found, 404, NOT_FOUND",
            "/conflict, 409, CONFLICT",
            "/data-conflict, 409, CONFLICT",
            "/unexpected, 500, INTERNAL_SERVER_ERROR",
            "/missing-header, 400, BAD_REQUEST"
    })
    @DisplayName("예외 종류에 맞는 응답 코드와 양식으로 내보낸다")
    void respondsWithMatchingStatus(String path, int status, String code) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    @DisplayName("예상하지 못한 예외의 내부 정보는 응답에 넣지 않는다")
    void hidesUnexpectedExceptionMessage() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }

    @RestController
    static class TestController {

        @GetMapping("/bad-request")
        void badRequest() {
            throw new IllegalArgumentException("잘못된 값");
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new EntityNotFoundException("없음");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new IllegalStateException("상태 충돌");
        }

        @GetMapping("/data-conflict")
        void dataConflict() {
            throw new DataIntegrityViolationException("제약 위반");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new RuntimeException(SECRET);
        }

        @GetMapping("/missing-header")
        void missingHeader(@RequestHeader("X-Member-Id") Long memberId) {
        }
    }
}
