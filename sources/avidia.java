package org.jimedrand.avidia;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class avidia {

    private static final String RESET = "\033[0m";
    private static final String RED = "\033[0;31m";
    private static final String GREEN = "\033[0;32m";
    private static final String YELLOW = "\033[0;33m";
    private static final String BLUE = "\033[0;34m";
    private static final String CYAN = "\033[0;36m";
    private static final String BOLD = "\033[1m";
    private static String sdkPath;

    // Android versions and their API levels
    private static final Map<String, String> ANDROID_VERSIONS = new LinkedHashMap<String, String>() {{
        put("1.0", "1"); put("1.1", "2"); put("1.5", "3"); put("1.6", "4");
        put("2.0", "5"); put("2.0.1", "6"); put("2.1", "7"); put("2.2", "8");
        put("2.3", "9"); put("2.3.3", "10"); put("3.0", "11"); put("3.1", "12");
        put("3.2", "13"); put("4.0", "14"); put("4.0.3", "15"); put("4.1", "16");
        put("4.2", "17"); put("4.3", "18"); put("4.4", "19"); put("4.4W", "20");
        put("5.0", "21"); put("5.1", "22"); put("6.0", "23"); put("7.0", "24");
        put("7.1", "25"); put("8.0", "26"); put("8.1", "27"); put("9", "28");
        put("10", "29"); put("11", "30"); put("12", "31"); put("12L", "32");
        put("13", "33"); put("14", "34"); put("15", "35"); put("16", "36");
    }};

    // Device profiles
    private static final Map<String, String> DEVICE_PROFILES = new LinkedHashMap<String, String>() {{
        // Phones
        put("Pixel 8 Pro", "pixel_8_pro");
        put("Pixel 8", "pixel_8");
        put("Pixel 7 Pro", "pixel_7_pro");
        put("Pixel 7", "pixel_7");
        put("Pixel 6 Pro", "pixel_6_pro");
        put("Pixel 6", "pixel_6");
        put("Pixel 5", "pixel_5");
        put("Pixel 4 XL", "pixel_4_xl");
        put("Pixel 4", "pixel_4");
        put("Pixel 3 XL", "pixel_3_xl");
        put("Pixel 3", "pixel_3");
        put("Pixel 2 XL", "pixel_2_xl");
        put("Pixel 2", "pixel_2");
        put("Pixel XL", "pixel_xl");
        put("Pixel", "pixel");

        // Nexus devices
        put("Nexus 6P", "Nexus_6P");
        put("Nexus 6", "Nexus_6");
        put("Nexus 5X", "Nexus_5X");
        put("Nexus 5", "Nexus_5");
        put("Nexus 4", "Nexus_4");

        // Tablets
        put("Nexus 10", "Nexus_10");
        put("Nexus 9", "Nexus_9");
        put("Nexus 7 (2013)", "Nexus_7_2013");
        put("Nexus 7 (2012)", "Nexus_7");

        // Wear OS
        put("Wear OS Square", "wearos_square");
        put("Wear OS Round", "wearos_round");

        // Android TV
        put("Android TV (1080p)", "tv_1080p");
        put("Android TV (720p)", "tv_720p");

        // Automotive
        put("Android Automotive", "automotive_1024p_landscape");

        // Custom device ID (user can enter their own)
        put("Custom (enter device ID)", "custom");
    }};

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Internal Error: This program must be called from Avidia bash");
            System.exit(1);
        }

        String mode = args[0];

        if (!validateEnvironment()) {
            System.exit(1);
        }

        switch (mode) {
            case "tui":
                showTUI();
                break;
            case "list":
                listAVDs();
                break;
            case "create":
                if (args.length < 6) {
                    System.err.println("Usage: create <name> <api> <abi> <device> <storage>");
                    System.exit(1);
                }
                createAVDProcess(args[1], args[2], args[3], args[4], args[5]);
                break;
            case "start":
                if (args.length < 2) {
                    System.err.println("Usage: start <name>");
                    System.exit(1);
                }
                startAVDByName(args[1]);
                break;
            case "delete":
                if (args.length < 2) {
                    System.err.println("Usage: delete <name>");
                    System.exit(1);
                }
                deleteAVDByName(args[1]);
                break;
            case "install-sdk":
                if (args.length < 2) {
                    System.err.println("Usage: install-sdk <package>");
                    System.exit(1);
                }
                installSDKPackageProcess(args[1]);
                break;
            case "info":
                showSystemInfo();
                break;
            case "sdk-manager":
                if (args.length < 2) {
                    System.err.println("Usage: sdk-manager <command>");
                    System.exit(1);
                }
                executeSDKManager(args[1]);
                break;
            default:
                System.err.println("Unknown operation: " + mode);
                System.exit(1);
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

        File sdkManager = new File(sdkPath + "/cmdline-tools/latest/bin/sdkmanager");
        if (!sdkManager.exists()) {
            System.err.println(RED + "Android SDK Manager not found. Please run 'sudo avidia initiate'" + RESET);
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

    private static void showTUI() {
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
            System.out.println(GREEN + " [4]" + RESET + " Remove Virtual Device");
            System.out.println("     Delete an Android Virtual Device permanently");
            System.out.println();
            System.out.println(GREEN + " [5]" + RESET + " SDK Package Manager");
            System.out.println("     Manage Android SDK packages and system images");
            System.out.println();
            System.out.println(GREEN + " [6]" + RESET + " Install Additional SDK");
            System.out.println("     Add new Android SDK platforms and system images");
            System.out.println();
            System.out.println(GREEN + " [7]" + RESET + " System Information");
            System.out.println("     View system configuration and environment status");
            System.out.println();
            System.out.println(GREEN + " [0]" + RESET + " Exit to Shell");
            System.out.println(BLUE + "================================================" + RESET);
            System.out.print("\nEnter selection: ");

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
                    deleteAVDTUI(scanner);
                    break;
                case "5":
                    manageSDKTUI(scanner);
                    break;
                case "6":
                    installAdditionalSDKTUI(scanner);
                    break;
                case "7":
                    showSystemInfo();
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
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private static void listAVDs() {
        System.out.println(YELLOW + "\n================================================" + RESET);
        System.out.println(BOLD + "           AVAILABLE VIRTUAL DEVICES           " + RESET);
        System.out.println(YELLOW + "================================================" + RESET);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/emulator/emulator", "-list-avds"
            );
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                count++;
                System.out.println(GREEN + "  [" + count + "] " + RESET + line);
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

    private static void createAVDTUI(Scanner scanner) {
        System.out.println(YELLOW + "\n================================================" + RESET);
        System.out.println(BOLD + "           CREATE VIRTUAL DEVICE              " + RESET);
        System.out.println(YELLOW + "================================================" + RESET);

        // Device Name
        System.out.print("\nVirtual device name: ");
        String avdName = scanner.nextLine().trim();

        if (avdName.isEmpty()) {
            System.err.println(RED + "Device name cannot be empty!" + RESET);
            return;
        }

        // Android Version Selection
        System.out.println("\n" + CYAN + "Android Version (API Level):" + RESET);
        System.out.println("------------------------------------------------");
        int i = 1;
        for (Map.Entry<String, String> entry : ANDROID_VERSIONS.entrySet()) {
            System.out.printf(" [%2d] Android %-6s (API %s)\n", i, entry.getKey(), entry.getValue());
            i++;
        }
        System.out.print("\nSelect version [28 for Android 9]: ");
        String versionChoice = scanner.nextLine().trim();

        String apiLevel = "28"; // Default
        if (!versionChoice.isEmpty()) {
            try {
                int choice = Integer.parseInt(versionChoice);
                int idx = 1;
                for (Map.Entry<String, String> entry : ANDROID_VERSIONS.entrySet()) {
                    if (idx == choice) {
                        apiLevel = entry.getValue();
                        System.out.println(GREEN + "Selected: Android " + entry.getKey() + " (API " + apiLevel + ")" + RESET);
                        break;
                    }
                    idx++;
                }
            } catch (NumberFormatException e) {
                System.out.println(YELLOW + "Using default: Android 9 (API 28)" + RESET);
            }
        }

        // System Image Type (GMS vs AOSP)
        System.out.println("\n" + CYAN + "System Image Type:" + RESET);
        System.out.println(" [1] GMS (Google Mobile Services) - Includes Google Play Store");
        System.out.println(" [2] Google APIs - Android with Google APIs");
        System.out.println(" [3] AOSP (Android Open Source Project) - Pure Android");
        System.out.println(" [4] Google Play Intel x86 Atom System Image");
        System.out.println(" [5] Android TV");
        System.out.println(" [6] Wear OS");
        System.out.println(" [7] Android Automotive");
        System.out.print("Select image type [1]: ");
        String imageChoice = scanner.nextLine().trim();

        String imageType = "google_apis_playstore"; // Default GMS
        switch (imageChoice) {
            case "2":
                imageType = "google_apis";
                break;
            case "3":
                imageType = "android";
                break;
            case "4":
                imageType = "google_apis_playstore";
                break;
            case "5":
                imageType = "android-tv";
                break;
            case "6":
                imageType = "android-wear";
                break;
            case "7":
                imageType = "android-automotive";
                break;
        }

        // Architecture
        System.out.println("\n" + CYAN + "System Image Architecture:" + RESET);
        System.out.println(" [1] x86_64 (Intel/AMD, faster with KVM)");
        System.out.println(" [2] x86 (Intel/AMD 32-bit)");
        System.out.println(" [3] arm64-v8a (ARM hosts)");
        System.out.println(" [4] armeabi-v7a (ARM 32-bit)");
        System.out.print("Architecture [1]: ");
        String abiChoice = scanner.nextLine().trim();
        String abi = "x86_64";
        switch (abiChoice) {
            case "2":
                abi = "x86";
                break;
            case "3":
                abi = "arm64-v8a";
                break;
            case "4":
                abi = "armeabi-v7a";
                break;
        }

        // Device Profile
        System.out.println("\n" + CYAN + "Device Profile:" + RESET);
        System.out.println("------------------------------------------------");
        int j = 1;
        for (Map.Entry<String, String> entry : DEVICE_PROFILES.entrySet()) {
            if (j <= 20) { // Show first 20 devices
                System.out.printf(" [%2d] %s\n", j, entry.getKey());
            }
            j++;
        }
        System.out.println(" [99] Enter custom device ID");
        System.out.print("\nSelect device profile [14 for Pixel 4]: ");
        String deviceChoice = scanner.nextLine().trim();

        String deviceId = "pixel_4"; // Default
        if (deviceChoice.equals("99")) {
            System.out.print("Enter custom device ID (e.g., 'Nexus_5'): ");
            deviceId = scanner.nextLine().trim();
        } else if (!deviceChoice.isEmpty()) {
            try {
                int choice = Integer.parseInt(deviceChoice);
                int idx = 1;
                for (Map.Entry<String, String> entry : DEVICE_PROFILES.entrySet()) {
                    if (idx == choice) {
                        deviceId = entry.getValue();
                        System.out.println(GREEN + "Selected: " + entry.getKey() + RESET);
                        break;
                    }
                    idx++;
                }
            } catch (NumberFormatException e) {
                System.out.println(YELLOW + "Using default: Pixel 4" + RESET);
            }
        }

        // Storage
        System.out.println("\n" + CYAN + "Device Storage:" + RESET);
        System.out.print("Internal storage (MB) [4096]: ");
        String storage = scanner.nextLine().trim();
        if (storage.isEmpty()) storage = "4096";

        // RAM
        System.out.print("Device RAM (MB) [2048]: ");
        String ram = scanner.nextLine().trim();
        if (ram.isEmpty()) ram = "2048";

        // Summary
        System.out.println("\n" + CYAN + "================================================" + RESET);
        System.out.println(BOLD + "        CONFIGURATION SUMMARY             " + RESET);
        System.out.println(CYAN + "================================================" + RESET);
        System.out.println(" Name       : " + GREEN + avdName + RESET);
        System.out.println(" API Level  : " + GREEN + apiLevel + RESET);
        System.out.println(" Image Type : " + GREEN + imageType + RESET);
        System.out.println(" Architecture: " + GREEN + abi + RESET);
        System.out.println(" Device     : " + GREEN + deviceId + RESET);
        System.out.println(" Storage    : " + GREEN + storage + " MB" + RESET);
        System.out.println(" RAM        : " + GREEN + ram + " MB" + RESET);
        System.out.println(CYAN + "================================================" + RESET);

        System.out.print("\nCreate virtual device? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("yes") || confirm.equals("y")) {
            createAVDProcess(avdName, apiLevel, abi, imageType, storage, deviceId, ram);
        } else {
            System.out.println(YELLOW + "Device creation cancelled." + RESET);
        }
    }

    private static void createAVDProcess(String avdName, String apiLevel, String abi, String imageType, String storage, String deviceId, String ram) {
        System.out.println(CYAN + "\nCreating virtual device: " + avdName + RESET);

        try {
            String packageName = String.format("system-images;android-%s;%s;%s", apiLevel, imageType, abi);

            System.out.println(YELLOW + "Verifying system image..." + RESET);
            if (!isSystemImageInstalled(packageName)) {
                System.out.println(YELLOW + "Required system image not installed." + RESET);
                System.out.println("Install it first using: avidia install-sdk " + apiLevel + " " + abi);
                return;
            }

            List<String> command = new ArrayList<>();
            command.add(sdkPath + "/cmdline-tools/latest/bin/avdmanager");
            command.add("create");
            command.add("avd");
            command.add("--force");
            command.add("--name");
            command.add(avdName);
            command.add("--package");
            command.add(packageName);
            command.add("--device");
            command.add(deviceId);
            command.add("--sdcard");
            command.add(storage + "M");

            // Create config.ini with additional settings
            String avdDir = System.getProperty("user.home") + "/.android/avd/" + avdName + ".avd/";
            new File(avdDir).mkdirs();

            List<String> config = new ArrayList<>();
            config.add("avd.ini.encoding=UTF-8");
            config.add("hw.device.name=" + deviceId);
            config.add("hw.lcd.density=440");
            config.add("hw.ramSize=" + ram);
            config.add("hw.sdCard=yes");
            config.add("hw.gpu.enabled=yes");
            config.add("hw.gpu.mode=auto");
            config.add("hw.keyboard=yes");
            config.add("hw.mainKeys=no");
            config.add("hw.camera.back=emulated");
            config.add("hw.camera.front=emulated");

            Files.write(Paths.get(avdDir + "config.ini"), config);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            OutputStream os = process.getOutputStream();
            os.write("no\n".getBytes());
            os.flush();
            os.close();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("  " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println(GREEN + "\nVirtual device created successfully." + RESET);
                System.out.println("Start with: " + CYAN + "avidia start " + avdName + RESET);
            } else {
                System.err.println(RED + "Failed to create virtual device." + RESET);
            }

        } catch (Exception e) {
            System.err.println(RED + "Error during device creation: " + e.getMessage() + RESET);
        }
    }

    private static void createAVDProcess(String avdName, String apiLevel, String abi, String deviceProfile, String storage) {
        // Overload for backward compatibility
        createAVDProcess(avdName, apiLevel, abi, "google_apis", storage, deviceProfile, "2048");
    }

    private static boolean isSystemImageInstalled(String packageName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/cmdline-tools/latest/bin/sdkmanager",
                "--list_installed"
            );

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().contains(packageName)) {
                    return true;
                }
            }

            process.waitFor();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static void startAVDTUI(Scanner scanner) {
        List<String> avds = getAvailableAVDsList();
        if (avds.isEmpty()) {
            System.out.println(RED + "No AVDs found. Create one first." + RESET);
            return;
        }

        System.out.println("\n" + CYAN + "Available AVDs:" + RESET);
        for (int i = 0; i < avds.size(); i++) {
            System.out.println(" [" + (i + 1) + "] " + avds.get(i));
        }

        System.out.print("\nSelect AVD number: ");
        String choice = scanner.nextLine().trim();

        try {
            int idx = Integer.parseInt(choice) - 1;
            if (idx >= 0 && idx < avds.size()) {
                startAVDByName(avds.get(idx));
            } else {
                System.out.println(RED + "Invalid selection." + RESET);
            }
        } catch (NumberFormatException e) {
            System.out.println(RED + "Invalid number." + RESET);
        }
    }

    private static List<String> getAvailableAVDsList() {
        List<String> avds = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/emulator/emulator", "-list-avds"
            );
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
            System.err.println(RED + "Failed to list AVDs: " + e.getMessage() + RESET);
        }
        return avds;
    }

    private static void startAVDByName(String avdName) {
        System.out.println(CYAN + "\nStarting virtual device: " + avdName + RESET);
        System.out.println(YELLOW + "Press Ctrl+C to stop the emulator" + RESET);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/emulator/emulator",
                "-avd", avdName,
                "-no-snapshot-load",
                "-no-audio",
                "-gpu", "swiftshader_indirect"
            );

            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();

        } catch (Exception e) {
            System.err.println(RED + "Failed to start virtual device: " + e.getMessage() + RESET);
        }
    }

    private static void deleteAVDTUI(Scanner scanner) {
        List<String> avds = getAvailableAVDsList();
        if (avds.isEmpty()) {
            System.out.println(RED + "No AVDs found." + RESET);
            return;
        }

        System.out.println("\n" + CYAN + "Available AVDs:" + RESET);
        for (int i = 0; i < avds.size(); i++) {
            System.out.println(" [" + (i + 1) + "] " + avds.get(i));
        }

        System.out.print("\nSelect AVD number to delete: ");
        String choice = scanner.nextLine().trim();

        try {
            int idx = Integer.parseInt(choice) - 1;
            if (idx >= 0 && idx < avds.size()) {
                System.out.print(RED + "Confirm deletion of '" + avds.get(idx) + "'? (yes/no): " + RESET);
                String confirm = scanner.nextLine().trim().toLowerCase();

                if (confirm.equals("yes") || confirm.equals("y")) {
                    deleteAVDByName(avds.get(idx));
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

    private static void deleteAVDByName(String avdName) {
        System.out.println(CYAN + "\nDeleting virtual device: " + avdName + RESET);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/cmdline-tools/latest/bin/avdmanager",
                "delete", "avd",
                "-n", avdName
            );

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println(GREEN + "Virtual device deleted successfully." + RESET);
            } else {
                System.err.println(RED + "Failed to delete virtual device." + RESET);
            }

        } catch (Exception e) {
            System.err.println(RED + "Deletion error: " + e.getMessage() + RESET);
        }
    }

    private static void manageSDKTUI(Scanner scanner) {
        System.out.println(YELLOW + "\n================================================" + RESET);
        System.out.println(BOLD + "           SDK PACKAGE MANAGER               " + RESET);
        System.out.println(YELLOW + "================================================" + RESET);
        System.out.println(GREEN + " [1]" + RESET + " List installed packages");
        System.out.println(GREEN + " [2]" + RESET + " List available packages");
        System.out.println(GREEN + " [3]" + RESET + " Update all packages");
        System.out.println(GREEN + " [0]" + RESET + " Back to main menu");
        System.out.println(YELLOW + "================================================" + RESET);
        System.out.print("\nSelect operation: ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                executeSDKManager("--list_installed");
                break;
            case "2":
                executeSDKManager("--list");
                break;
            case "3":
                executeSDKManager("--update");
                break;
        }
    }

    private static void executeSDKManager(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/cmdline-tools/latest/bin/sdkmanager",
                command
            );

            pb.inheritIO();
            Process process = pb.start();

            if (!command.equals("--list") && !command.equals("--list_installed")) {
                OutputStream os = process.getOutputStream();
                os.write("y\n".getBytes());
                os.flush();
                os.close();
            }

            process.waitFor();

        } catch (Exception e) {
            System.err.println(RED + "SDK Manager error: " + e.getMessage() + RESET);
        }
    }

    private static void installAdditionalSDKTUI(Scanner scanner) {
        System.out.println(YELLOW + "\n================================================" + RESET);
        System.out.println(BOLD + "       INSTALL ADDITIONAL SDK PACKAGES        " + RESET);
        System.out.println(YELLOW + "================================================" + RESET);

        System.out.println("\n" + CYAN + "Available Android Versions (API Levels):" + RESET);
        int i = 1;
        for (Map.Entry<String, String> entry : ANDROID_VERSIONS.entrySet()) {
            System.out.printf(" [%2d] Android %-6s (API %s)\n", i, entry.getKey(), entry.getValue());
            i++;
        }

        System.out.print("\nSelect Android version number: ");
        String versionChoice = scanner.nextLine().trim();

        String apiLevel = "28";
        if (!versionChoice.isEmpty()) {
            try {
                int choice = Integer.parseInt(versionChoice);
                int idx = 1;
                for (Map.Entry<String, String> entry : ANDROID_VERSIONS.entrySet()) {
                    if (idx == choice) {
                        apiLevel = entry.getValue();
                        System.out.println(GREEN + "Selected: Android " + entry.getKey() + " (API " + apiLevel + ")" + RESET);
                        break;
                    }
                    idx++;
                }
            } catch (NumberFormatException e) {
                System.out.println(YELLOW + "Using default: Android 9 (API 28)" + RESET);
            }
        }

        System.out.println("\n" + CYAN + "System Image Type:" + RESET);
        System.out.println(" [1] GMS (Google Mobile Services)");
        System.out.println(" [2] Google APIs");
        System.out.println(" [3] AOSP");
        System.out.print("Select image type [1]: ");
        String imageChoice = scanner.nextLine().trim();

        String imageType = "google_apis_playstore";
        switch (imageChoice) {
            case "2":
                imageType = "google_apis";
                break;
            case "3":
                imageType = "android";
                break;
        }

        System.out.println("\n" + CYAN + "Architecture:" + RESET);
        System.out.println(" [1] x86_64");
        System.out.println(" [2] x86");
        System.out.println(" [3] arm64-v8a");
        System.out.println(" [4] armeabi-v7a");
        System.out.print("Select architecture [1]: ");
        String abiChoice = scanner.nextLine().trim();

        String abi = "x86_64";
        switch (abiChoice) {
            case "2":
                abi = "x86";
                break;
            case "3":
                abi = "arm64-v8a";
                break;
            case "4":
                abi = "armeabi-v7a";
                break;
        }

        String packageName = String.format("system-images;android-%s;%s;%s", apiLevel, imageType, abi);

        System.out.println("\nPackage to install: " + GREEN + packageName + RESET);

        System.out.print("\nProceed with installation? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("yes") || confirm.equals("y")) {
            installSDKPackageProcess(packageName);
        } else {
            System.out.println(YELLOW + "Installation cancelled." + RESET);
        }
    }

    private static void installSDKPackageProcess(String packageName) {
        System.out.println(CYAN + "\nInstalling package: " + packageName + RESET);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/cmdline-tools/latest/bin/sdkmanager",
                packageName
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            OutputStream os = process.getOutputStream();
            os.write("y\n".getBytes());
            os.flush();
            os.close();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("  " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println(GREEN + "\nPackage installed successfully." + RESET);
            } else {
                System.err.println(RED + "Package installation failed." + RESET);
            }

        } catch (Exception e) {
            System.err.println(RED + "Installation error: " + e.getMessage() + RESET);
        }
    }

    private static void showSystemInfo() {
        System.out.println(YELLOW + "\n================================================" + RESET);
        System.out.println(BOLD + "          SYSTEM INFORMATION               " + RESET);
        System.out.println(YELLOW + "================================================" + RESET);

        System.out.println(CYAN + "Android SDK:" + RESET);
        System.out.println(" Path: " + GREEN + sdkPath + RESET);

        System.out.println("\n" + CYAN + "Java Runtime:" + RESET);
        System.out.println(" Version: " + GREEN + System.getProperty("java.version") + RESET);
        System.out.println(" Vendor: " + GREEN + System.getProperty("java.vendor") + RESET);

        System.out.println("\n" + CYAN + "Operating System:" + RESET);
        System.out.println(" OS: " + GREEN + System.getProperty("os.name") + RESET);
        System.out.println(" Architecture: " + GREEN + System.getProperty("os.arch") + RESET);

        System.out.println("\n" + CYAN + "Environment:" + RESET);
        System.out.println(" User: " + GREEN + System.getProperty("user.name") + RESET);
        System.out.println(" Home: " + GREEN + System.getProperty("user.home") + RESET);

        // Check KVM
        File kvm = new File("/dev/kvm");
        System.out.println("\n" + CYAN + "KVM Acceleration:" + RESET);
        if (kvm.exists()) {
            System.out.println(" Status: " + GREEN + "Available" + RESET);
        } else {
            System.out.println(" Status: " + YELLOW + "Not available (install qemu-full)" + RESET);
        }

        System.out.println(YELLOW + "================================================" + RESET);
    }
}
