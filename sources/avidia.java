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
    private static String userName;

    // Android versions and their API levels
    private static final Map<String, String> ANDROID_VERSIONS = new LinkedHashMap<String, String>() {{
        put("16", "36");
        put("15", "35");
        put("14", "34");
        put("13", "33");
        put("12L", "32");
        put("12", "31");
        put("11", "30");
        put("10", "29");
        put("9", "28");
        put("8.1", "27");
        put("8.0", "26");
        put("7.1", "25");
        put("7.0", "24");
        put("6.0", "23");
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

        // Dapatkan username
        userName = System.getProperty("user.name");

        switch (mode) {
            case "tui":
                showTUI();
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
                if (args.length < 2) {
                    System.err.println("Usage: install-sdk <package>");
                    System.exit(1);
                }
                installSDKPackage(args[1]);
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
            System.out.println(GREEN + " [5]" + RESET + " Install System Image");
            System.out.println("     Install new Android system images for emulation");
            System.out.println();
            System.out.println(GREEN + " [6]" + RESET + " System Information");
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
                    installImageTUI(scanner);
                    break;
                case "6":
                    showSystemInfo();
                    waitForEnter(scanner);
                    break;
                case "0":
                    System.out.println(CYAN + "\nReturning to Terminal..." + RESET);
                    running = false;
                    break;
                default:
                    System.out.println(RED + "Invalid selection! Please choose 0-6." + RESET);
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

        System.out.print("\nVirtual device name: ");
        String avdName = scanner.nextLine().trim();

        if (avdName.isEmpty()) {
            System.err.println(RED + "Device name cannot be empty!" + RESET);
            return;
        }

        System.out.println("\n" + CYAN + "Android Version:" + RESET);
        System.out.println("------------------------------------------------");
        for (Map.Entry<String, String> entry : ANDROID_VERSIONS.entrySet()) {
            System.out.printf("  Android %-5s (API %s)\n", entry.getKey(), entry.getValue());
        }
        System.out.print("\nEnter API level (e.g., 34 for Android 14): ");
        String apiLevel = scanner.nextLine().trim();

        if (apiLevel.isEmpty()) {
            apiLevel = "34";
        }

        System.out.println("\n" + CYAN + "Architecture:" + RESET);
        System.out.println(" [1] x86_64 (Intel/AMD 64-bit, recommended)");
        System.out.println(" [2] x86 (Intel/AMD 32-bit)");
        System.out.print("Select [1]: ");
        String archChoice = scanner.nextLine().trim();
        String abi = archChoice.equals("2") ? "x86" : "x86_64";

        System.out.println("\n" + CYAN + "Image Type:" + RESET);
        System.out.println(" [1] Google APIs (with Google Play Services)");
        System.out.println(" [2] AOSP (Android Open Source Project)");
        System.out.print("Select [1]: ");
        String typeChoice = scanner.nextLine().trim();
        String imageType = typeChoice.equals("2") ? "android" : "google_apis";

        String packageName = String.format("system-images;android-%s;%s;%s", apiLevel, imageType, abi);

        System.out.println("\n" + CYAN + "Device Model:" + RESET);
        System.out.println(" Enter device ID (e.g., pixel_5, pixel_6, Nexus_5)");
        System.out.print("Device [pixel_5]: ");
        String deviceId = scanner.nextLine().trim();
        if (deviceId.isEmpty()) {
            deviceId = "pixel_5";
        }

        // Check if system image is installed
        if (!isSystemImageInstalled(packageName)) {
            System.out.println(RED + "\nSystem image not installed!" + RESET);
            System.out.println("Package: " + packageName);
            System.out.print("Do you want to install it now? (yes/no): ");
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

        System.out.print("\nCreate virtual device? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("yes") || confirm.equals("y")) {
            createAVD(avdName, packageName, deviceId);
        } else {
            System.out.println(YELLOW + "Device creation cancelled." + RESET);
        }
    }

    private static void createAVDTUI(String avdName) {
        // Simple create for command-line usage
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Creating AVD: " + avdName);
        System.out.print("Enter API level [34]: ");
        String apiLevel = scanner.nextLine().trim();
        if (apiLevel.isEmpty()) apiLevel = "34";
        
        String packageName = String.format("system-images;android-%s;google_apis;x86_64", apiLevel);
        
        System.out.print("Enter device ID [pixel_5]: ");
        String deviceId = scanner.nextLine().trim();
        if (deviceId.isEmpty()) deviceId = "pixel_5";
        
        createAVD(avdName, packageName, deviceId);
        scanner.close();
    }

    private static void createAVD(String avdName, String packageName, String deviceId) {
        System.out.println(CYAN + "\nCreating virtual device: " + avdName + RESET);

        try {
            // Check if system image is installed
            if (!isSystemImageInstalled(packageName)) {
                System.out.println(RED + "System image not installed!" + RESET);
                System.out.println("Package: " + packageName);
                System.out.println("Install it first with: avidia install-sdk " + 
                    packageName.replace("system-images;android-", "").split(";")[0]);
                return;
            }

            // Create AVD menggunakan avdmanager langsung
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
        List<String> avds = getAvailableAVDs();
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
                startAVD(avds.get(idx));
            } else {
                System.out.println(RED + "Invalid selection." + RESET);
            }
        } catch (NumberFormatException e) {
            System.out.println(RED + "Invalid number." + RESET);
        }
    }

    private static List<String> getAvailableAVDs() {
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
            command.add("host");
            command.add("-no-snapshot-load");
            
            if (kvm.exists()) {
                command.add("-qemu");
                command.add("-enable-kvm");
                System.out.println(GREEN + "KVM acceleration enabled" + RESET);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();

        } catch (Exception e) {
            System.err.println(RED + "Failed to start virtual device: " + e.getMessage() + RESET);
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

        System.out.print("\nSelect AVD number to delete: ");
        String choice = scanner.nextLine().trim();

        try {
            int idx = Integer.parseInt(choice) - 1;
            if (idx >= 0 && idx < avds.size()) {
                System.out.print(RED + "Confirm deletion of '" + avds.get(idx) + "'? (yes/no): " + RESET);
                String confirm = scanner.nextLine().trim().toLowerCase();

                if (confirm.equals("yes") || confirm.equals("y")) {
                    deleteAVD(avds.get(idx));
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

    private static void deleteAVD(String avdName) {
        System.out.println(CYAN + "\nDeleting virtual device: " + avdName + RESET);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/cmdline-tools/latest/bin/avdmanager",
                "delete", "avd",
                "-n", avdName
            );
            
            Map<String, String> env = pb.environment();
            env.put("ANDROID_HOME", sdkPath);
            env.put("ANDROID_SDK_ROOT", sdkPath);

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

    private static void installImageTUI(Scanner scanner) {
        System.out.println(YELLOW + "\n================================================" + RESET);
        System.out.println(BOLD + "       INSTALL SYSTEM IMAGE PACKAGE        " + RESET);
        System.out.println(YELLOW + "================================================" + RESET);

        System.out.println("\n" + CYAN + "Android Version:" + RESET);
        for (Map.Entry<String, String> entry : ANDROID_VERSIONS.entrySet()) {
            System.out.printf("  Android %-5s (API %s)\n", entry.getKey(), entry.getValue());
        }

        System.out.print("\nEnter API level (e.g., 34): ");
        String apiLevel = scanner.nextLine().trim();
        
        if (apiLevel.isEmpty()) {
            System.out.println(YELLOW + "Installation cancelled." + RESET);
            return;
        }

        System.out.println("\n" + CYAN + "Image Type:" + RESET);
        System.out.println(" [1] google_apis (with Google Play Services)");
        System.out.println(" [2] android (AOSP - pure Android)");
        System.out.println(" [3] google_apis_playstore (with Google Play Store)");
        System.out.print("Select [1]: ");
        String typeChoice = scanner.nextLine().trim();
        
        String imageType = "google_apis";
        if (typeChoice.equals("2")) {
            imageType = "android";
        } else if (typeChoice.equals("3")) {
            imageType = "google_apis_playstore";
        }

        System.out.println("\n" + CYAN + "Architecture:" + RESET);
        System.out.println(" [1] x86_64 (64-bit, recommended)");
        System.out.println(" [2] x86 (32-bit)");
        System.out.print("Select [1]: ");
        String archChoice = scanner.nextLine().trim();
        String abi = archChoice.equals("2") ? "x86" : "x86_64";

        String packageName = String.format("system-images;android-%s;%s;%s", apiLevel, imageType, abi);

        System.out.println("\nPackage to install: " + GREEN + packageName + RESET);
        System.out.print("\nProceed with installation? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("yes") || confirm.equals("y")) {
            installSDKPackage(packageName);
        } else {
            System.out.println(YELLOW + "Installation cancelled." + RESET);
        }
    }

    private static void installSDKPackage(String packageName) {
        System.out.println(CYAN + "\nInstalling package: " + packageName + RESET);
        System.out.println("This may take several minutes...");

        try {
            // Gunakan bash shell untuk menjalankan sdkmanager dengan environment yang tepat
            List<String> command = new ArrayList<>();
            command.add("bash");
            command.add("-c");
            command.add("export ANDROID_HOME='" + sdkPath + "' && " +
                       "export ANDROID_SDK_ROOT='" + sdkPath + "' && " +
                       "echo 'y' | '" + sdkPath + "/cmdline-tools/latest/bin/sdkmanager' --sdk_root='" + sdkPath + "' '" + packageName + "'");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

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
                System.err.println("Exit code: " + exitCode);
                
                // Coba alternatif: jalankan dengan sudo jika diperlukan
                System.out.println("\nTrying alternative installation method...");
                installSDKPackageAlternative(packageName);
            }

        } catch (Exception e) {
            System.err.println(RED + "Installation error: " + e.getMessage() + RESET);
        }
    }
    
    private static void installSDKPackageAlternative(String packageName) {
        try {
            // Coba jalankan melalui avidia bash script
            List<String> command = new ArrayList<>();
            command.add("avidia");
            command.add("install-sdk");
            
            // Parse package name untuk mendapatkan arguments
            // Format: system-images;android-34;google_apis;x86_64
            String[] parts = packageName.split(";");
            if (parts.length >= 2) {
                String apiLevel = parts[1].replace("android-", "");
                command.add(apiLevel);
                
                if (parts.length >= 3) {
                    command.add(parts[2]);
                }
                
                if (parts.length >= 4) {
                    command.add(parts[3]);
                }
            }
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
            
        } catch (Exception e) {
            System.err.println(RED + "Alternative installation also failed: " + e.getMessage() + RESET);
            System.err.println("Please run manually: sudo avidia install-sdk <api-level>");
        }
    }

    private static void showSystemInfo() {
        System.out.println(YELLOW + "\n================================================" + RESET);
        System.out.println(BOLD + "          SYSTEM INFORMATION               " + RESET);
        System.out.println(YELLOW + "================================================" + RESET);

        System.out.println(CYAN + "Android SDK:" + RESET);
        System.out.println(" Path: " + GREEN + sdkPath + RESET);
        System.out.println(" User: " + GREEN + userName + RESET);

        System.out.println("\n" + CYAN + "Java Runtime:" + RESET);
        System.out.println(" Version: " + GREEN + System.getProperty("java.version") + RESET);
        System.out.println(" Vendor: " + GREEN + System.getProperty("java.vendor") + RESET);

        System.out.println("\n" + CYAN + "Operating System:" + RESET);
        System.out.println(" OS: " + GREEN + System.getProperty("os.name") + RESET);
        System.out.println(" Architecture: " + GREEN + System.getProperty("os.arch") + RESET);

        System.out.println("\n" + CYAN + "Environment:" + RESET);
        System.out.println(" ANDROID_HOME: " + GREEN + System.getenv("ANDROID_HOME") + RESET);
        System.out.println(" ANDROID_SDK_ROOT: " + GREEN + System.getenv("ANDROID_SDK_ROOT") + RESET);

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
