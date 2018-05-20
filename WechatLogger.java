import com.fasterxml.jackson.annotation.JsonInclude;
import com.mtbot.service.service.RedisService;
import com.mtbot.service.tool.JsonMapper;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public final class WechatLogger {

    private final Logger logger = LoggerFactory.getLogger(WechatLogger.class);

    private static final String APPID = "wx6105041ddf56b9fd";
    private static final String APPSECRET = "85c07a0816d69407d194f105a6d6a03d";

    private static final String TEMPLATE_ID = "3HP5qQw2XvVPVK8kPwEsSRxEDYVdWaY2o7Y_wA-bKSw";

    private static final String NOTICE_TEMPLATE_ID = "lf9ZWwKOxBMdZoWIVC3ZqOvYfYgv9EGFviulSzVpT0o";

    private final long TTL = 1000 * 60 * 60 * 72;
    private final Clock clock = Clock.system(ZoneId.of("Asia/Shanghai"));
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private RedisService redisService;

    @Autowired
    private Http http;

    public void sendMessage(String key, String msg) {
        String access_token = getAccessToken();
        String api = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=".concat(access_token);
        String link = "https://api.io/log/".concat(key);
        List<String> users = getUserList(access_token);

        if (users != null && users.size() > 0) {
            for (String openid : users) {
                WXTemplateData data = new WXTemplateData();
                data.time = new WXTemplateInfo(LocalDateTime.now(clock).format(formatter));
                data.log = new WXTemplateInfo(msg, "#ff00ff");

                Map map = new HashMap(8);
                map.put("touser", openid);
                map.put("template_id", TEMPLATE_ID);
                map.put("url", link);
                map.put("data", data);

                http.post(api, JsonMapper.toJson(map));
            }
        }
    }

    public void sendNotice(String user, String msg) {
        String access_token = getAccessToken();
        String api = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=".concat(access_token);

        if (user == null || user.length() == 0) {
            List<String> users = getUserList(access_token);

            if (users != null && users.size() > 0) {
                for (String openid : users) {
                    WXNoticeTemplate template = new WXNoticeTemplate();
                    template.template_id = NOTICE_TEMPLATE_ID;
                    template.touser = openid;
                    template.put("time", new WXTemplateInfo(LocalDateTime.now(clock).format(formatter)));
                    template.put("msg", new WXTemplateInfo(msg, "#ff00ff"));

                    logger.info("notice: {}", JsonMapper.toJson(template));
                    http.post(api, JsonMapper.toJson(template));
                }
            }
        } else {
            WXNoticeTemplate template = new WXNoticeTemplate();
            template.template_id = NOTICE_TEMPLATE_ID;
            template.touser = user;
            template.put("msg", new WXTemplateInfo(msg, "#ff00ff"));
            template.put("time", new WXTemplateInfo(LocalDateTime.now(clock).format(formatter)));

            logger.info("notice: {}", JsonMapper.toJson(template));
            http.post(api, JsonMapper.toJson(template));
        }
    }

    /*public void sendToUser(String openid, String msg) {
        String link = "https://api.io/log/";

        WXTemplateData data = new WXTemplateData();
        data.time = new WXTemplateInfo(LocalDateTime.now(clock).format(formatter));
        data.log = new WXTemplateInfo("带头大哥", "#ff00ff");

        Map map = new HashMap();
        map.put("touser", openid);
        map.put("template_id", TEMPLATE_ID);
        map.put("url", link);
        map.put("data", data);

        String access_token = getAccessToken();
        String api = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + access_token;
        http.post(api, JsonMapper.toJson(map));
    }*/

    private List<String> getUserList(String access_token) {
        List list = JsonMapper.fromJson(redisService.getString("wechat_log_users"), List.class);

        if (list == null || list.size() == 0) {
            String url = String.format("https://api.weixin.qq.com/cgi-bin/user/get?access_token=%s", access_token);
            String response = http.get(url);
            Map map = JsonMapper.fromJson(response, Map.class);
            if (map != null) {
                Object data = map.get("data");
                logger.info("getUserList {}", data);
                if (data != null) {
                    list = (List) ((Map) data).get("openid");

                    if (list != null)
                        redisService.set("wechat_log_users", list, TTL);
                }
            }
        }

        return list;
    }

    private String getAccessToken() {
        String access_token = redisService.getString("wechat_log_access_token");

        if (access_token == null) {
            String url = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s", APPID, APPSECRET);
            String result = http.get(url);
            Map map = JsonMapper.fromJson(result, Map.class);

            if (map != null) {
                access_token = (String) map.get("access_token");

                if (access_token != null)
                    redisService.setString("wechat_log_access_token", access_token, 1000 * 60 * 90);
            }
        }

        return access_token;
    }

    @Data
    private static class WXTemplateData {
        private WXTemplateInfo time;
        private WXTemplateInfo log;

        WXTemplateData() {
        }

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WXNoticeTemplate {
        public String touser;
        public String template_id;
        public String url;
        public Map<String, WXTemplateInfo> data = new HashMap<>(8);
        //private String topcolor = "#FF0000";

        public void put(String key, WXTemplateInfo value) {
            data.put(key, value);
        }

    }

    @Data
    private static class WXTemplateInfo {
        private String value;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String color;

        WXTemplateInfo() {
        }

        WXTemplateInfo(String value) {
            this.value = value;
        }

        WXTemplateInfo(String value, String color) {
            this.value = value;
            this.color = color;
        }

    }

}
