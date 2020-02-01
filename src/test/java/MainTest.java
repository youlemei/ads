import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;
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

}
