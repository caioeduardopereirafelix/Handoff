package handoff.application.Service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceSlugTest {

    @Test
    void slugifyRemovesAccentsAndSymbols() {
        assertThat(AuthService.slugify("  Estúdio Ação & Cia.  ")).isEqualTo("estudio-acao-cia");
    }

    @Test
    void slugifyFallsBackWhenNothingIsLeft() {
        assertThat(AuthService.slugify("!!!")).isEqualTo("org");
    }

    @Test
    void slugifyLimitsLength() {
        assertThat(AuthService.slugify("a".repeat(100))).hasSize(60);
    }
}
