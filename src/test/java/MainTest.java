import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainTest {

    public static void main(String[] args) {
        Pattern pattern = Pattern.compile("\\{[a-zA-Z0-9]+\\}");
        String uri = "http://114.55.179.27:9999/adapi/interface/cgad/?cpid=61&adid=300&idfa={idfa}&ip={ip}&callbackurl={callbackurl}";
        Matcher matcher = pattern.matcher(uri);
        while (matcher.find()) {
            System.out.println(matcher.group());
        }

        Pattern param_pattern = Pattern.compile("^\\{[a-zA-Z0-9]+\\}$");
        UriComponents adUri = UriComponentsBuilder.fromHttpUrl(uri).build();
        adUri.getQueryParams().forEach((key, list) -> {
            if (list != null && list.size() == 1) {
                String value = list.get(0);
                System.out.println(key + " " + value + " " + param_pattern.matcher(value).matches());
            }
        });
    }

    @Test
    public void testMd5() throws Exception{
        System.out.println(DigestUtils.md5DigestAsHex("Aa123".getBytes()));
    }
    
    @Test
    public void testUrlParam() throws Exception{
        UriComponents url = UriComponentsBuilder.fromHttpUrl("http://localhost:9999/test")
                .queryParam("url1", "http://localhost/callback?clickId=1111&date=20200210")
                .queryParam("url2", URLEncoder.encode("http://localhost/callback?clickId=1111&date=20200210", "UTF-8"))
                .encode()
                .build();
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> resp = restTemplate.getForEntity(url.toUriString(), String.class);
        System.out.println(resp);
    }

    @Test
    public void testUrlDecode() throws Exception{
        System.out.println(URLDecoder.decode("http%3A%2F%2Flocalhost%2Fcallback%3FclickId%3D1111%26date%3D20200210", "UTF-8"));
    }

}
