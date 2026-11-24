package handoff.application.Service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FileSignatureTest {

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test
    void acceptsRealSignatures() {
        assertThat(FileSignature.matches("image/png", bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0))).isTrue();
        assertThat(FileSignature.matches("image/jpeg", bytes(0xFF, 0xD8, 0xFF, 0xE0))).isTrue();
        assertThat(FileSignature.matches("image/gif", ascii("GIF89a..."))).isTrue();
        assertThat(FileSignature.matches("image/webp", ascii("RIFF\0\0\0\0WEBPVP8 "))).isTrue();
        assertThat(FileSignature.matches("application/pdf", ascii("%PDF-1.7"))).isTrue();
        assertThat(FileSignature.matches("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                bytes('P', 'K', 3, 4, 20))).isTrue();
        assertThat(FileSignature.matches("text/csv", ascii("nome;email\nAna;ana@example.com"))).isTrue();
    }

    @Test
    void rejectsContentThatDoesNotMatchTheType() {
        byte[] html = ascii("<html><script>alert(1)</script></html>");
        assertThat(FileSignature.matches("image/png", html)).isFalse();
        assertThat(FileSignature.matches("application/pdf", html)).isFalse();
        assertThat(FileSignature.matches("application/zip", html)).isFalse();
        assertThat(FileSignature.matches("image/webp", ascii("RIFF\0\0\0\0WAVEfmt "))).isFalse();
        assertThat(FileSignature.matches("text/plain", bytes('M', 'Z', 0, 0))).isFalse();
        assertThat(FileSignature.matches("image/jpeg", new byte[0])).isFalse();
        assertThat(FileSignature.matches("application/x-msdownload", bytes('M', 'Z'))).isFalse();
    }
}
