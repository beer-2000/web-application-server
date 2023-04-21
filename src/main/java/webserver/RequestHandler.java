package webserver;

import db.DataBase;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

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

            // .html 렌더링
            if (requestInformation.get("Path").contains(".html")) {
                System.out.println(">>>>>>>>>>> Request Informaion");
                System.out.println(requestInformation);
                openHtmlWithResponseStatus(out, requestInformation.get("Path"), ResponseStatus.OK);
                return;
            }

            // .css 렌더링
            if (requestInformation.get("Path").endsWith(".css")) {
                System.out.println(">>>>>>>>>>> Request Informaion");
                System.out.println(requestInformation);
                openCssWithResponseStatus(out, requestInformation.get("Path"), ResponseStatus.OK);
            }

            // POST /user/create
            if (requestInformation.get("Path").equals("/user/create")
                && requestInformation.get("Method").equals("POST")) {
                System.out.println(">>>>>>>>>>> Request Informaion");
                System.out.println(requestInformation);
                Map<String, String> requestBodyTable = getRequestBodyTableBy(requestInformation);
                User.save(new User(
                        requestBodyTable.get("userId"),
                        requestBodyTable.get("password"),
                        requestBodyTable.get("name"),
                        requestBodyTable.get("email")
                ));
                System.out.println(DataBase.findAll());
                openHtmlWithResponseStatus(out, "/index.html", ResponseStatus.REDIRECT);
            }

            // POST /user/login
            if (requestInformation.get("Path").equals("/user/login")
                    && requestInformation.get("Method").equals("POST")) {
                System.out.println(">>>>>>>>>>> Request Informaion");
                System.out.println(requestInformation);
                Map<String, String> requestBodyTable = getRequestBodyTableBy(requestInformation);
                System.out.println("request body: " + requestBodyTable);
                User user = DataBase.findUserById(requestBodyTable.get("userId"));
                if (user == null) {
                    openHtmlWithLoginCookie(out, "/user/login_failed.html", ResponseStatus.OK, "false");
                    return;
                }
                openHtmlWithLoginCookie(out, "/index.html", ResponseStatus.OK, "true");
            }

            // GET /user/list
            if (requestInformation.get("Path").equals("/user/list")
                    && requestInformation.get("Method").equals("GET")) {
                System.out.println(">>>>>>>>>>> Request Informaion");
                System.out.println(requestInformation);
                String cookieString = requestInformation.get("Cookie");
                Map<String, String> cookieMap = HttpRequestUtils.parseCookies(cookieString);
                System.out.println("cookieMap: " + cookieMap);
                if (Boolean.parseBoolean(cookieMap.get("logined"))) {
                    DataOutputStream dos = new DataOutputStream(out);
                    try {
                        Collection<User> users = DataBase.findAll();
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("<table border='1'>");
                        for (User user : users) {
                            stringBuilder.append("<tr>");
                            stringBuilder.append("<td>" + user.getUserId() + "</td>");
                            stringBuilder.append("<td>" + user.getName() + "</td>");
                            stringBuilder.append("<td>" + user.getEmail() + "</td>");
                            stringBuilder.append("</tr>");
                        }
                        stringBuilder.append("</table>");
                        byte[] body = stringBuilder.toString().getBytes();
                        openHtmlWithBody(out, "/user/list", ResponseStatus.OK, body);
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }

                }
                openHtmlWithResponseStatus(out, "/user/login", ResponseStatus.REDIRECT);
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private static Map<String, String> getRequestBodyTableBy(Map<String, String> requestInformation) {
        Map<String, String> requestBodyTable = new HashMap<>();
        for (String keyAndValue : requestInformation.get("Request-Body").split("&")) {
            requestBodyTable.put(keyAndValue.split("=")[0], keyAndValue.split("=")[1]);
        }
        return requestBodyTable;
    }

    private void openHtmlWithResponseStatus(
            final OutputStream out,
            final String path,
            final ResponseStatus responseStatus) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());
        try {
            System.out.println("Response: " + String.format("HTTP/1.1 %d %s \r\n", responseStatus.getCode(), responseStatus.getMessage()));
            dos.writeBytes(String.format("HTTP/1.1 %d %s \r\n", responseStatus.getCode(), responseStatus.getMessage()));
            if (responseStatus.equals(ResponseStatus.REDIRECT)) {
                dos.writeBytes(String.format("Location: %s", path));
                return;
            }
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + body.length + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        responseBody(dos, body);
    }

    private void openCssWithResponseStatus(
            final OutputStream out,
            final String path,
            final ResponseStatus responseStatus) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());
        try {
            System.out.println("Response: " + String.format("HTTP/1.1 %d %s \r\n", responseStatus.getCode(), responseStatus.getMessage()));
            dos.writeBytes(String.format("HTTP/1.1 %d %s \r\n", responseStatus.getCode(), responseStatus.getMessage()));
            if (responseStatus.equals(ResponseStatus.REDIRECT)) {
                dos.writeBytes(String.format("Location: %s", path));
                return;
            }
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + body.length + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        responseBody(dos, body);
    }

    private void openHtmlWithLoginCookie(
            final OutputStream out,
            final String path,
            final ResponseStatus responseStatus,
            final String loginCookie) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        byte[] body = Files.readAllBytes(new File("./webapp" + path).toPath());
        try {
            System.out.println("Response: " + String.format("HTTP/1.1 %d %s \r\n", responseStatus.getCode(), responseStatus.getMessage()));
            dos.writeBytes(String.format("HTTP/1.1 %d %s \r\n", responseStatus.getCode(), responseStatus.getMessage()));
            if (responseStatus.equals(ResponseStatus.REDIRECT)) {
                dos.writeBytes(String.format("Location: %s", path));
                return;
            }
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes(String.format("Set-Cookie: logined=%s", loginCookie));
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        responseBody(dos, body);
    }

    private void openHtmlWithBody(
            final OutputStream out,
            final String path,
            final ResponseStatus responseStatus,
            final byte[] body) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        try {
            System.out.println("Response: " + String.format("HTTP/1.1 %d %s \r\n", responseStatus.getCode(), responseStatus.getMessage()));
            dos.writeBytes(String.format("HTTP/1.1 %d %s \r\n", responseStatus.getCode(), responseStatus.getMessage()));
            if (responseStatus.equals(ResponseStatus.REDIRECT)) {
                dos.writeBytes(String.format("Location: %s", path));
                return;
            }
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + body.length + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        responseBody(dos, body);
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
