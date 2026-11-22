package com.userfront.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class FrontEndAssetsTest {

    private static final Path INVENTORY = Paths.get("FRONTEND-LIBRARIES.md");

    private static final Path RESOURCES = Paths.get("src", "main", "resources");

    private static final Path STATIC = RESOURCES.resolve("static");

    private static final Pattern INVENTORY_ROW = Pattern.compile(
            "^\\| `static/([^`]+)` \\| ([^|]+) \\| ([^|]+) \\| ([^|]+) \\| `?([0-9a-f]{64}|-)`? \\|$");

    private static final Pattern LOCAL_ASSET = Pattern.compile("(?:src|href)=[\"'](/(?:js|css)/[^\"']+)[\"']");

    @Test
    public void everyVendoredFileIsInTheInventory() throws IOException {
        TreeSet<String> onDisk = new TreeSet<>();
        for (String dir : new String[] {"js", "css"}) {
            try (Stream<Path> files = Files.walk(STATIC.resolve(dir))) {
                onDisk.addAll(files.filter(Files::isRegularFile)
                        .map(p -> STATIC.relativize(p).toString().replace('\\', '/'))
                        .collect(Collectors.toList()));
            }
        }
        assertEquals(new TreeSet<>(inventory().keySet()), onDisk);
    }

    @Test
    public void vendoredFilesMatchTheirRecordedChecksums() throws Exception {
        for (Map.Entry<String, String> entry : inventory().entrySet()) {
            if (!"-".equals(entry.getValue())) {
                assertEquals("static/" + entry.getKey() + " changed; update FRONTEND-LIBRARIES.md",
                        entry.getValue(), sha256(STATIC.resolve(entry.getKey())));
            }
        }
    }

    @Test
    public void templatesOnlyReferenceInventoriedAssets() throws IOException {
        Map<String, String> inventory = inventory();
        List<String> referenced = templateAssets();
        assertFalse(referenced.isEmpty());
        for (String asset : referenced) {
            String path = asset.substring(1);
            assertTrue(asset + " is referenced by a template but is not in FRONTEND-LIBRARIES.md",
                    inventory.containsKey(path));
            assertTrue(asset + " does not exist", Files.isRegularFile(STATIC.resolve(path)));
        }
    }

    @Test
    public void everyInventoriedLibraryIsReferenced() throws IOException {
        List<String> referenced = templateAssets();
        for (String file : inventory().keySet()) {
            assertTrue("static/" + file + " is not used by any template; delete it",
                    referenced.contains("/" + file));
        }
    }

    private static Map<String, String> inventory() throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        for (String line : Files.readAllLines(INVENTORY, StandardCharsets.UTF_8)) {
            Matcher m = INVENTORY_ROW.matcher(line.trim());
            if (m.matches()) {
                files.put(m.group(1), m.group(5));
            }
        }
        assertFalse("no rows parsed from " + INVENTORY, files.isEmpty());
        return files;
    }

    private static List<String> templateAssets() throws IOException {
        List<String> assets = new ArrayList<>();
        try (Stream<Path> templates = Files.walk(RESOURCES.resolve("templates"))) {
            for (Path template : templates.filter(p -> p.toString().endsWith(".html")).collect(Collectors.toList())) {
                Matcher m = LOCAL_ASSET.matcher(new String(Files.readAllBytes(template), StandardCharsets.UTF_8));
                while (m.find()) {
                    assets.add(m.group(1));
                }
            }
        }
        return assets;
    }

    private static String sha256(Path file) throws IOException, NoSuchAlgorithmException {
        StringBuilder hex = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
