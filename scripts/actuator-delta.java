///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.2

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

class actuator_delta {

    private static final ObjectMapper JSON = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    private static final DateTimeFormatter TS_FILE_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT).withZone(ZoneOffset.UTC);

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        Map<String, String> options = parseOptions(args, 1);

        String scenario = required(options, "--scenario");
        String mode = required(options, "--mode");
        String baseUrlRaw = required(options, "--base-url");
        String actuatorBasePath = options.get("--actuator-base-path");

        URI baseUrl = URI.create(baseUrlRaw);
        String resultsDirRaw = options.getOrDefault("--results-dir", "test-plans/manual-results");
        Path resultsDir = Paths.get(resultsDirRaw);

        Files.createDirectories(resultsDir);

        Path scenarioDir = resultsDir.resolve(sanitizePathPart(scenario)).resolve(sanitizePathPart(mode));
        Path workDir = resultsDir.resolve(".work");
        Files.createDirectories(scenarioDir);
        Files.createDirectories(workDir);

        String runKey = sanitizePathPart(scenario)
            + "__" + sanitizePathPart(mode)
            + "__" + sanitizePathPart(baseUrl.getHost() + "-" + resolvePort(baseUrl));
        Path workFile = workDir.resolve(runKey + ".json");

        if ("start".equalsIgnoreCase(command)) {
            doStart(baseUrl, scenario, mode, actuatorBasePath, scenarioDir, workFile);
            return;
        }

        if ("finish".equalsIgnoreCase(command)) {
            doFinish(baseUrl, scenario, mode, actuatorBasePath, scenarioDir, workFile, resultsDir);
            return;
        }

        throw new IllegalArgumentException("Unknown command: " + command);
    }

    private static void doStart(
        URI baseUrl,
        String scenario,
        String mode,
        String actuatorBasePath,
        Path scenarioDir,
        Path workFile
    ) throws Exception {
        Snapshot before = fetchSnapshot(baseUrl, scenario, mode, actuatorBasePath);
        String stamp = TS_FILE_FORMAT.format(before.capturedAt());

        Path beforeFile = scenarioDir.resolve("before-" + stamp + ".json");
        JSON.writeValue(beforeFile.toFile(), before.payload());

        ObjectNode session = JSON.createObjectNode();
        session.put("scenario", scenario);
        session.put("mode", mode);
        session.put("baseUrl", baseUrl.toString());
        session.put("beforeFile", beforeFile.toString().replace('\\', '/'));
        session.set("beforeSnapshot", before.payload());
        JSON.writeValue(workFile.toFile(), session);

        System.out.println("Captured BEFORE snapshot: " + beforeFile.toString().replace('\\', '/'));
        System.out.println("Run the scenario now, then execute the finish command.");
    }

    private static void doFinish(
        URI baseUrl,
        String scenario,
        String mode,
        String actuatorBasePath,
        Path scenarioDir,
        Path workFile,
        Path resultsDir
    ) throws Exception {
        if (!Files.exists(workFile)) {
            throw new IllegalStateException(
                "No start snapshot found for this scenario/mode/base-url. Run the start command first."
            );
        }

        JsonNode session = JSON.readTree(workFile.toFile());
        JsonNode beforeNode = session.path("beforeSnapshot");
        if (beforeNode.isMissingNode() || beforeNode.isNull()) {
            throw new IllegalStateException("Invalid start snapshot session file: " + workFile);
        }

        Snapshot after = fetchSnapshot(baseUrl, scenario, mode, actuatorBasePath);
        String afterStamp = TS_FILE_FORMAT.format(after.capturedAt());
        Path afterFile = scenarioDir.resolve("after-" + afterStamp + ".json");
        JSON.writeValue(afterFile.toFile(), after.payload());

        ObjectNode delta = buildDelta(beforeNode, after.payload());
        delta.put("scenario", scenario);
        delta.put("mode", mode);
        delta.put("baseUrl", baseUrl.toString());
        delta.put("beforeFile", session.path("beforeFile").asText());
        delta.put("afterFile", afterFile.toString().replace('\\', '/'));

        Path deltaJson = scenarioDir.resolve("delta-" + afterStamp + ".json");
        JSON.writeValue(deltaJson.toFile(), delta);

        Path deltaCsv = scenarioDir.resolve("delta-" + afterStamp + ".csv");
        writeCsv(deltaCsv, delta, true);

        Path aggregateCsv = resultsDir.resolve("metrics-deltas.csv");
        writeCsv(aggregateCsv, delta, !Files.exists(aggregateCsv));

        Files.deleteIfExists(workFile);

        System.out.println("Captured AFTER snapshot: " + afterFile.toString().replace('\\', '/'));
        System.out.println("Wrote delta JSON: " + deltaJson.toString().replace('\\', '/'));
        System.out.println("Wrote delta CSV: " + deltaCsv.toString().replace('\\', '/'));
        System.out.println("Updated aggregate CSV: " + aggregateCsv.toString().replace('\\', '/'));
    }

    private static Snapshot fetchSnapshot(URI baseUrl, String scenario, String mode) throws Exception {
        return fetchSnapshot(baseUrl, scenario, mode, null);
    }

    private static Snapshot fetchSnapshot(
        URI baseUrl,
        String scenario,
        String mode,
        String actuatorBasePath
    ) throws Exception {
        Instant capturedAt = Instant.now();
        EndpointPair endpointPair = resolveActuatorEndpoints(baseUrl, actuatorBasePath);
        JsonNode metrics = fetchHttpServerRequestsMetric(
            endpointPair.metricsUri(),
            endpointPair.metricsCollectionUri()
        );
        JsonNode exchanges = fetchHttpExchangesOrEmpty(endpointPair.httpExchangesUri());

        ObjectNode payload = JSON.createObjectNode();
        payload.put("capturedAt", capturedAt.toString());
        payload.put("scenario", scenario);
        payload.put("mode", mode);
        payload.put("baseUrl", baseUrl.toString());
        payload.put("metricsUri", endpointPair.metricsUri().toString());
        payload.put("httpexchangesUri", endpointPair.httpExchangesUri().toString());
        payload.set("metrics", metrics);
        payload.set("metricsBreakdown", fetchMetricsBreakdown(endpointPair.metricsUri(), metrics));
        payload.set("httpexchanges", exchanges);

        return new Snapshot(capturedAt, payload);
    }

    private static JsonNode fetchJson(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Request failed for " + uri + " with status " + response.statusCode());
        }

        return JSON.readTree(response.body());
    }

    private static EndpointPair resolveActuatorEndpoints(URI baseUrl, String actuatorBasePath) throws Exception {
        List<String> basePathCandidates = new ArrayList<>();
        if (actuatorBasePath != null && !actuatorBasePath.isBlank()) {
            basePathCandidates.add(normalizeBasePath(actuatorBasePath));
        }
        basePathCandidates.add("/actuator");
        basePathCandidates.add("/manage");
        basePathCandidates.add("/management");
        basePathCandidates.add("");

        List<String> tried = new ArrayList<>();

        for (String basePath : basePathCandidates) {
            URI indexUri = buildActuatorUri(baseUrl, basePath, "");
            Optional<JsonNode> index = fetchJsonIfSuccessful(indexUri);
            if (index.isPresent()) {
                EndpointPair fromLinks = resolveFromActuatorLinks(baseUrl, index.get());
                if (fromLinks != null) {
                    return fromLinks;
                }
            }

            URI metricsUri = buildActuatorUri(baseUrl, basePath, "metrics/http.server.requests");
            URI exchangesUri = buildActuatorUri(baseUrl, basePath, "httpexchanges");

            tried.add(metricsUri.toString());
            tried.add(exchangesUri.toString());

            Optional<JsonNode> metrics = fetchJsonIfSuccessful(metricsUri);
            if (metrics.isPresent()) {
                URI metricsCollectionUri = buildActuatorUri(baseUrl, basePath, "metrics");
                return new EndpointPair(metricsUri, metricsCollectionUri, exchangesUri);
            }
        }

        throw new IOException(
            "Could not find Actuator metrics/httpexchanges endpoints for base URL " + baseUrl
                + ". Tried: " + String.join(", ", tried)
                + ". Verify the intended service is running on this port and that Actuator endpoints are exposed."
        );
    }

    private static EndpointPair resolveFromActuatorLinks(URI baseUrl, JsonNode actuatorIndex) {
        JsonNode links = actuatorIndex.path("_links");
        if (!links.isObject()) {
            return null;
        }

        String metricsHref = links.path("metrics").path("href").asText("");
        if (metricsHref.isBlank()) {
            return null;
        }

        URI metricsUri = resolveLinkedUri(baseUrl, metricsHref + "/http.server.requests");
        URI metricsCollectionUri = resolveLinkedUri(baseUrl, metricsHref);

        String exchangesHref = links.path("httpexchanges").path("href").asText("");
        URI exchangesUri = exchangesHref.isBlank()
            ? resolveLinkedUri(baseUrl, "/actuator/httpexchanges")
            : resolveLinkedUri(baseUrl, exchangesHref);

        return new EndpointPair(metricsUri, metricsCollectionUri, exchangesUri);
    }

    private static JsonNode fetchHttpServerRequestsMetric(URI metricUri, URI metricsCollectionUri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(metricUri)
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return JSON.readTree(response.body());
        }

        if (response.statusCode() == 404 && isHttpServerRequestsMissing(metricsCollectionUri)) {
            ObjectNode synthetic = JSON.createObjectNode();
            synthetic.put("name", "http.server.requests");
            ArrayNode measurements = synthetic.putArray("measurements");
            ObjectNode count = measurements.addObject();
            count.put("statistic", "COUNT");
            count.put("value", 0.0);
            synthetic.putArray("availableTags");
            return synthetic;
        }

        throw new IOException("Request failed for " + metricUri + " with status " + response.statusCode());
    }

    private static boolean isHttpServerRequestsMissing(URI metricsCollectionUri) {
        try {
            JsonNode metricsCollection = fetchJson(metricsCollectionUri);
            JsonNode names = metricsCollection.path("names");
            if (!names.isArray()) {
                return true;
            }

            for (JsonNode name : names) {
                if ("http.server.requests".equals(name.asText(""))) {
                    return false;
                }
            }

            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static JsonNode fetchHttpExchangesOrEmpty(URI exchangesUri) {
        Optional<JsonNode> response = fetchJsonIfSuccessful(exchangesUri);
        if (response.isPresent()) {
            return response.get();
        }

        ObjectNode fallback = JSON.createObjectNode();
        fallback.putArray("exchanges");
        return fallback;
    }

    private static URI resolveLinkedUri(URI baseUrl, String href) {
        if (href == null || href.isBlank()) {
            return baseUrl;
        }
        return baseUrl.resolve(href);
    }

    private static Optional<JsonNode> fetchJsonIfSuccessful(URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            return Optional.of(JSON.readTree(response.body()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static String normalizeBasePath(String basePath) {
        String trimmed = basePath == null ? "" : basePath.trim();
        if (trimmed.isEmpty() || "/".equals(trimmed)) {
            return "";
        }
        String withLeadingSlash = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return withLeadingSlash.endsWith("/") ? withLeadingSlash.substring(0, withLeadingSlash.length() - 1) : withLeadingSlash;
    }

    private static URI buildActuatorUri(URI baseUrl, String basePath, String tail) {
        String normalizedBasePath = normalizeBasePath(basePath);
        String normalizedTail = tail == null ? "" : tail.trim();

        StringBuilder path = new StringBuilder();
        path.append(normalizedBasePath);
        if (!normalizedTail.isEmpty()) {
            if (path.isEmpty() || path.charAt(path.length() - 1) != '/') {
                path.append('/');
            }
            path.append(normalizedTail.startsWith("/") ? normalizedTail.substring(1) : normalizedTail);
        }

        if (path.isEmpty()) {
            path.append('/');
        }

        return baseUrl.resolve(path.toString());
    }

    private static ObjectNode buildDelta(JsonNode before, JsonNode after) {
        double beforeCount = extractMetricCount(before.path("metrics"));
        double afterCount = extractMetricCount(after.path("metrics"));

        List<JsonNode> beforeExchanges = asList(before.path("httpexchanges").path("exchanges"));
        List<JsonNode> afterExchanges = asList(after.path("httpexchanges").path("exchanges"));

        List<JsonNode> newExchanges = subtractAsMultiset(beforeExchanges, afterExchanges);

        int status2xx = 0;
        int status3xx = 0;
        int status4xx = 0;
        int status5xx = 0;
        Map<String, Integer> operationCounts = new LinkedHashMap<>();
        String distributionSource = "httpexchanges";

        for (JsonNode exchange : newExchanges) {
            int status = parseInt(exchange.path("response").path("status").asText("0"));
            if (status >= 200 && status < 300) {
                status2xx++;
            } else if (status >= 300 && status < 400) {
                status3xx++;
            } else if (status >= 400 && status < 500) {
                status4xx++;
            } else if (status >= 500 && status < 600) {
                status5xx++;
            }

            String method = exchange.path("request").path("method").asText("UNKNOWN");
            String uri = normalizeUri(exchange.path("request").path("uri").asText("UNKNOWN"));
            String operation = method + " " + uri;
            operationCounts.merge(operation, 1, Integer::sum);
        }

        if (newExchanges.isEmpty()) {
            Map<String, Integer> statusDeltas = diffStatusCounts(
                before.path("metricsBreakdown").path("statusCounts"),
                after.path("metricsBreakdown").path("statusCounts")
            );

            status2xx = sumByRange(statusDeltas, 200, 299);
            status3xx = sumByRange(statusDeltas, 300, 399);
            status4xx = sumByRange(statusDeltas, 400, 499);
            status5xx = sumByRange(statusDeltas, 500, 599);

            operationCounts = diffOperationCounts(
                before.path("metricsBreakdown").path("operationCounts"),
                after.path("metricsBreakdown").path("operationCounts")
            );

            distributionSource = operationCounts.isEmpty() && statusDeltas.isEmpty()
                ? "none"
                : "metrics-tags";
        }

        ObjectNode delta = JSON.createObjectNode();
        delta.put("beforeCapturedAt", before.path("capturedAt").asText(""));
        delta.put("afterCapturedAt", after.path("capturedAt").asText(""));
        delta.put("distributionSource", distributionSource);

        ObjectNode totals = delta.putObject("totals");
        totals.put("metricCountBefore", beforeCount);
        totals.put("metricCountAfter", afterCount);
        totals.put("metricCountDelta", afterCount - beforeCount);
        totals.put("newExchangeCount", newExchanges.size());
        totals.put("invalid4xxCount", status4xx);

        ObjectNode statuses = delta.putObject("statusDistribution");
        statuses.put("2xx", status2xx);
        statuses.put("3xx", status3xx);
        statuses.put("4xx", status4xx);
        statuses.put("5xx", status5xx);

        ArrayNode operations = delta.putArray("operationDistribution");
        for (Map.Entry<String, Integer> entry : operationCounts.entrySet()) {
            String operation = entry.getKey();
            int firstSpace = operation.indexOf(' ');
            String method = firstSpace > 0 ? operation.substring(0, firstSpace) : "UNKNOWN";
            String uri = firstSpace > 0 ? operation.substring(firstSpace + 1) : operation;

            ObjectNode op = operations.addObject();
            op.put("method", method);
            op.put("uri", uri);
            op.put("count", entry.getValue());
        }

        return delta;
    }

    private static ObjectNode fetchMetricsBreakdown(URI metricUri, JsonNode metricsNode) {
        ObjectNode breakdown = JSON.createObjectNode();
        ObjectNode statusCounts = breakdown.putObject("statusCounts");
        ArrayNode operationCounts = breakdown.putArray("operationCounts");

        List<String> statuses = extractTagValues(metricsNode, "status");
        for (String status : statuses) {
            double count = queryMetricCount(metricUri, List.of("status:" + status));
            statusCounts.put(status, count);
        }

        List<String> methods = extractTagValues(metricsNode, "method");
        List<String> uris = extractTagValues(metricsNode, "uri");
        for (String method : methods) {
            for (String uri : uris) {
                double count = queryMetricCount(metricUri, List.of("method:" + method, "uri:" + uri));
                if (count <= 0) {
                    continue;
                }

                ObjectNode op = operationCounts.addObject();
                op.put("method", method);
                op.put("uri", uri);
                op.put("count", count);
            }
        }

        return breakdown;
    }

    private static double queryMetricCount(URI metricUri, List<String> tags) {
        try {
            StringBuilder query = new StringBuilder();
            for (String tag : tags) {
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append("tag=").append(URLEncoder.encode(tag, StandardCharsets.UTF_8));
            }

            URI uriWithTags = metricUri;
            if (query.length() > 0) {
                String separator = metricUri.toString().contains("?") ? "&" : "?";
                uriWithTags = URI.create(metricUri + separator + query);
            }

            JsonNode response = fetchJson(uriWithTags);
            return extractMetricCount(response);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static List<String> extractTagValues(JsonNode metricsNode, String tagName) {
        List<String> values = new ArrayList<>();
        JsonNode availableTags = metricsNode.path("availableTags");
        if (!availableTags.isArray()) {
            return values;
        }

        for (JsonNode tag : availableTags) {
            if (!tagName.equals(tag.path("tag").asText(""))) {
                continue;
            }
            JsonNode tagValues = tag.path("values");
            if (!tagValues.isArray()) {
                continue;
            }
            for (JsonNode value : tagValues) {
                String text = value.asText("");
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
            break;
        }

        return values;
    }

    private static Map<String, Integer> diffStatusCounts(JsonNode beforeStatus, JsonNode afterStatus) {
        Map<String, Integer> deltas = new LinkedHashMap<>();
        if (!afterStatus.isObject()) {
            return deltas;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = afterStatus.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String status = field.getKey();
            int after = (int) Math.round(field.getValue().asDouble(0));
            int before = (int) Math.round(beforeStatus.path(status).asDouble(0));
            int delta = Math.max(0, after - before);
            if (delta > 0) {
                deltas.put(status, delta);
            }
        }

        return deltas;
    }

    private static int sumByRange(Map<String, Integer> statusDeltas, int min, int max) {
        int sum = 0;
        for (Map.Entry<String, Integer> entry : statusDeltas.entrySet()) {
            int code = parseInt(entry.getKey());
            if (code >= min && code <= max) {
                sum += entry.getValue();
            }
        }
        return sum;
    }

    private static Map<String, Integer> diffOperationCounts(JsonNode beforeOps, JsonNode afterOps) {
        Map<String, Integer> beforeMap = toOperationCountMap(beforeOps);
        Map<String, Integer> afterMap = toOperationCountMap(afterOps);

        Map<String, Integer> deltas = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : afterMap.entrySet()) {
            String key = entry.getKey();
            int delta = Math.max(0, entry.getValue() - beforeMap.getOrDefault(key, 0));
            if (delta > 0) {
                deltas.put(key, delta);
            }
        }

        return deltas;
    }

    private static Map<String, Integer> toOperationCountMap(JsonNode operationsNode) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (!operationsNode.isArray()) {
            return map;
        }

        for (JsonNode op : operationsNode) {
            String method = op.path("method").asText("UNKNOWN");
            String uri = op.path("uri").asText("UNKNOWN");
            int count = (int) Math.round(op.path("count").asDouble(0));
            String key = method + " " + uri;
            map.merge(key, count, Integer::sum);
        }

        return map;
    }

    private static void writeCsv(Path path, JsonNode delta, boolean includeHeader) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (includeHeader) {
            sb.append("after_captured_at,scenario,mode,base_url,metric_count_before,metric_count_after,metric_count_delta,new_exchange_count,invalid_4xx_count,status_2xx,status_3xx,status_4xx,status_5xx\n");
        }

        sb.append(csv(delta.path("afterCapturedAt").asText(""))).append(',')
            .append(csv(delta.path("scenario").asText(""))).append(',')
            .append(csv(delta.path("mode").asText(""))).append(',')
            .append(csv(delta.path("baseUrl").asText(""))).append(',')
            .append(delta.path("totals").path("metricCountBefore").asDouble(0)).append(',')
            .append(delta.path("totals").path("metricCountAfter").asDouble(0)).append(',')
            .append(delta.path("totals").path("metricCountDelta").asDouble(0)).append(',')
            .append(delta.path("totals").path("newExchangeCount").asInt(0)).append(',')
            .append(delta.path("totals").path("invalid4xxCount").asInt(0)).append(',')
            .append(delta.path("statusDistribution").path("2xx").asInt(0)).append(',')
            .append(delta.path("statusDistribution").path("3xx").asInt(0)).append(',')
            .append(delta.path("statusDistribution").path("4xx").asInt(0)).append(',')
            .append(delta.path("statusDistribution").path("5xx").asInt(0))
            .append('\n');

        if (includeHeader) {
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
            return;
        }

        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }

    private static List<JsonNode> subtractAsMultiset(List<JsonNode> before, List<JsonNode> after) {
        Map<String, Integer> beforeCounts = new HashMap<>();
        for (JsonNode item : before) {
            String key = item.toString();
            beforeCounts.merge(key, 1, Integer::sum);
        }

        List<JsonNode> delta = new ArrayList<>();
        for (JsonNode item : after) {
            String key = item.toString();
            int count = beforeCounts.getOrDefault(key, 0);
            if (count > 0) {
                beforeCounts.put(key, count - 1);
                continue;
            }
            delta.add(item);
        }

        return delta;
    }

    private static String normalizeUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "UNKNOWN";
        }

        try {
            URI parsed = URI.create(uri);
            if (parsed.getPath() != null && !parsed.getPath().isBlank()) {
                return parsed.getPath();
            }
        } catch (IllegalArgumentException ignored) {
        }

        int queryPos = uri.indexOf('?');
        return queryPos >= 0 ? uri.substring(0, queryPos) : uri;
    }

    private static double extractMetricCount(JsonNode metricsNode) {
        JsonNode measurements = metricsNode.path("measurements");
        if (!measurements.isArray()) {
            return 0;
        }

        for (JsonNode measurement : measurements) {
            if ("COUNT".equalsIgnoreCase(measurement.path("statistic").asText(""))) {
                return measurement.path("value").asDouble(0);
            }
        }

        return 0;
    }

    private static List<JsonNode> asList(JsonNode arrayNode) {
        List<JsonNode> items = new ArrayList<>();
        if (!arrayNode.isArray()) {
            return items;
        }

        Iterator<JsonNode> it = arrayNode.elements();
        while (it.hasNext()) {
            items.add(it.next());
        }
        return items;
    }

    private static int resolvePort(URI uri) {
        if (uri.getPort() > 0) {
            return uri.getPort();
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        return 80;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String sanitizePathPart(String value) {
        return value.trim().replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private static Map<String, String> parseOptions(String[] args, int start) {
        Map<String, String> options = new HashMap<>();

        int i = start;
        while (i < args.length) {
            String key = args[i];
            if (!key.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + key);
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for option: " + key);
            }
            options.put(key, args[i + 1]);
            i += 2;
        }

        return options;
    }

    private static String required(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required option: " + key);
        }
        return value;
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  jbang scripts/actuator-delta.java start --scenario S1 --mode conventional --base-url http://localhost:8080");
        System.out.println("  jbang scripts/actuator-delta.java finish --scenario S1 --mode conventional --base-url http://localhost:8080");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --scenario <name>      Scenario id (for example S1)");
        System.out.println("  --mode <name>          Mode label (for example conventional or hypermedia)");
        System.out.println("  --base-url <url>       Service base URL");
        System.out.println("  --actuator-base-path   Optional Actuator base path override (for example /actuator)");
        System.out.println("  --results-dir <path>   Optional output path (default: test-plans/manual-results)");
    }

    private record Snapshot(Instant capturedAt, ObjectNode payload) {
    }

    private record EndpointPair(URI metricsUri, URI metricsCollectionUri, URI httpExchangesUri) {
    }
}
