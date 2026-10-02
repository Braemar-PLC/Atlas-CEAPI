import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class LinuxRelaySmoke {
    public static void main(String[] args) throws Exception {
        IOException lastFailure = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try (var socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", 9002), 1000);
                socket.setSoTimeout(3000);
                socket.getOutputStream().write(
                        "GET / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                                .getBytes(StandardCharsets.US_ASCII));
                var response = new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine();
                if (response == null || !response.contains("404")) {
                    throw new IllegalStateException("Unexpected relay response: " + response);
                }
                System.out.println("PASS: CEAPI relay listening on 9002: " + response);
                return;
            } catch (IOException e) {
                lastFailure = e;
                Thread.sleep(1000);
            }
        }
        throw new IllegalStateException("CEAPI relay did not answer on port 9002", lastFailure);
    }
}
