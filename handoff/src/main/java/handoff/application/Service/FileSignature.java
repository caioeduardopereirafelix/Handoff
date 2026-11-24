package handoff.application.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

final class FileSignature {

    static final int BYTES_TO_READ = 512;

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF87 = ascii("GIF87a");
    private static final byte[] GIF89 = ascii("GIF89a");
    private static final byte[] RIFF = ascii("RIFF");
    private static final byte[] WEBP = ascii("WEBP");
    private static final byte[] PDF = ascii("%PDF-");
    private static final byte[] ZIP = {'P', 'K', 0x03, 0x04};
    private static final byte[] EMPTY_ZIP = {'P', 'K', 0x05, 0x06};

    private FileSignature() {
    }

    static boolean matches(String contentType, byte[] head) {
        return switch (contentType) {
            case "image/png" -> startsWith(head, 0, PNG);
            case "image/jpeg" -> startsWith(head, 0, JPEG);
            case "image/gif" -> startsWith(head, 0, GIF87) || startsWith(head, 0, GIF89);
            case "image/webp" -> startsWith(head, 0, RIFF) && startsWith(head, 8, WEBP);
            case "application/pdf" -> startsWith(head, 0, PDF);
            case "application/zip",
                 "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                 "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                 "application/vnd.openxmlformats-officedocument.presentationml.presentation" ->
                    startsWith(head, 0, ZIP) || startsWith(head, 0, EMPTY_ZIP);
            case "text/plain", "text/csv" -> looksLikeText(head);
            default -> false;
        };
    }

    private static boolean looksLikeText(byte[] head) {
        for (byte b : head) {
            if (b == 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
        return data.length >= offset + prefix.length
                && Arrays.equals(data, offset, offset + prefix.length, prefix, 0, prefix.length);
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }
}
