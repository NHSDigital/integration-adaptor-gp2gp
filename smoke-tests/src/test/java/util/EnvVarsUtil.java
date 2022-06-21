package util;

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
            String regex = "(" + protocol + "://)(" + containerName + ")(:\\d+)[A-Za-z/\\d@_-]*";
            return uri.replaceAll(regex, "$1localhost$3");
        } else {
            return uri;
        }
    }
}
