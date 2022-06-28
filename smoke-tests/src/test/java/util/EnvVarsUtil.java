package util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnvVarsUtil {

    public static String replaceContainerUri(String uri, String protocol, String containerName) {

        String dockerContainerPath = protocol + "://" + containerName;

        if (uri.contains(dockerContainerPath)) {
            String regex = "(" + protocol + "://)(" + containerName + ")(:\\d+[A-Za-z/\\d@_-]*)";
            return uri.replaceAll(regex, "$1localhost$3");
        } else {
            return uri;
        }
    }

    public static String replaceContainerUriAndExtractHost(String uri, String protocol, String containerName) {

        String dockerContainerPath = protocol + "://" + containerName;

        if (uri.contains(dockerContainerPath)) {
            String regexWithContainer = "(" + protocol + "://)(" + containerName + ")(:\\d+)[A-Za-z/\\d@_-]*";
            return uri.replaceAll(regexWithContainer, "$1localhost$3");
        }

        String regexWithoutContainer = "("+ protocol +"://[A-z]*[:\\d+]*)[/@A-z\\d-_]*";

        Pattern pattern = Pattern.compile(regexWithoutContainer);
        Matcher matcher = pattern.matcher(uri);

        if (matcher.find()) {
            return uri.replaceAll(regexWithoutContainer, "$1");
        }

        return uri;

    }
}
