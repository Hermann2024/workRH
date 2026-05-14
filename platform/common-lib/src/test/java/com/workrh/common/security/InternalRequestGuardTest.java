package com.workrh.common.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workrh.common.web.UnauthorizedException;
import org.junit.jupiter.api.Test;

class InternalRequestGuardTest {

    @Test
    void acceptsMatchingInternalKey() {
        assertThatCode(() -> InternalRequestGuard.requireValidKey("secret-value", "secret-value", "test key"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingConfiguredKey() {
        assertThatThrownBy(() -> InternalRequestGuard.requireValidKey("", "secret-value", "test key"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void rejectsMismatchedKey() {
        assertThatThrownBy(() -> InternalRequestGuard.requireValidKey("secret-value", "wrong", "test key"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid");
    }
}
