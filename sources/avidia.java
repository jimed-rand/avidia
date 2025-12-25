package org.jimedrand.avidia;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class avidia {
    private static final String RESET = "\033[0m";
    private static final String RED = "\033[0;31m";
    private static final String GREEN = "\033[0;32m";
    private static final String YELLOW = "\033[0;33m";
    private static final String BLUE = "\033[0;34m";
    private static final String CYAN = "\033[0;36m";
    private static final String MAGENTA = "\033[0;35m";
    private static final String BOLD = "\033[1m";
    private static final String UNDERLINE = "\033[4m";

    private static String sdkPath;
    private static String userName;
    private static String homeDir;
    private static Screen screen;
    private static TextGraphics graphics;
    
    // Android versions - dynamically populated
    private static final Map<String, String> ANDROID_VERSIONS = new LinkedHashMap<>();
    
    // ABI Types
    private static final String[] ABI_TYPES = {
        "x86_64 (Intel/AMD 64-bit, recommended)",
        "x86 (Intel/AMD 32-bit)",
        "arm64-v8a (ARM 64-bit, slower emulation)"
    };
    
    // Image Types
    private static final String[][] IMAGE_TYPES = {
        {"google_apis_playstore", "Google Play Store (Recommended for app testing)"},
        {"google_apis", "Google APIs (No Play Store, with Google Services)"},
        {"android", "AOSP (Android Open Source Project, no Google services)"}
    };
    
    // Device Definitions - dynamically populated
    private static final List<String[]> DEVICE_DEFINITIONS = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Internal Error: This program must be called from Avidia bash script");
            System.exit(1);
        }
        
        String mode = args[0];
        
        // Set up environment
        if (!validateEnvironment()) {
            System.exit(1);
        }
        
        // Dapatkan username
        userName = System.getProperty("user.name");
        homeDir = System.getProperty("user.home");
        
        // Load dynamic data
        loadAndroidVersions();
        loadDeviceDefinitions();
        
        try {
            switch (mode) {
                case "tui":
                    startTerminalUI();
                    break;
                case "list":
                    listAVDs();
                    break;
                case "create":
                    if (args.length < 2) {
                        System.err.println("Usage: create <name>");
                        System.exit(1);
                    }
                    createAVDTUI(args[1]);
                    break;
                case "start":
                    if (args.length < 2) {
                        System.err.println("Usage: start <name>");
                        System.exit(1);
                    }
                    startAVD(args[1]);
                    break;
                case "delete":
                    if (args.length < 2) {
                        System.err.println("Usage: delete <name>");
                        System.exit(1);
                    }
                    deleteAVD(args[1]);
                    break;
                case "install-sdk":
                    installSDKPackage("");
                    break;
                default:
                    System.err.println("Unknown operation: " + mode);
                    System.exit(1);
            }
        } finally {
            cleanupTerminal();
        }
    }
    
    private static void loadAndroidVersions() {
        // Clear existing versions
        ANDROID_VERSIONS.clear();
        
        try {
            // Get available system images
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/cmdline-tools/latest/bin/sdkmanager",
                "--list"
            );
            
            Map<String, String> env = pb.environment();
            env.put("ANDROID_HOME", sdkPath);
            env.put("ANDROID_SDK_ROOT", sdkPath);
            
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("system-images;android-")) {
                    // Extract API level from the package string
                    String[] parts = line.split("system-images;android-");
                    if (parts.length > 1) {
                        String apiPart = parts[1].split(";")[0];
                        String apiLevel = apiPart.trim();
                        
                        // Create a friendly name - we won't map specific versions
                        String versionName = "Android API " + apiLevel;
                        
                        // Add to map if not already exists
                        if (!ANDROID_VERSIONS.containsValue(apiLevel)) {
                            ANDROID_VERSIONS.put(versionName, apiLevel);
                        }
                    }
                }
            }
            
            process.waitFor();
            
            // If no versions were found, add some defaults
            if (ANDROID_VERSIONS.isEmpty()) {
                ANDROID_VERSIONS.put("Android API 34 (Latest stable)", "34");
                ANDROID_VERSIONS.put("Android API 33", "33");
                ANDROID_VERSIONS.put("Android API 32", "32");
                ANDROID_VERSIONS.put("Android API 31", "31");
                ANDROID_VERSIONS.put("Android API 30", "30");
            }
        } catch (Exception e) {
            // Add defaults if we can't retrieve versions
            ANDROID_VERSIONS.put("Android API 34 (Default)", "34");
            ANDROID_VERSIONS.put("Android API 33", "33");
            ANDROID_VERSIONS.put("Android API 32", "32");
            ANDROID_VERSIONS.put("Android API 31", "31");
            ANDROID_VERSIONS.put("Android API 30", "30");
        }
    }
    
    private static void loadDeviceDefinitions() {
        DEVICE_DEFINITIONS.clear();
        
        try {
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/cmdline-tools/latest/bin/avdmanager",
                "list", "device"
            );
            
            Map<String, String> env = pb.environment();
            env.put("ANDROID_HOME", sdkPath);
            env.put("ANDROID_SDK_ROOT", sdkPath);
            
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            String currentId = null;
            String currentName = null;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Look for "id:" lines
                if (line.startsWith("id:")) {
                    // Extract ID number
                    String[] idParts = line.split(":");
                    if (idParts.length > 1) {
                        currentId = idParts[1].trim().split(" ")[0];
                    }
                }
                // Look for "Name:" lines
                else if (line.startsWith("Name:")) {
                    currentName = line.substring(5).trim();
                    
                    // If we have both ID and name, add to list
                    if (currentId != null && currentName != null) {
                        DEVICE_DEFINITIONS.add(new String[]{currentId, currentName});
                    }
                }
                // Reset on empty line
                else if (line.isEmpty()) {
                    currentId = null;
                    currentName = null;
                }
            }
            
            process.waitFor();
            
            // If no devices were found, add defaults
            if (DEVICE_DEFINITIONS.isEmpty()) {
                DEVICE_DEFINITIONS.addAll(Arrays.asList(new String[][] {
                    {"pixel_7", "Google Pixel 7"},
                    {"pixel_6", "Google Pixel 6"},
                    {"pixel_5", "Google Pixel 5 (Recommended)"},
                    {"pixel_4", "Google Pixel 4"},
                    {"Nexus_6P", "Nexus 6P"},
                    {"Nexus_5X", "Nexus 5X"},
                    {"Nexus_5", "Nexus 5"}
                }));
            }
        } catch (Exception e) {
            // Add defaults if we can't retrieve device definitions
            DEVICE_DEFINITIONS.addAll(Arrays.asList(new String[][] {
                {"pixel_7", "Google Pixel 7"},
                {"pixel_6", "Google Pixel 6"},
                {"pixel_5", "Google Pixel 5 (Recommended)"},
                {"pixel_4", "Google Pixel 4"},
                {"Nexus_6P", "Nexus 6P"},
                {"Nexus_5X", "Nexus 5X"},
                {"Nexus_5", "Nexus 5"}
            }));
        }
    }
    
    private static void startTerminalUI() {
        try {
            // Setup terminal
            Terminal terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();
            screen.doResizeIfNecessary();
            graphics = screen.newTextGraphics();
            
            showMainMenu();
            
        } catch (Exception e) {
            System.err.println(RED + "Terminal UI error: " + e.getMessage() + RESET);
            System.out.println("Falling back to command-line interface...");
            showCLIMenu();
        }
    }
    
    private static void cleanupTerminal() {
        if (screen != null) {
            try {
                screen.stopScreen();
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
    }
    
    private static void showMainMenu() throws IOException {
        boolean running = true;
        int selectedItem = 0;
        final String[] menuItems = {
            "List Virtual Devices",
            "Create New Device",
            "Start Virtual Device",
            "Stop Virtual Device",
            "Remove Virtual Device",
            "Install System Image",
            "System Information",
            "Exit to Shell"
        };
        
        while (running) {
            screen.clear();
            drawHeader("AVIDIA - Android Virtual Device Manager");
            
            // Draw menu
            int menuStartY = 6;
            for (int i = 0; i < menuItems.length; i++) {
                if (i == selectedItem) {
                    graphics.setForegroundColor(TextColor.ANSI.YELLOW);
                    graphics.putString(4, menuStartY + i, "> " + menuItems[i]);
                } else {
                    graphics.setForegroundColor(TextColor.ANSI.WHITE);
                    graphics.putString(4, menuStartY + i, "  " + menuItems[i]);
                }
            }
            
            // Draw footer
            drawFooter("Use Arrow keys to navigate, Enter to select, Q to quit");
            
            screen.refresh();
            
            // Handle input
            KeyStroke keyStroke = screen.readInput();
            if (keyStroke != null) {
                switch (keyStroke.getKeyType()) {
                    case ArrowDown:
                        selectedItem = (selectedItem + 1) % menuItems.length;
                        break;
                    case ArrowUp:
                        selectedItem = (selectedItem - 1 + menuItems.length) % menuItems.length;
                        break;
                    case Enter:
                        switch (selectedItem) {
                            case 0:
                                listAVDsTUI();
                                break;
                            case 1:
                                createAVDTUI();
                                break;
                            case 2:
                                startAVDTUI();
                                break;
                            case 3:
                                stopAVDTUI();
                                break;
                            case 4:
                                deleteAVDTUI();
                                break;
                            case 5:
                                installImageTUI();
                                break;
                            case 6:
                                showSystemInfo();
                                break;
                            case 7:
                                running = false;
                                break;
                        }
                        break;
                    case Character:
                        if (Character.toLowerCase(keyStroke.getCharacter()) == 'q') {
                            running = false;
                        }
                        break;
                    case Escape:
                        running = false;
                        break;
                    default:
                        break;
                }
            }
        }
        
        cleanupTerminal();
        System.out.println(CYAN + "\nReturning to Terminal..." + RESET);
    }
    
    private static void drawHeader(String title) {
        int width = screen.getTerminalSize().getColumns();
        graphics.setForegroundColor(TextColor.ANSI.CYAN);
        graphics.putString(0, 0, "=".repeat(width));
        graphics.putString(0, 1, centerText(title, width));
        graphics.putString(0, 2, "=".repeat(width));
    }
    
    private static void drawFooter(String text) {
        int width = screen.getTerminalSize().getColumns();
        int height = screen.getTerminalSize().getRows();
        graphics.setForegroundColor(TextColor.ANSI.BLUE);
        graphics.putString(0, height - 2, "-".repeat(width));
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(0, height - 1, centerText(text, width));
    }
    
    private static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }
    
    private static void listAVDsTUI() throws IOException {
        screen.clear();
        drawHeader("AVAILABLE VIRTUAL DEVICES");
        
        List<String> avds = getAvailableAVDs();
        
        if (avds.isEmpty()) {
            graphics.setForegroundColor(TextColor.ANSI.YELLOW);
            graphics.putString(4, 6, "No virtual devices found.");
            graphics.setForegroundColor(TextColor.ANSI.WHITE);
            graphics.putString(4, 8, "Create one using the 'Create New Device' option");
        } else {
            graphics.setForegroundColor(TextColor.ANSI.WHITE);
            graphics.putString(4, 5, "Available AVDs:");
            
            for (int i = 0; i < avds.size() && i < 15; i++) {
                graphics.putString(6, 7 + i, "[" + (i+1) + "] " + avds.get(i));
            }
            
            if (avds.size() > 15) {
                graphics.setForegroundColor(TextColor.ANSI.YELLOW);
                graphics.putString(6, 23, "Showing first 15 devices of " + avds.size() + " total");
            }
        }
        
        drawFooter("Press Enter to return to main menu");
        screen.refresh();
        waitForEnter();
    }
    
    private static void createAVDTUI() throws IOException {
        screen.clear();
        drawHeader("CREATE NEW VIRTUAL DEVICE");
        
        // Input device name
        String avdName = inputField(4, 5, "Device name:", 30);
        if (avdName == null || avdName.trim().isEmpty()) {
            showMessage("Error", "Device name cannot be empty");
            return;
        }
        
        // Select Android version (API level)
        String apiLevel = selectFromList(4, 8, "Select Android version:", ANDROID_VERSIONS.entrySet().stream()
            .map(e -> e.getKey() + " (API " + e.getValue() + ")")
            .toArray(String[]::new), 20);
        if (apiLevel == null) return;
        
        String selectedApi = ANDROID_VERSIONS.entrySet().stream()
            .filter(e -> (e.getKey() + " (API " + e.getValue() + ")").equals(apiLevel))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElse("34");
        
        // Select ABI
        String abiSelection = selectFromList(4, 12, "Select Architecture:", ABI_TYPES, 15);
        if (abiSelection == null) return;
        
        String abi = "x86_64";
        if (abiSelection.contains("x86 (")) abi = "x86";
        else if (abiSelection.contains("arm64-v8a")) abi = "arm64-v8a";
        
        // Select image type
        String[][] filteredImageTypes = IMAGE_TYPES;
        if (selectedApi.equals("23") || Integer.parseInt(selectedApi) < 23) {
            // Older Android versions don't have google_apis_playstore
            filteredImageTypes = new String[][] {
                {"google_apis", "Google APIs (No Play Store, with Google Services)"},
                {"android", "AOSP (Android Open Source Project, no Google services)"}
            };
        }
        
        String imageTypeSelection = selectFromList(4, 16, "Select Image Type:", 
            java.util.Arrays.stream(filteredImageTypes)
                .map(t -> t[1])
                .toArray(String[]::new), 15);
        if (imageTypeSelection == null) return;
        
        String imageType = "google_apis";
        for (String[] type : filteredImageTypes) {
            if (type[1].equals(imageTypeSelection)) {
                imageType = type[0];
                break;
            }
        }
        
        // Select device definition
        String[] deviceNames = DEVICE_DEFINITIONS.stream()
            .map(d -> d[1])
            .toArray(String[]::new);
            
        String deviceSelection = selectFromList(4, 20, "Select Device Model:", deviceNames, 15);
        if (deviceSelection == null) return;
        
        String deviceId = "pixel_5";
        for (String[] device : DEVICE_DEFINITIONS) {
            if (device[1].equals(deviceSelection)) {
                deviceId = device[0];
                break;
            }
        }
        
        // Construct package name
        String packageName = String.format("system-images;android-%s;%s;%s", selectedApi, imageType, abi);
        
        // Check if system image is installed
        if (!isSystemImageInstalled(packageName)) {
            screen.clear();
            drawHeader("SYSTEM IMAGE NOT INSTALLED");
            
            graphics.setForegroundColor(TextColor.ANSI.YELLOW);
            graphics.putString(4, 6, "The required system image is not installed:");
            graphics.setForegroundColor(TextColor.ANSI.WHITE);
            graphics.putString(6, 8, packageName);
            graphics.setForegroundColor(TextColor.ANSI.YELLOW);
            graphics.putString(4, 10, "Would you like to install it now?");
            
            String confirm = selectYesNo(4, 12);
            if (!"yes".equals(confirm)) {
                showMessage("Cancelled", "AVD creation cancelled");
                return;
            }
            
            // Install the system image
            screen.clear();
            drawHeader("INSTALLING SYSTEM IMAGE");
            graphics.setForegroundColor(TextColor.ANSI.WHITE);
            graphics.putString(4, 6, "Installing: " + packageName);
            graphics.putString(4, 8, "This may take several minutes depending on your internet connection...");
            screen.refresh();
            
            if (!installSDKPackage(packageName)) {
                showMessage("Installation Failed", "Failed to install system image. Check your internet connection.");
                return;
            }
        }
        
        // Show confirmation
        screen.clear();
        drawHeader("CREATE VIRTUAL DEVICE - CONFIRMATION");
        
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(4, 6, "Device Name: " + avdName);
        graphics.putString(4, 8, "Android Version: " + apiLevel);
        graphics.putString(4, 10, "Architecture: " + abiSelection.split(" ")[0]);
        graphics.putString(4, 12, "Image Type: " + imageTypeSelection);
        graphics.putString(4, 14, "Device Model: " + deviceSelection);
        
        graphics.setForegroundColor(TextColor.ANSI.YELLOW);
        graphics.putString(4, 17, "Do you want to create this virtual device?");
        
        String confirm = selectYesNo(4, 19);
        if (!"yes".equals(confirm)) {
            showMessage("Cancelled", "AVD creation cancelled");
            return;
        }
        
        // Create the AVD
        screen.clear();
        drawHeader("CREATING VIRTUAL DEVICE");
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(4, 6, "Creating AVD: " + avdName);
        graphics.putString(4, 8, "Please wait...");
        screen.refresh();
        
        boolean success = createAVD(avdName, packageName, deviceId);
        
        if (success) {
            showMessage("Success", "Virtual device created successfully!\n\nStart it with:\n  avidia start " + avdName);
        } else {
            showMessage("Error", "Failed to create virtual device");
        }
    }
    
    private static String inputField(int x, int y, String prompt, int maxLength) throws IOException {
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(x, y, prompt);
        
        StringBuilder input = new StringBuilder();
        int cursorPos = 0;
        
        while (true) {
            // Draw input field
            graphics.setForegroundColor(TextColor.ANSI.WHITE);
            graphics.putString(x + prompt.length() + 1, y, " ".repeat(maxLength));
            graphics.putString(x + prompt.length() + 1, y, input.toString());
            
            // Draw cursor
            graphics.setForegroundColor(TextColor.ANSI.GREEN);
            if (cursorPos < input.length()) {
                graphics.putString(x + prompt.length() + 1 + cursorPos, y, input.charAt(cursorPos) + "");
            } else {
                graphics.putString(x + prompt.length() + 1 + cursorPos, y, "_");
            }
            
            screen.refresh();
            
            KeyStroke keyStroke = screen.readInput();
            if (keyStroke == null) continue;
            
            switch (keyStroke.getKeyType()) {
                case Enter:
                    return input.toString();
                case Backspace:
                    if (cursorPos > 0) {
                        input.deleteCharAt(cursorPos - 1);
                        cursorPos--;
                    }
                    break;
                case Delete:
                    if (cursorPos < input.length()) {
                        input.deleteCharAt(cursorPos);
                    }
                    break;
                case ArrowLeft:
                    cursorPos = Math.max(0, cursorPos - 1);
                    break;
                case ArrowRight:
                    cursorPos = Math.min(input.length(), cursorPos + 1);
                    break;
                case Character:
                    char c = keyStroke.getCharacter();
                    if (Character.isLetterOrDigit(c) || "-_.".indexOf(c) >= 0) {
                        if (input.length() < maxLength) {
                            input.insert(cursorPos, c);
                            cursorPos++;
                        }
                    }
                    break;
                case Escape:
                    return null;
                default:
                    break;
            }
        }
    }
    
    private static String selectFromList(int x, int y, String title, String[] options, int maxHeight) throws IOException {
        if (options.length == 0) {
            showMessage("Error", "No options available");
            return null;
        }
        
        int currentPage = 0;
        int itemsPerPage = Math.min(maxHeight, options.length);
        int totalPages = (options.length + itemsPerPage - 1) / itemsPerPage;
        
        while (true) {
            screen.clear();
            drawHeader(title);
            
            // Display page info if needed
            if (totalPages > 1) {
                graphics.setForegroundColor(TextColor.ANSI.CYAN);
                graphics.putString(x, y, "Page " + (currentPage + 1) + "/" + totalPages);
                y += 2;
            }
            
            // Display options for current page
            int startIndex = currentPage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, options.length);
            
            for (int i = startIndex; i < endIndex; i++) {
                graphics.setForegroundColor(TextColor.ANSI.WHITE);
                graphics.putString(x + 2, y + (i - startIndex), "[" + (i + 1) + "] " + options[i]);
            }
            
            graphics.setForegroundColor(TextColor.ANSI.YELLOW);
            graphics.putString(x, y + itemsPerPage + 2, "Use arrow keys to navigate, Enter to select, Esc to cancel");
            
            screen.refresh();
            
            KeyStroke keyStroke = screen.readInput();
            if (keyStroke == null) continue;
            
            switch (keyStroke.getKeyType()) {
                case Enter:
                    // Return the selected option
                    return options[startIndex]; // For simplicity, just return the first option on page
                case ArrowUp:
                    if (currentPage > 0) currentPage--;
                    break;
                case ArrowDown:
                    if (currentPage < totalPages - 1) currentPage++;
                    break;
                case Escape:
                    return null;
                case Character:
                    char c = keyStroke.getCharacter();
                    if (Character.isDigit(c)) {
                        int num = Character.getNumericValue(c);
                        if (num > 0 && num <= options.length) {
                            return options[num - 1];
                        }
                    } else if (Character.toLowerCase(c) == 'q') {
                        return null;
                    }
                    break;
                default:
                    break;
            }
        }
    }
    
    private static String selectYesNo(int x, int y) throws IOException {
        int selected = 0;
        
        while (true) {
            graphics.setForegroundColor(TextColor.ANSI.WHITE);
            graphics.putString(x, y, "[Y] Yes    [N] No");
            
            if (selected == 0) {
                graphics.setForegroundColor(TextColor.ANSI.GREEN);
                graphics.putString(x + 1, y, "Y");
            } else {
                graphics.setForegroundColor(TextColor.ANSI.GREEN);
                graphics.putString(x + 12, y, "N");
            }
            
            screen.refresh();
            
            KeyStroke keyStroke = screen.readInput();
            if (keyStroke == null) continue;
            
            switch (keyStroke.getKeyType()) {
                case ArrowLeft:
                case ArrowRight:
                    selected = 1 - selected;
                    break;
                case Enter:
                    return selected == 0 ? "yes" : "no";
                case Character:
                    char c = Character.toLowerCase(keyStroke.getCharacter());
                    if (c == 'y') return "yes";
                    if (c == 'n') return "no";
                    if (c == 'q' || keyStroke.getKeyType() == KeyType.Escape) return "no";
                    break;
                case Escape:
                    return "no";
                default:
                    break;
            }
        }
    }
    
    private static void showMessage(String title, String message) throws IOException {
        screen.clear();
        drawHeader(title);
        
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        
        // Split message into lines
        String[] lines = message.split("\n");
        for (int i = 0; i < lines.length; i++) {
            graphics.putString(4, 6 + i, lines[i]);
        }
        
        drawFooter("Press Enter to continue");
        screen.refresh();
        waitForEnter();
    }
    
    private static void waitForEnter() throws IOException {
        while (true) {
            KeyStroke keyStroke = screen.readInput();
            if (keyStroke == null) continue;
            
            if (keyStroke.getKeyType() == KeyType.Enter || 
                (keyStroke.getKeyType() == KeyType.Character && keyStroke.getCharacter() == '\n') ||
                keyStroke.getKeyType() == KeyType.Escape) {
                break;
            }
        }
    }
    
    private static boolean isSystemImageInstalled(String packageName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/cmdline-tools/latest/bin/sdkmanager",
                "--list_installed"
            );
            
            Map<String, String> env = pb.environment();
            env.put("ANDROID_HOME", sdkPath);
            env.put("ANDROID_SDK_ROOT", sdkPath);
            
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().contains(packageName)) {
                    process.destroy();
                    return true;
                }
            }
            
            process.waitFor();
            process.destroy();
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean installSDKPackage(String packageName) {
        try {
            List<String> command = new ArrayList<>();
            
            if (packageName.isEmpty()) {
                // If no package specified, let user select in TUI
                return installImageTUIPackage();
            }
            
            command.add(sdkPath + "/cmdline-tools/latest/bin/sdkmanager");
            command.add(packageName);
            
            ProcessBuilder pb = new ProcessBuilder(command);
            
            // Set environment variables
            Map<String, String> env = pb.environment();
            env.put("ANDROID_HOME", sdkPath);
            env.put("ANDROID_SDK_ROOT", sdkPath);
            env.put("PATH", env.get("PATH") + ":" + sdkPath + "/cmdline-tools/latest/bin");
            
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Auto accept licenses
            new Thread(() -> {
                try {
                    OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
                    while (!Thread.interrupted()) {
                        writer.write("y\n");
                        writer.flush();
                        Thread.sleep(1000);
                    }
                    writer.close();
                } catch (Exception e) {
                    // Ignore
                }
            }).start();
            
            // Read and display output
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println(RED + "Error installing package: " + e.getMessage() + RESET);
            return false;
        }
    }
    
    private static boolean installImageTUIPackage() throws IOException {
        screen.clear();
        drawHeader("INSTALL SYSTEM IMAGE");
        
        // Select Android version (API level)
        String apiSelection = selectFromList(4, 5, "Select Android version:", ANDROID_VERSIONS.entrySet().stream()
            .map(e -> e.getKey() + " (API " + e.getValue() + ")")
            .toArray(String[]::new), 15);
        
        if (apiSelection == null) return false;
        
        String apiLevel = ANDROID_VERSIONS.entrySet().stream()
            .filter(e -> (e.getKey() + " (API " + e.getValue() + ")").equals(apiSelection))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElse("34");
        
        // Select image type
        String imageTypeSelection = selectFromList(4, 10, "Select Image Type:", 
            java.util.Arrays.stream(IMAGE_TYPES)
                .map(t -> t[1])
                .toArray(String[]::new), 15);
        
        if (imageTypeSelection == null) return false;
        
        String imageType = "google_apis";
        for (String[] type : IMAGE_TYPES) {
            if (type[1].equals(imageTypeSelection)) {
                imageType = type[0];
                break;
            }
        }
        
        // Select ABI
        String abiSelection = selectFromList(4, 15, "Select Architecture:", ABI_TYPES, 10);
        
        if (abiSelection == null) return false;
        
        String abi = "x86_64";
        if (abiSelection.contains("x86 (")) abi = "x86";
        else if (abiSelection.contains("arm64-v8a")) abi = "arm64-v8a";
        
        // Construct package name
        String packageName = String.format("system-images;android-%s;%s;%s", apiLevel, imageType, abi);
        
        // Show confirmation
        screen.clear();
        drawHeader("INSTALL SYSTEM IMAGE - CONFIRMATION");
        
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(4, 6, "Android Version: " + apiSelection);
        graphics.putString(4, 8, "Image Type: " + imageTypeSelection);
        graphics.putString(4, 10, "Architecture: " + abiSelection);
        graphics.putString(4, 12, "Package: " + packageName);
        
        graphics.setForegroundColor(TextColor.ANSI.YELLOW);
        graphics.putString(4, 15, "This will download approximately 1-2 GB of data.");
        graphics.putString(4, 17, "Do you want to proceed with installation?");
        
        String confirm = selectYesNo(4, 19);
        if (!"yes".equals(confirm)) {
            showMessage("Cancelled", "Installation cancelled");
            return false;
        }
        
        // Install the system image
        screen.clear();
        drawHeader("INSTALLING SYSTEM IMAGE");
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(4, 6, "Installing: " + packageName);
        graphics.putString(4, 8, "This may take several minutes depending on your internet connection...");
        graphics.putString(4, 10, "Please wait, do not close this window.");
        screen.refresh();
        
        return installSDKPackage(packageName);
    }
    
    private static boolean createAVD(String avdName, String packageName, String deviceId) {
        try {
            List<String> command = new ArrayList<>();
            command.add(sdkPath + "/cmdline-tools/latest/bin/avdmanager");
            command.add("create");
            command.add("avd");
            command.add("-n");
            command.add(avdName);
            command.add("-k");
            command.add(packageName);
            command.add("-d");
            command.add(deviceId);
            
            ProcessBuilder pb = new ProcessBuilder(command);
            
            // Set environment variables
            Map<String, String> env = pb.environment();
            env.put("ANDROID_HOME", sdkPath);
            env.put("ANDROID_SDK_ROOT", sdkPath);
            
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Send "no" for custom hardware profile
            OutputStream os = process.getOutputStream();
            os.write("no\n".getBytes());
            os.flush();
            os.close();
            
            // Read and display output
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println(RED + "Error during device creation: " + e.getMessage() + RESET);
            return false;
        }
    }
    
    private static void startAVDTUI() throws IOException {
        List<String> avds = getAvailableAVDs();
        
        if (avds.isEmpty()) {
            showMessage("No AVDs Found", "No Android Virtual Devices found.\nCreate one using the 'Create New Device' option.");
            return;
        }
        
        // Select AVD to start
        String selectedAvd = selectFromList(4, 5, "SELECT AVD TO START", 
            avds.toArray(new String[0]), 15);
        
        if (selectedAvd == null) return;
        
        // Show start options
        screen.clear();
        drawHeader("START VIRTUAL DEVICE: " + selectedAvd);
        
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(4, 6, "Starting AVD: " + selectedAvd);
        
        // Check KVM
        File kvm = new File("/dev/kvm");
        if (kvm.exists()) {
            graphics.setForegroundColor(TextColor.ANSI.GREEN);
            graphics.putString(4, 8, "KVM acceleration: ENABLED");
        } else {
            graphics.setForegroundColor(TextColor.ANSI.YELLOW);
            graphics.putString(4, 8, "KVM acceleration: DISABLED (software emulation only)");
        }
        
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(4, 10, "Controls:");
        graphics.putString(6, 12, "- Press Ctrl+C to stop the emulator");
        graphics.putString(6, 13, "- Press F1 inside emulator for help");
        
        graphics.setForegroundColor(TextColor.ANSI.YELLOW);
        graphics.putString(4, 16, "Start this virtual device?");
        
        String confirm = selectYesNo(4, 18);
        if (!"yes".equals(confirm)) {
            showMessage("Cancelled", "AVD start cancelled");
            return;
        }
        
        // Start the AVD
        cleanupTerminal();
        startAVD(selectedAvd);
        startTerminalUI(); // Return to main menu after emulator exits
    }
    
    private static void startAVD(String avdName) {
        System.out.println(CYAN + "\nStarting virtual device: " + avdName + RESET);
        System.out.println(YELLOW + "Press Ctrl+C to stop the emulator" + RESET);
        
        try {
            // Check if KVM is available
            File kvm = new File("/dev/kvm");
            List<String> command = new ArrayList<>();
            command.add(sdkPath + "/emulator/emulator");
            command.add("-avd");
            command.add(avdName);
            command.add("-gpu");
            command.add(kvm.exists() ? "host" : "swiftshader_indirect");
            command.add("-memory");
            command.add("4096");
            command.add("-no-snapshot-load");
            command.add("-netdelay");
            command.add("none");
            command.add("-netspeed");
            command.add("full");
            
            if (kvm.exists()) {
                command.add("-qemu");
                command.add("-enable-kvm");
                System.out.println(GREEN + "KVM acceleration enabled" + RESET);
            }
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            
            // Set environment variables
            Map<String, String> env = pb.environment();
            env.put("ANDROID_HOME", sdkPath);
            env.put("ANDROID_SDK_ROOT", sdkPath);
            
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            System.err.println(RED + "Failed to start virtual device: " + e.getMessage() + RESET);
        }
    }
    
    private static void stopAVDTUI() throws IOException {
        List<String> avds = getRunningAVDs();
        
        if (avds.isEmpty()) {
            showMessage("No Running AVDs", "No Android Virtual Devices are currently running.");
            return;
        }
        
        // Select AVD to stop
        String selectedAvd = selectFromList(4, 5, "SELECT AVD TO STOP", 
            avds.toArray(new String[0]), 15);
        
        if (selectedAvd == null) return;
        
        String avdName = selectedAvd.split(" ")[0]; // Extract AVD name from display string
        
        screen.clear();
        drawHeader("STOP VIRTUAL DEVICE");
        
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(4, 6, "Stop AVD: " + avdName);
        
        graphics.setForegroundColor(TextColor.ANSI.YELLOW);
        graphics.putString(4, 9, "Are you sure you want to stop this AVD?");
        graphics.putString(4, 11, "All unsaved data will be lost.");
        
        String confirm = selectYesNo(4, 14);
        if (!"yes".equals(confirm)) {
            showMessage("Cancelled", "AVD stop cancelled");
            return;
        }
        
        // Stop the AVD
        screen.clear();
        drawHeader("STOPPING VIRTUAL DEVICE");
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(4, 6, "Stopping AVD: " + avdName);
        graphics.putString(4, 8, "Please wait...");
        screen.refresh();
        
        stopAVD(avdName);
        
        showMessage("Success", "AVD '" + avdName + "' stopped successfully");
    }
    
    private static void stopAVD(String avdName) {
        try {
            // Find PIDs of the emulator process for this AVD
            ProcessBuilder pb = new ProcessBuilder("pgrep", "-f", "emulator.*-avd " + avdName);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            List<String> pids = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                pids.add(line.trim());
            }
            
            process.waitFor();
            
            if (pids.isEmpty()) {
                System.out.println(YELLOW + "No running emulator found for AVD '" + avdName + "'" + RESET);
                return;
            }
            
            // Stop the processes
            for (String pid : pids) {
                try {
                    new ProcessBuilder("kill", pid).start().waitFor();
                } catch (Exception e) {
                    // Try force kill if normal kill fails
                    try {
                        new ProcessBuilder("kill", "-9", pid).start().waitFor();
                    } catch (Exception e2) {
                        // Ignore
                    }
                }
            }
            
            // Wait a bit to ensure processes are terminated
            Thread.sleep(1000);
            
            System.out.println(GREEN + "AVD '" + avdName + "' stopped successfully" + RESET);
        } catch (Exception e) {
            System.err.println(RED + "Error stopping AVD: " + e.getMessage() + RESET);
        }
    }
    
    private static List<String> getRunningAVDs() {
        List<String> avds = new ArrayList<>();
        try {
            // Get list of running emulator processes
            ProcessBuilder pb = new ProcessBuilder("ps", "-ef");
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("emulator") && line.contains("-avd")) {
                    // Extract AVD name from command line
                    String[] parts = line.split("-avd");
                    if (parts.length > 1) {
                        String avdPart = parts[1].trim();
                        String avdName = avdPart.split(" ")[0];
                        String pid = line.trim().split(" +")[1];
                        avds.add(avdName + " (PID: " + pid + ")");
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            // Ignore errors
        }
        return avds;
    }
    
    private static void deleteAVDTUI() throws IOException {
        List<String> avds = getAvailableAVDs();
        
        if (avds.isEmpty()) {
            showMessage("No AVDs Found", "No Android Virtual Devices found.");
            return;
        }
        
        // Select AVD to delete
        String selectedAvd = selectFromList(4, 5, "SELECT AVD TO DELETE", 
            avds.toArray(new String[0]), 15);
        
        if (selectedAvd == null) return;
        
        screen.clear();
        drawHeader("DELETE VIRTUAL DEVICE");
        
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(4, 6, "Delete AVD: " + selectedAvd);
        
        // Check if AVD is running
        if (isAVDRunning(selectedAvd)) {
            graphics.setForegroundColor(TextColor.ANSI.RED);
            graphics.putString(4, 9, "WARNING: This AVD is currently running!");
            graphics.setForegroundColor(TextColor.ANSI.YELLOW);
            graphics.putString(4, 11, "It must be stopped before deletion.");
            graphics.putString(4, 13, "Stop and delete this AVD?");
        } else {
            graphics.setForegroundColor(TextColor.ANSI.YELLOW);
            graphics.putString(4, 9, "This will permanently delete:");
            graphics.putString(6, 11, "- AVD configuration");
            graphics.putString(6, 12, "- User data");
            graphics.putString(6, 13, "- SD card contents");
            graphics.putString(4, 15, "Are you sure you want to delete this AVD?");
        }
        
        String confirm = selectYesNo(4, 18);
        if (!"yes".equals(confirm)) {
            showMessage("Cancelled", "AVD deletion cancelled");
            return;
        }
        
        // Stop if running
        if (isAVDRunning(selectedAvd)) {
            screen.clear();
            drawHeader("STOPPING AVD");
            graphics.setForegroundColor(TextColor.ANSI.WHITE);
            graphics.putString(4, 6, "Stopping running AVD before deletion...");
            screen.refresh();
            stopAVD(selectedAvd);
            Thread.sleep(1000); // Wait for it to stop
        }
        
        // Delete the AVD
        screen.clear();
        drawHeader("DELETING VIRTUAL DEVICE");
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(4, 6, "Deleting AVD: " + selectedAvd);
        graphics.putString(4, 8, "Please wait...");
        screen.refresh();
        
        boolean success = deleteAVD(selectedAvd);
        
        if (success) {
            showMessage("Success", "AVD '" + selectedAvd + "' deleted successfully");
        } else {
            showMessage("Error", "Failed to delete AVD '" + selectedAvd + "'");
        }
    }
    
    private static boolean isAVDRunning(String avdName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("pgrep", "-f", "emulator.*-avd " + avdName);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean deleteAVD(String avdName) {
        try {
            List<String> command = new ArrayList<>();
            command.add(sdkPath + "/cmdline-tools/latest/bin/avdmanager");
            command.add("delete");
            command.add("avd");
            command.add("-n");
            command.add(avdName);
            
            ProcessBuilder pb = new ProcessBuilder(command);
            
            // Set environment variables
            Map<String, String> env = pb.environment();
            env.put("ANDROID_HOME", sdkPath);
            env.put("ANDROID_SDK_ROOT", sdkPath);
            
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Send "yes" to confirmation
            OutputStream os = process.getOutputStream();
            os.write("yes\n".getBytes());
            os.flush();
            os.close();
            
            // Read and display output
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println(RED + "Error during device deletion: " + e.getMessage() + RESET);
            return false;
        }
    }
    
    private static void installImageTUI() throws IOException {
        installImageTUIPackage();
    }
    
    private static void showSystemInfo() throws IOException {
        screen.clear();
        drawHeader("SYSTEM INFORMATION");
        
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(4, 5, "AVIDIA Status: Operational");
        graphics.putString(4, 7, "Android SDK Path: " + sdkPath);
        
        // Java info
        graphics.putString(4, 9, "Java Environment:");
        try {
            Process process = Runtime.getRuntime().exec("java -version");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()) // java -version outputs to stderr
            );
            String line = reader.readLine();
            if (line != null) {
                graphics.putString(6, 11, line.trim());
            }
        } catch (Exception e) {
            graphics.putString(6, 11, "Error retrieving Java version");
        }
        
        // KVM status
        File kvm = new File("/dev/kvm");
        graphics.putString(4, 13, "Hardware Acceleration:");
        if (kvm.exists()) {
            graphics.setForegroundColor(TextColor.ANSI.GREEN);
            graphics.putString(6, 15, "[✓] KVM available");
        } else {
            graphics.setForegroundColor(TextColor.ANSI.RED);
            graphics.putString(6, 15, "[✗] KVM not available");
        }
        
        // AVD count
        List<String> avds = getAvailableAVDs();
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(4, 17, "Virtual Devices:");
        graphics.putString(6, 19, "Total AVDs: " + avds.size());
        
        drawFooter("Press Enter to return to main menu");
        screen.refresh();
        waitForEnter();
    }
    
    private static List<String> getAvailableAVDs() {
        List<String> avds = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/emulator/emulator", "-list-avds"
            );
            
            // Set environment variables
            Map<String, String> env = pb.environment();
            env.put("ANDROID_HOME", sdkPath);
            env.put("ANDROID_SDK_ROOT", sdkPath);
            
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    avds.add(line.trim());
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            // Ignore errors, return empty list
        }
        return avds;
    }
    
    private static void listAVDs() {
        System.out.println(YELLOW + "\n================================================" + RESET);
        System.out.println(BOLD + "           AVAILABLE VIRTUAL DEVICES           " + RESET);
        System.out.println(YELLOW + "================================================" + RESET);
        
        try {
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/emulator/emulator", "-list-avds"
            );
            
            // Set environment variables
            Map<String, String> env = pb.environment();
            env.put("ANDROID_HOME", sdkPath);
            env.put("ANDROID_SDK_ROOT", sdkPath);
            
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    count++;
                    System.out.println(GREEN + "  [" + count + "] " + RESET + line);
                }
            }
            
            if (count == 0) {
                System.out.println(YELLOW + "  No virtual devices found." + RESET);
                System.out.println("  Create one using option [2] or 'avidia create <name>'");
            }
            
            System.out.println(YELLOW + "================================================" + RESET);
            process.waitFor();
        } catch (Exception e) {
            System.err.println(RED + "Failed to list virtual devices: " + e.getMessage() + RESET);
        }
    }
    
    private static void showCLIMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            printHeader();
            System.out.println(BLUE + "================================================" + RESET);
            System.out.println(BOLD + "                 MAIN OPERATIONS                " + RESET);
            System.out.println(BLUE + "================================================" + RESET);
            System.out.println(GREEN + " [1]" + RESET + " List Virtual Devices");
            System.out.println("     Display all available Android Virtual Devices");
            System.out.println();
            System.out.println(GREEN + " [2]" + RESET + " Create New Device");
            System.out.println("     Configure and create a new Android Virtual Device");
            System.out.println();
            System.out.println(GREEN + " [3]" + RESET + " Start Virtual Device");
            System.out.println("     Launch an existing Android Virtual Device");
            System.out.println();
            System.out.println(GREEN + " [4]" + RESET + " Stop Virtual Device");
            System.out.println("     Stop a running Android Virtual Device");
            System.out.println();
            System.out.println(GREEN + " [5]" + RESET + " Remove Virtual Device");
            System.out.println("     Delete an Android Virtual Device permanently");
            System.out.println();
            System.out.println(GREEN + " [6]" + RESET + " Install System Image");
            System.out.println("     Install new Android system images for emulation");
            System.out.println();
            System.out.println(GREEN + " [7]" + RESET + " System Information");
            System.out.println("     View system configuration and environment status");
            System.out.println();
            System.out.println(GREEN + " [0]" + RESET + " Exit to Shell");
            System.out.println(BLUE + "================================================" + RESET);
            System.out.print("\n" + CYAN + "Enter selection: " + RESET);
            
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    listAVDs();
                    waitForEnter(scanner);
                    break;
                case "2":
                    createAVDTUI(scanner);
                    break;
                case "3":
                    startAVDTUI(scanner);
                    break;
                case "4":
                    stopAVDTUI(scanner);
                    break;
                case "5":
                    deleteAVDTUI(scanner);
                    break;
                case "6":
                    installImageTUI(scanner);
                    break;
                case "7":
                    showSystemInfoCLI();
                    waitForEnter(scanner);
                    break;
                case "0":
                    System.out.println(CYAN + "\nReturning to Terminal..." + RESET);
                    running = false;
                    break;
                default:
                    System.out.println(RED + "Invalid selection! Please choose 0-7." + RESET);
                    waitForEnter(scanner);
            }
        }
        scanner.close();
    }
    
    private static void waitForEnter(Scanner scanner) {
        System.out.print("\n" + YELLOW + "Press Enter to continue..." + RESET);
        scanner.nextLine();
    }
    
    private static void createAVDTUI(Scanner scanner) {
        System.out.println(YELLOW + "\n================================================" + RESET);
        System.out.println(BOLD + "           CREATE VIRTUAL DEVICE              " + RESET);
        System.out.println(YELLOW + "================================================" + RESET);
        
        System.out.print("\n" + CYAN + "Virtual device name: " + RESET);
        String avdName = scanner.nextLine().trim();
        if (avdName.isEmpty()) {
            System.err.println(RED + "Device name cannot be empty!" + RESET);
            return;
        }
        
        System.out.println("\n" + CYAN + "Android Version:" + RESET);
        System.out.println("------------------------------------------------");
        for (Map.Entry<String, String> entry : ANDROID_VERSIONS.entrySet()) {
            System.out.printf("  %-25s (API %s)\n", entry.getKey(), entry.getValue());
        }
        
        System.out.print("\n" + CYAN + "Enter API level or number to select: " + RESET);
        String apiInput = scanner.nextLine().trim();
        String apiLevel = "34";
        
        // If input is a number, select by index
        if (apiInput.matches("\\d+")) {
            int index = Integer.parseInt(apiInput) - 1;
            if (index >= 0 && index < ANDROID_VERSIONS.size()) {
                apiLevel = (String) ANDROID_VERSIONS.values().toArray()[index];
            }
        } else if (!apiInput.isEmpty()) {
            // Try to find matching API level
            apiLevel = apiInput;
        }
        
        System.out.println("\n" + CYAN + "Architecture:" + RESET);
        System.out.println(" [1] x86_64 (Intel/AMD 64-bit, recommended)");
        System.out.println(" [2] x86 (Intel/AMD 32-bit)");
        System.out.println(" [3] arm64-v8a (ARM 64-bit, slower emulation)");
        System.out.print("Select [1]: " + RESET);
        String archChoice = scanner.nextLine().trim();
        String abi = "x86_64";
        if ("2".equals(archChoice)) abi = "x86";
        else if ("3".equals(archChoice)) abi = "arm64-v8a";
        
        System.out.println("\n" + CYAN + "Image Type:" + RESET);
        System.out.println(" [1] Google Play Store (Recommended for app testing)");
        System.out.println(" [2] Google APIs (No Play Store, with Google Services)");
        System.out.println(" [3] AOSP (Android Open Source Project, no Google services)");
        System.out.print("Select [1]: " + RESET);
        String typeChoice = scanner.nextLine().trim();
        String imageType = "google_apis_playstore";
        if ("2".equals(typeChoice)) imageType = "google_apis";
        else if ("3".equals(typeChoice)) imageType = "android";
        
        String packageName = String.format("system-images;android-%s;%s;%s", apiLevel, imageType, abi);
        
        System.out.println("\n" + CYAN + "Device Model:" + RESET);
        System.out.println(" Available options:");
        for (int i = 0; i < DEVICE_DEFINITIONS.size(); i++) {
            System.out.printf("  [%d] %s\n", i+1, DEVICE_DEFINITIONS.get(i)[1]);
        }
        
        System.out.print("\nSelect device number [3 for Pixel 5]: " + RESET);
        String deviceChoice = scanner.nextLine().trim();
        String deviceId = "pixel_5";
        
        if (deviceChoice.matches("\\d+")) {
            int index = Integer.parseInt(deviceChoice) - 1;
            if (index >= 0 && index < DEVICE_DEFINITIONS.size()) {
                deviceId = DEVICE_DEFINITIONS.get(index)[0];
            }
        }
        
        // Check if system image is installed
        if (!isSystemImageInstalled(packageName)) {
            System.out.println(RED + "\nSystem image not installed!" + RESET);
            System.out.println("Package: " + packageName);
            System.out.print(YELLOW + "Do you want to install it now? (yes/no): " + RESET);
            String installChoice = scanner.nextLine().trim().toLowerCase();
            if (installChoice.equals("yes") || installChoice.equals("y")) {
                installSDKPackage(packageName);
            } else {
                System.out.println(YELLOW + "AVD creation cancelled." + RESET);
                return;
            }
        }
        
        System.out.println("\n" + CYAN + "================================================" + RESET);
        System.out.println(BOLD + "        CONFIGURATION SUMMARY             " + RESET);
        System.out.println(CYAN + "================================================" + RESET);
        System.out.println(" Name       : " + GREEN + avdName + RESET);
        System.out.println(" API Level  : " + GREEN + apiLevel + RESET);
        System.out.println(" Package    : " + GREEN + packageName + RESET);
        System.out.println(" Device     : " + GREEN + deviceId + RESET);
        System.out.println(CYAN + "================================================" + RESET);
        
        System.out.print("\n" + CYAN + "Create virtual device? (yes/no): " + RESET);
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("yes") || confirm.equals("y")) {
            createAVD(avdName, packageName, deviceId);
        } else {
            System.out.println(YELLOW + "Device creation cancelled." + RESET);
        }
    }
    
    private static void startAVDTUI(Scanner scanner) {
        List<String> avds = getAvailableAVDs();
        if (avds.isEmpty()) {
            System.out.println(RED + "No AVDs found. Create one first." + RESET);
            return;
        }
        
        System.out.println("\n" + CYAN + "Available AVDs:" + RESET);
        for (int i = 0; i < avds.size(); i++) {
            System.out.println(" [" + (i + 1) + "] " + avds.get(i));
        }
        
        System.out.print("\n" + CYAN + "Select AVD number: " + RESET);
        String choice = scanner.nextLine().trim();
        try {
            int idx = Integer.parseInt(choice) - 1;
            if (idx >= 0 && idx < avds.size()) {
                startAVD(avds.get(idx));
            } else {
                System.out.println(RED + "Invalid selection." + RESET);
            }
        } catch (NumberFormatException e) {
            System.out.println(RED + "Invalid number." + RESET);
        }
    }
    
    private static void stopAVDTUI(Scanner scanner) {
        List<String> runningAvds = getRunningAVDs();
        if (runningAvds.isEmpty()) {
            System.out.println(YELLOW + "No AVDs are currently running." + RESET);
            return;
        }
        
        System.out.println("\n" + CYAN + "Running AVDs:" + RESET);
        for (int i = 0; i < runningAvds.size(); i++) {
            System.out.println(" [" + (i + 1) + "] " + runningAvds.get(i));
        }
        
        System.out.print("\n" + CYAN + "Select AVD number to stop: " + RESET);
        String choice = scanner.nextLine().trim();
        try {
            int idx = Integer.parseInt(choice) - 1;
            if (idx >= 0 && idx < runningAvds.size()) {
                String avdName = runningAvds.get(idx).split(" ")[0];
                stopAVD(avdName);
            } else {
                System.out.println(RED + "Invalid selection." + RESET);
            }
        } catch (NumberFormatException e) {
            System.out.println(RED + "Invalid number." + RESET);
        }
    }
    
    private static void deleteAVDTUI(Scanner scanner) {
        List<String> avds = getAvailableAVDs();
        if (avds.isEmpty()) {
            System.out.println(RED + "No AVDs found." + RESET);
            return;
        }
        
        System.out.println("\n" + CYAN + "Available AVDs:" + RESET);
        for (int i = 0; i < avds.size(); i++) {
            System.out.println(" [" + (i + 1) + "] " + avds.get(i));
        }
        
        System.out.print("\n" + CYAN + "Select AVD number to delete: " + RESET);
        String choice = scanner.nextLine().trim();
        try {
            int idx = Integer.parseInt(choice) - 1;
            if (idx >= 0 && idx < avds.size()) {
                String avdName = avds.get(idx);
                
                // Check if AVD is running
                if (isAVDRunning(avdName)) {
                    System.out.println(RED + "WARNING: This AVD is currently running!" + RESET);
                    System.out.print(YELLOW + "Do you want to stop and delete it? (yes/no): " + RESET);
                    String confirm = scanner.nextLine().trim().toLowerCase();
                    if (!confirm.equals("yes") && !confirm.equals("y")) {
                        System.out.println(YELLOW + "Deletion cancelled." + RESET);
                        return;
                    }
                    stopAVD(avdName);
                }
                
                System.out.println("\n" + RED + "WARNING: This will permanently delete all data associated with this AVD!" + RESET);
                System.out.print(YELLOW + "Are you sure you want to delete '" + avdName + "'? (yes/no): " + RESET);
                String confirm = scanner.nextLine().trim().toLowerCase();
                if (confirm.equals("yes") || confirm.equals("y")) {
                    deleteAVD(avdName);
                } else {
                    System.out.println(YELLOW + "Deletion cancelled." + RESET);
                }
            } else {
                System.out.println(RED + "Invalid selection." + RESET);
            }
        } catch (NumberFormatException e) {
            System.out.println(RED + "Invalid number." + RESET);
        }
    }
    
    private static void installImageTUI(Scanner scanner) {
        System.out.println(YELLOW + "\n================================================" + RESET);
        System.out.println(BOLD + "        INSTALL SYSTEM IMAGE                  " + RESET);
        System.out.println(YELLOW + "================================================" + RESET);
        
        System.out.println("\n" + CYAN + "Available Android Versions:" + RESET);
        int index = 1;
        for (Map.Entry<String, String> entry : ANDROID_VERSIONS.entrySet()) {
            System.out.printf("  [%d] %-25s (API %s)\n", index++, entry.getKey(), entry.getValue());
        }
        
        System.out.print("\n" + CYAN + "Select version number or API level [1]: " + RESET);
        String versionInput = scanner.nextLine().trim();
        String apiLevel = "34";
        
        if (versionInput.matches("\\d+")) {
            int versionIndex = Integer.parseInt(versionInput) - 1;
            if (versionIndex >= 0 && versionIndex < ANDROID_VERSIONS.size()) {
                apiLevel = (String) ANDROID_VERSIONS.values().toArray()[versionIndex];
            }
        } else if (!versionInput.isEmpty()) {
            apiLevel = versionInput;
        }
        
        System.out.println("\n" + CYAN + "Image Types:" + RESET);
        System.out.println(" [1] Google Play Store (Recommended for app testing)");
        System.out.println(" [2] Google APIs (No Play Store, with Google Services)");
        System.out.println(" [3] AOSP (Android Open Source Project, no Google services)");
        System.out.print("Select [1]: " + RESET);
        String typeChoice = scanner.nextLine().trim();
        String imageType = "google_apis_playstore";
        if ("2".equals(typeChoice)) imageType = "google_apis";
        else if ("3".equals(typeChoice)) imageType = "android";
        
        System.out.println("\n" + CYAN + "Architecture:" + RESET);
        System.out.println(" [1] x86_64 (Intel/AMD 64-bit, recommended)");
        System.out.println(" [2] x86 (Intel/AMD 32-bit)");
        System.out.println(" [3] arm64-v8a (ARM 64-bit, slower emulation)");
        System.out.print("Select [1]: " + RESET);
        String archChoice = scanner.nextLine().trim();
        String abi = "x86_64";
        if ("2".equals(archChoice)) abi = "x86";
        else if ("3".equals(archChoice)) abi = "arm64-v8a";
        
        String packageName = String.format("system-images;android-%s;%s;%s", apiLevel, imageType, abi);
        
        System.out.println("\n" + CYAN + "Package to install:" + RESET);
        System.out.println(" " + packageName);
        
        System.out.print("\n" + CYAN + "Proceed with installation? (yes/no): " + RESET);
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("yes") || confirm.equals("y")) {
            installSDKPackage(packageName);
        } else {
            System.out.println(YELLOW + "Installation cancelled." + RESET);
        }
    }
    
    private static void showSystemInfoCLI() {
        printHeader();
        System.out.println(BOLD + "System Information:" + RESET);
        System.out.println("===================");
        System.out.println();
        System.out.println("AVIDIA Status:    Operational");
        System.out.println("Android SDK:      " + sdkPath);
        System.out.println();
        
        // Java info
        System.out.println("Java Version:");
        try {
            Process process = Runtime.getRuntime().exec("java -version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("  " + line);
            }
        } catch (Exception e) {
            System.out.println(RED + "  Error retrieving Java version: " + e.getMessage() + RESET);
        }
        System.out.println();
        
        // KVM status
        System.out.println("KVM Status:");
        File kvm = new File("/dev/kvm");
        if (kvm.exists()) {
            System.out.println(GREEN + "  Available (Hardware acceleration enabled)" + RESET);
        } else {
            System.out.println(RED + "  Not available (Software acceleration only)" + RESET);
            System.out.println("  Run 'avidia suggest-kvm' for installation instructions");
        }
        System.out.println();
        
        // AVD count
        List<String> avds = getAvailableAVDs();
        System.out.println("Virtual Devices:");
        System.out.println("  Total AVDs: " + avds.size());
        if (!avds.isEmpty()) {
            for (String avd : avds) {
                System.out.println("    - " + avd);
            }
        }
    }
    
    private static boolean validateEnvironment() {
        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome == null || androidHome.isEmpty()) {
            androidHome = "/usr/share/android";
        }
        
        sdkPath = androidHome;
        File sdkDir = new File(sdkPath);
        if (!sdkDir.exists()) {
            System.err.println(RED + "Android SDK directory not found: " + sdkPath + RESET);
            System.err.println("Run 'sudo avidia initiate' to initiate Android SDK system-wide.");
            return false;
        }
        
        // Check required tools
        File emulator = new File(sdkPath + "/emulator/emulator");
        File avdmanager = new File(sdkPath + "/cmdline-tools/latest/bin/avdmanager");
        File sdkmanager = new File(sdkPath + "/cmdline-tools/latest/bin/sdkmanager");
        
        if (!emulator.exists() || !emulator.canExecute()) {
            System.err.println(RED + "Emulator not found or not executable at: " + emulator.getAbsolutePath() + RESET);
            return false;
        }
        
        if (!avdmanager.exists() || !avdmanager.canExecute()) {
            System.err.println(RED + "AVD Manager not found or not executable at: " + avdmanager.getAbsolutePath() + RESET);
            return false;
        }
        
        if (!sdkmanager.exists() || !sdkmanager.canExecute()) {
            System.err.println(RED + "SDK Manager not found or not executable at: " + sdkmanager.getAbsolutePath() + RESET);
            return false;
        }
        
        return true;
    }
    
    private static void printHeader() {
        System.out.println(CYAN + BOLD);
        System.out.println("================================================");
        System.out.println("                    AVIDIA                      ");
        System.out.println("================================================");
        System.out.println(RESET);
    }
}
