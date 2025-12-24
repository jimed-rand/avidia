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
        
        sdkPath = androidHome;

        if (sdkPath == null || sdkPath.isEmpty()) {
            System.err.println(RED + "Android SDK environment not configured!" + RESET);
            System.err.println("ANDROID_HOME environment variable is not set.");
            System.err.println("Run 'sudo avidia initiate' to initiate Android SDK system-wide.");
            return false;
        }

        File sdkDir = new File(sdkPath);
        if (!sdkDir.exists()) {
            System.err.println(RED + "Android SDK directory not found: " + sdkPath + RESET);
            System.err.println("Run 'sudo avidia initiate' to initiate Android SDK.");
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

        System.out.print("\nVirtual device name: ");
        String avdName = scanner.nextLine().trim();

        if (avdName.isEmpty()) {
            System.err.println(RED + "Device name cannot be empty!" + RESET);
            return;
        }

        System.out.println("\n" + CYAN + "Select device profile:" + RESET);
        System.out.println(" [1] Phone");
        System.out.println(" [2] Tablet");
        System.out.println(" [3] Android TV");
        System.out.println(" [4] wearOS");
        System.out.println(" [5] Android Auto");
        System.out.print("Selection [1]: ");
        String typeChoice = scanner.nextLine().trim();

        String deviceProfile = "phone";
        switch (typeChoice) {
            case "2": deviceProfile = "tablet"; break;
            case "3": deviceProfile = "tv_"; break;
            case "4": deviceProfile = "wearos"; break;
            case "5": deviceProfile = "automotive"; break;
        }

        System.out.println("\n" + CYAN + "Android API level:" + RESET);
        System.out.println(" Recommended: 34 (Android 14)");
        System.out.print("API level [34]: ");
        String apiLevel = scanner.nextLine().trim();
        if (apiLevel.isEmpty()) apiLevel = "34";

        System.out.println("\n" + CYAN + "System image architecture:" + RESET);
        System.out.println(" [1] x86_64 (Intel/AMD, faster with KVM)");
        System.out.println(" [2] arm64-v8a (ARM hosts)");
        System.out.print("Architecture [1]: ");
        String abiChoice = scanner.nextLine().trim();
        String abi = abiChoice.equals("2") ? "arm64-v8a" : "x86_64";

        System.out.println("\n" + CYAN + "Device storage:" + RESET);
        System.out.print("Internal storage (MB) [4096]: ");
        String storage = scanner.nextLine().trim();
        if (storage.isEmpty()) storage = "4096";

        System.out.println("\n" + CYAN + "================================================" + RESET);
        System.out.println(BOLD + "        CONFIGURATION SUMMARY             " + RESET);
        System.out.println(CYAN + "================================================" + RESET);
        System.out.println(" Name       : " + GREEN + avdName + RESET);
        System.out.println(" Device     : " + GREEN + deviceProfile + RESET);
        System.out.println(" API Level  : " + GREEN + apiLevel + RESET);
        System.out.println(" Architecture: " + GREEN + abi + RESET);
        System.out.println(" Storage    : " + GREEN + storage + " MB" + RESET);
        System.out.println(CYAN + "================================================" + RESET);

        System.out.print("\nCreate virtual device? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("yes") || confirm.equals("y")) {
            createAVDProcess(avdName, apiLevel, abi, deviceProfile, storage);
        } else {
            System.out.println(YELLOW + "Device creation cancelled." + RESET);
        }
    }

    private static void createAVDProcess(String avdName, String apiLevel, String abi, String deviceProfile, String storage) {
        System.out.println(CYAN + "\nCreating virtual device: " + avdName + RESET);

        try {
            String packageName = String.format("system-images;android-%s;google_apis;%s", apiLevel, abi);

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
            command.add(deviceProfile);
            command.add("--sdcard");
            command.add(storage + "M");

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
        System.out.print("\nEnter virtual device name: ");
        String avdName = scanner.nextLine().trim();
        startAVDByName(avdName);
    }

    private static void startAVDByName(String avdName) {
        System.out.println(CYAN + "\nStarting virtual device: " + avdName + RESET);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                sdkPath + "/emulator/emulator",
                "-avd", avdName,
                "-no-snapshot-load",
                "-no-audio"
            );

            pb.inheritIO();
            Process process = pb.start();

            System.out.println(GREEN + "Virtual device started. Press Ctrl+C to stop." + RESET);
            process.waitFor();

        } catch (Exception e) {
            System.err.println(RED + "Failed to start virtual device: " + e.getMessage() + RESET);
        }
    }

    private static void deleteAVDTUI(Scanner scanner) {
        System.out.print("\nEnter virtual device name to delete: ");
        String avdName = scanner.nextLine().trim();

        System.out.print(RED + "Confirm deletion? (yes/no): " + RESET);
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("yes")) {
            deleteAVDByName(avdName);
        } else {
            System.out.println(YELLOW + "Deletion cancelled." + RESET);
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

        System.out.print("\nEnter API level (e.g., 34 for Android 14): ");
        String apiLevel = scanner.nextLine().trim();

        System.out.print("Enter architecture (x86_64 or arm64-v8a) [x86_64]: ");
        String abi = scanner.nextLine().trim();
        if (abi.isEmpty()) abi = "x86_64";

        String packageName = String.format("system-images;android-%s;google_apis;%s", apiLevel, abi);
        
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
        
        System.out.println("\n" + CYAN + "Java Development Kit:" + RESET);
        System.out.println(" Version: " + GREEN + System.getProperty("java.version") + RESET);
        System.out.println(" Vendor: " + GREEN + System.getProperty("java.vendor") + RESET);
        
        System.out.println("\n" + CYAN + "Operating System:" + RESET);
        System.out.println(" OS: " + GREEN + System.getProperty("os.name") + RESET);
        System.out.println(" Architecture: " + GREEN + System.getProperty("os.arch") + RESET);
        
        System.out.println(YELLOW + "================================================" + RESET);
    }
}
