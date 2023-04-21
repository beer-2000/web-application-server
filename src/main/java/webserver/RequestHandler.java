package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            Map<String, String> requestInformation = HttpRequestUtils.getRequestInformation(in);

            if (requestInformation.get("Path").contains(".html")) {
                System.out.println(">>>>>>>>>>> Request Informaion");
                System.out.println(requestInformation);
                openHtml(out, requestInformation.get("Path"));
                return;
            }

            if (requestInformation.get("Path").equals("/user/create")
                && requestInformation.get("Method").equals("POST")) {
                System.out.println(">>>>>>>>>>> Request Informaion");
                System.out.println(requestInformation);
                Map<String, String> requestBodyTable = new HashMap<>();
                for (String keyAndValue : requestInformation.get("Request-Body").split("&")) {
                    requestBodyTable.put(keyAndValue.split("=")[0], keyAndValue.split("=")[1]);
                }
                User.save(new User(
                        requestBodyTable.get("userId"),
                        requestBodyTable.get("password"),
                        requestBodyTable.get("name"),
                        requestBodyTable.get("email")
                ));
                User.readConsole();
                openHtml(out, "/index.html");
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void openHtml(OutputStream out, String path) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());
        response200Header(dos, body.length);
        responseBody(dos, body);
    }


    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
