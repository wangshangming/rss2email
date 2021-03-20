import com.alibaba.fastjson.JSON;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import lombok.SneakyThrows;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.text.StringSubstitutor;
import org.xml.sax.InputSource;

import javax.mail.Session;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Application {

    static {
        JSON.defaultTimeZone = TimeZone.getTimeZone("Asia/Shanghai");
    }

    private static String username;
    private static String password;
    private static List<Read> reads;
    private static Set<String> readLinks;
    private static Session mailSession;

    @SneakyThrows
    private static <T> List<T> readArray(String path, Class<T> clazz) {
        String json = Files.readString(Paths.get(path));
        return JSON.parseArray(json, clazz);
    }

    @SneakyThrows
    public static void main(String[] args) {
        username = args[0];
        password = args[1];

        List<Feed> feeds = readArray("src/main/resources/feed.json", Feed.class);
        reads = readArray("src/main/resources/read.json", Read.class);
        readLinks = reads.stream().map(Read::getLink).collect(Collectors.toSet());
        feeds.forEach(Application::process);

        String readStr = JSON.toJSONString(reads, true);
        Files.writeString(Paths.get("src/main/resources/read.json"), readStr);
    }

    @SneakyThrows
    private static void process(Feed feed) {
        System.out.println(feed);
        URL feedURL = new URL(feed.getXmlUrl());
        SyndFeed syndFeed = new SyndFeedInput().build(new InputSource(feedURL.openStream()));
        String blogTitle = syndFeed.getTitle();
        String blogLink = syndFeed.getLink();
        syndFeed.getEntries()
                .stream()
                .filter(it -> !readLinks.contains(it.getLink()))
                .forEach(it -> {
                    try {
                        String postsTitle = it.getTitle();
                        String postsLink = it.getLink();
                        Map<String, Object> map = new HashMap<>();
                        map.put("blogTitle", blogTitle);
                        map.put("blogLink", blogLink);
                        map.put("postsTitle", postsTitle);
                        map.put("postsLink", postsLink);
                        String template = Files.readString(Paths.get("src/main/resources/template.html"));
                        String html = StringSubstitutor.replace(template, map);
                        sendEmail(html);
                        readLinks.add(postsLink);

                        Read read = new Read();
                        read.setTitle(postsTitle);
                        read.setLink(postsLink);
                        read.setTime(new Date());
                        reads.add(read);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @SneakyThrows
    private static void sendEmail(String html) {
        // Create the email message
        HtmlEmail email = new HtmlEmail();
        email.setCharset("utf-8");
        email.setHostName("smtp.qq.com");
        email.setAuthenticator(new DefaultAuthenticator(username, password));
        email.setSSLOnConnect(true);
        email.addTo(username);
        email.setFrom(username);
        email.setSubject("RSS订阅");
        // set the html message
        email.setHtmlMsg(html);
        if (mailSession == null) {
            mailSession = email.getMailSession();
        } else {
            email.setMailSession(mailSession);
        }
        // send the email
        email.send();
    }
}
