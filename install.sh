#!/bin/bash
set -euo pipefail

# ================================================
#               AVIDIA INSTALLER
# ================================================

# Version
AVIDIA_VERSION="1.0.0"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BOLD='\033[1m'
UNDERLINE='\033[4m'
NC='\033[0m' # No Color

# Installation directories
AVIDIA_HOME="$HOME/.avidia"
AVIDIA_BIN_DIR="$HOME/.local/bin"
AVIDIA_SDK_DIR="$AVIDIA_HOME/sdk"
AVIDIA_AVD_DIR="$AVIDIA_HOME/avd"
AVIDIA_LIB_DIR="$AVIDIA_HOME/lib"
AVIDIA_SCRIPT_PATH="$AVIDIA_BIN_DIR/avidia"

# Java requirements
JAVA_MIN_VERSION="11"

print_header() {
    echo -e "${CYAN}${BOLD}"
    echo "================================================"
    echo "              AVIDIA INSTALLER                  "
    echo "              Version $AVIDIA_VERSION                "
    echo "================================================"
    echo -e "${NC}"
}

print_success() {
    echo -e "${GREEN}[✓] $1${NC}"
}

print_error() {
    echo -e "${RED}[✗] $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}[!] $1${NC}"
}

print_info() {
    echo -e "${BLUE}[i] $1${NC}"
}

print_step() {
    echo -e "${MAGENTA}➜ $1${NC}"
}

check_java() {
    print_step "Checking Java installation..."

    if ! command -v java &> /dev/null; then
        print_error "Java not found!"
        echo ""
        echo "Please install Java $JAVA_MIN_VERSION or higher:"
        echo ""
        detect_package_manager_and_suggest
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)

    if [ "$JAVA_VERSION" -lt "$JAVA_MIN_VERSION" ]; then
        print_error "Java version $JAVA_VERSION is too old. Need at least Java $JAVA_MIN_VERSION"
        exit 1
    fi

    print_success "Java $JAVA_VERSION found"
}

detect_package_manager_and_suggest() {
    if command -v apt-get &> /dev/null; then
        echo "  Ubuntu/Debian:"
        echo "    sudo apt update"
        echo "    sudo apt install openjdk-${JAVA_MIN_VERSION}-jdk"
    elif command -v dnf &> /dev/null; then
        echo "  Fedora/RHEL:"
        echo "    sudo dnf install java-${JAVA_MIN_VERSION}-openjdk-devel"
    elif command -v yum &> /dev/null; then
        echo "  CentOS/RHEL:"
        echo "    sudo yum install java-${JAVA_MIN_VERSION}-openjdk-devel"
    elif command -v pacman &> /dev/null; then
        echo "  Arch Linux:"
        echo "    sudo pacman -S jdk${JAVA_MIN_VERSION}-openjdk"
    elif command -v zypper &> /dev/null; then
        echo "  openSUSE:"
        echo "    sudo zypper install java-${JAVA_MIN_VERSION}-openjdk-devel"
    else
        echo "  Please install Java $JAVA_MIN_VERSION from: https://adoptium.net/"
    fi
}

check_dependencies() {
    print_step "Checking dependencies..."

    local missing_deps=()

    # Check for wget or curl
    if ! command -v wget &> /dev/null && ! command -v curl &> /dev/null; then
        missing_deps+=("wget or curl")
    fi

    # Check for unzip
    if ! command -v unzip &> /dev/null; then
        missing_deps+=("unzip")
    fi

    # Check for git (optional but recommended)
    if ! command -v git &> /dev/null; then
        print_warning "Git not found (optional, for updates)"
    fi

    if [ ${#missing_deps[@]} -gt 0 ]; then
        print_error "Missing dependencies: ${missing_deps[*]}"
        echo "Please install them and run this script again."
        exit 1
    fi

    print_success "All dependencies found"
}

setup_directories() {
    print_step "Setting up directories..."

    mkdir -p "$AVIDIA_HOME"
    mkdir -p "$AVIDIA_SDK_DIR"
    mkdir -p "$AVIDIA_AVD_DIR"
    mkdir -p "$AVIDIA_LIB_DIR"
    mkdir -p "$AVIDIA_BIN_DIR"

    # Create SDK subdirectories
    mkdir -p "$AVIDIA_SDK_DIR/cmdline-tools"
    mkdir -p "$AVIDIA_SDK_DIR/platform-tools"
    mkdir -p "$AVIDIA_SDK_DIR/emulator"
    mkdir -p "$AVIDIA_SDK_DIR/system-images"
    mkdir -p "$AVIDIA_SDK_DIR/platforms"
    mkdir -p "$AVIDIA_SDK_DIR/licenses"

    print_success "Directory structure created"
}

download_lanterna() {
    print_step "Downloading Lanterna library..."

    LANTERNA_VERSION="3.1.1"
    LANTERNA_URL="https://repo1.maven.org/maven2/com/googlecode/lanterna/lanterna/${LANTERNA_VERSION}/lanterna-${LANTERNA_VERSION}.jar"

    if command -v wget &> /dev/null; then
        wget -q --show-progress -O "$AVIDIA_LIB_DIR/lanterna.jar" "$LANTERNA_URL"
    elif command -v curl &> /dev/null; then
        curl -L --progress-bar -o "$AVIDIA_LIB_DIR/lanterna.jar" "$LANTERNA_URL"
    fi

    if [ -f "$AVIDIA_LIB_DIR/lanterna.jar" ]; then
        print_success "Lanterna downloaded successfully"
    else
        print_error "Failed to download Lanterna"
        exit 1
    fi
}

compile_avidia() {
    print_step "Compiling Avidia..."

    # Create source directory
    mkdir -p "$AVIDIA_HOME/src"

    # Find avidia.java in current directory or script directory
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    JAVA_SOURCE=""

    if [ -f "avidia.java" ]; then
        JAVA_SOURCE="avidia.java"
    elif [ -f "$SCRIPT_DIR/avidia.java" ]; then
        JAVA_SOURCE="$SCRIPT_DIR/avidia.java"
    else
        print_error "avidia.java not found!"
        echo "Please place avidia.java in the same directory as this script"
        exit 1
    fi

    # Copy source file
    cp "$JAVA_SOURCE" "$AVIDIA_HOME/src/"

    # Compile
    cd "$AVIDIA_HOME/src"

    # Extract package name
    PACKAGE_NAME=$(grep "^package " avidia.java | head -1 | cut -d' ' -f2 | tr -d ';')
    PACKAGE_PATH=$(echo "$PACKAGE_NAME" | tr '.' '/')

    # Create package directory structure
    mkdir -p "$AVIDIA_HOME/classes/$PACKAGE_PATH"

    # Compile with classpath
    print_info "Compiling Java source..."
    javac -cp "$AVIDIA_LIB_DIR/lanterna.jar" -d "$AVIDIA_HOME/classes" avidia.java

    if [ $? -eq 0 ]; then
        print_success "Compilation successful"
    else
        print_error "Compilation failed"
        exit 1
    fi

    # Create JAR file
    print_info "Creating JAR file..."
    cd "$AVIDIA_HOME/classes"
    jar cfe "$AVIDIA_LIB_DIR/avidia.jar" "$PACKAGE_NAME.avidia" .

    if [ -f "$AVIDIA_LIB_DIR/avidia.jar" ]; then
        print_success "JAR file created successfully"
    else
        print_error "Failed to create JAR file"
        exit 1
    fi

    # Clean up source files
    rm -rf "$AVIDIA_HOME/src"
    rm -rf "$AVIDIA_HOME/classes"
}

create_avidia_script() {
    print_step "Creating Avidia launcher script..."

    cat > "$AVIDIA_SCRIPT_PATH" << 'EOF'
#!/bin/bash
set -euo pipefail

# AVIDIA - Android Virtual Device Manager
# User-centric installation

AVIDIA_HOME="$HOME/.avidia"
AVIDIA_LIB="$AVIDIA_HOME/lib"

# Check if Avidia is installed
if [ ! -f "$AVIDIA_LIB/avidia.jar" ]; then
    echo -e "\033[0;31mError: Avidia not properly installed\033[0m"
    echo "Please run the installer again"
    exit 1
fi

if [ ! -f "$AVIDIA_LIB/lanterna.jar" ]; then
    echo -e "\033[0;31mError: Lanterna library not found\033[0m"
    echo "Please run the installer again"
    exit 1
fi

# Set environment variables
export ANDROID_SDK_ROOT="$AVIDIA_HOME/sdk"
export ANDROID_HOME="$AVIDIA_HOME/sdk"
export AVD_HOME="$AVIDIA_HOME/avd"

# Add SDK tools to PATH if they exist
if [ -d "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin" ]; then
    export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin"
fi

if [ -d "$ANDROID_SDK_ROOT/platform-tools" ]; then
    export PATH="$PATH:$ANDROID_SDK_ROOT/platform-tools"
fi

if [ -d "$ANDROID_SDK_ROOT/emulator" ]; then
    export PATH="$PATH:$ANDROID_SDK_ROOT/emulator"
fi

# Ensure directories exist
mkdir -p "$AVD_HOME"
mkdir -p "$ANDROID_SDK_ROOT"

# Run Java application
java -cp "$AVIDIA_LIB/avidia.jar:$AVIDIA_LIB/lanterna.jar" org.jimedrand.avidia.avidia "$@"
EOF

    chmod +x "$AVIDIA_SCRIPT_PATH"

    # Also create a simpler wrapper in Avidia home
    cat > "$AVIDIA_HOME/avidia" << 'EOF'
#!/bin/bash
exec "$HOME/.local/bin/avidia" "$@"
EOF

    chmod +x "$AVIDIA_HOME/avidia"

    print_success "Avidia script created"
}

setup_environment() {
    print_step "Setting up environment..."

    # Create environment file
    cat > "$AVIDIA_HOME/env.sh" << EOF
#!/bin/bash
# AVIDIA Environment Configuration
# Generated on: $(date)

export ANDROID_SDK_ROOT="$AVIDIA_SDK_DIR"
export ANDROID_HOME="$AVIDIA_SDK_DIR"
export AVD_HOME="$AVIDIA_AVD_DIR"

# Add to PATH if not already present
if [[ ":\$PATH:" != *":$AVIDIA_SDK_DIR/cmdline-tools/latest/bin:"* ]]; then
    export PATH="\$PATH:$AVIDIA_SDK_DIR/cmdline-tools/latest/bin"
fi

if [[ ":\$PATH:" != *":$AVIDIA_SDK_DIR/platform-tools:"* ]]; then
    export PATH="\$PATH:$AVIDIA_SDK_DIR/platform-tools"
fi

if [[ ":\$PATH:" != *":$AVIDIA_SDK_DIR/emulator:"* ]]; then
    export PATH="\$PATH:$AVIDIA_SDK_DIR/emulator"
fi

# Add .local/bin to PATH if not already present
if [[ ":\$PATH:" != *":$HOME/.local/bin:"* ]]; then
    export PATH="\$HOME/.local/bin:\$PATH"
fi

echo -e "\033[0;32m[AVIDIA] Environment loaded\033[0m"
echo "  SDK: \$ANDROID_SDK_ROOT"
echo "  AVD: \$AVD_HOME"
EOF

    chmod +x "$AVIDIA_HOME/env.sh"

    # Create configuration file
    cat > "$AVIDIA_HOME/avidia.conf" << EOF
# AVIDIA Configuration File
# Generated on $(date)

[core]
version = $AVIDIA_VERSION
home = $AVIDIA_HOME
sdk_path = $AVIDIA_SDK_DIR
avd_path = $AVIDIA_AVD_DIR

[environment]
android_sdk_root = $AVIDIA_SDK_DIR
android_home = $AVIDIA_SDK_DIR
avd_home = $AVIDIA_AVD_DIR

[performance]
kvm_acceleration = auto
ram_size = 4096
gpu_mode = host
cores = 2

[network]
proxy_host =
proxy_port =
timeout = 30

[logging]
level = INFO
file = $AVIDIA_HOME/avidia.log
max_size = 10MB
EOF

    print_success "Environment configured"
}

update_shell_config() {
    print_step "Updating shell configuration..."

    local shell_updated=false

    # Detect shell
    local user_shell=$(basename "$SHELL")

    case "$user_shell" in
        bash)
            local config_file="$HOME/.bashrc"
            ;;
        zsh)
            local config_file="$HOME/.zshrc"
            ;;
        fish)
            local config_file="$HOME/.config/fish/config.fish"
            ;;
        *)
            print_warning "Unknown shell: $user_shell"
            print_info "Please manually add ~/.local/bin to your PATH"
            return
            ;;
    esac

    # Add .local/bin to PATH if not already there
    if ! grep -q "\.local/bin" "$config_file" 2>/dev/null; then
        echo '' >> "$config_file"
        echo '# Add user bin directory to PATH' >> "$config_file"
        echo 'export PATH="$HOME/.local/bin:$PATH"' >> "$config_file"
        shell_updated=true
    fi

    # Add Avidia environment sourcing (optional)
    if ! grep -q "avidia/env.sh" "$config_file" 2>/dev/null; then
        echo '' >> "$config_file"
        echo '# Load Avidia environment (optional)' >> "$config_file"
        echo '# source "$HOME/.avidia/env.sh"' >> "$config_file"
        shell_updated=true
    fi

    if [ "$shell_updated" = true ]; then
        print_success "Updated $config_file"
    else
        print_success "Shell configuration already up to date"
    fi
}

install_android_cli_tools() {
    print_step "Installing Android Command Line Tools..."

    if [ -f "$AVIDIA_SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]; then
        print_success "Android CLI tools already installed"
        return 0
    fi

    local temp_dir=$(mktemp -d)
    cd "$temp_dir"

    # Download latest command line tools
    print_info "Downloading Android Command Line Tools..."
    local tools_url="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

    if command -v wget &> /dev/null; then
        wget -q --show-progress -c "$tools_url" -O cmdline-tools.zip
    elif command -v curl &> /dev/null; then
        curl -# -L "$tools_url" -o cmdline-tools.zip
    else
        print_error "Neither wget nor curl found"
        return 1
    fi

    if [ ! -f "cmdline-tools.zip" ]; then
        print_error "Failed to download CLI tools"
        return 1
    fi

    # Extract tools
    print_info "Extracting CLI tools..."
    unzip -q cmdline-tools.zip -d "$AVIDIA_SDK_DIR/cmdline-tools"

    # Rename to latest
    if [ -d "$AVIDIA_SDK_DIR/cmdline-tools/cmdline-tools" ]; then
        mv "$AVIDIA_SDK_DIR/cmdline-tools/cmdline-tools" "$AVIDIA_SDK_DIR/cmdline-tools/latest"
    fi

    # Set permissions
    find "$AVIDIA_SDK_DIR/cmdline-tools" -name "*.sh" -exec chmod +x {} \; 2>/dev/null || true
    find "$AVIDIA_SDK_DIR/cmdline-tools" -name "*.bat" -exec chmod +x {} \; 2>/dev/null || true

    # Clean up
    cd ..
    rm -rf "$temp_dir"

    print_success "Android CLI tools installed"
    return 0
}

accept_android_licenses() {
    print_step "Accepting Android SDK licenses..."

    if [ ! -f "$AVIDIA_SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]; then
        print_warning "Android CLI tools not installed, skipping license acceptance"
        return 1
    fi

    # Create licenses directory
    mkdir -p "$AVIDIA_SDK_DIR/licenses"

    # Pre-create accepted licenses file
    echo -e "8933bad161af4178b1185d1a37fbf41ea5269c55\nd56f5187479451eabf01fb78af6dfcb131a6481e\n24333f8a63b6825ea9c5514f83c2829b004d1fee" > "$AVIDIA_SDK_DIR/licenses/android-sdk-license"
    echo "84831b9409646a918e30573bab4c9c91346d8abd" > "$AVIDIA_SDK_DIR/licenses/android-sdk-preview-license"

    # Run sdkmanager to accept licenses
    echo "y" | "$AVIDIA_SDK_DIR/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null 2>&1 || true

    print_success "Android licenses accepted"
    return 0
}

show_instructions() {
    print_header
    echo -e "${GREEN}${BOLD}Installation completed successfully!${NC}"
    echo ""
    echo "Avidia has been installed to: ${CYAN}$AVIDIA_HOME${NC}"
    echo ""
    echo "${YELLOW}${BOLD}Quick Start:${NC}"
    echo "  1. Reload your shell configuration:"
    echo "     ${CYAN}source ~/.bashrc${NC}  # or restart your terminal"
    echo ""
    echo "  2. Setup Avidia environment:"
    echo "     ${CYAN}avidia setup${NC}"
    echo ""
    echo "  3. Launch the Text User Interface:"
    echo "     ${CYAN}avidia tui${NC}"
    echo ""
    echo "${YELLOW}${BOLD}Optional - Install Android components:${NC}"
    echo "  To install Android CLI tools and system images, run:"
    echo "  ${CYAN}avidia tui${NC} and select 'Install System Image'"
    echo ""
    echo "${YELLOW}${BOLD}Directory Structure:${NC}"
    echo "  ${CYAN}$AVIDIA_HOME/${NC}"
    echo "  ├── sdk/              # Android SDK"
    echo "  ├── avd/              # Virtual Devices"
    echo "  ├── lib/              # Libraries (JAR files)"
    echo "  ├── env.sh           # Environment configuration"
    echo "  └── avidia.conf      # Configuration file"
    echo ""
    echo "${YELLOW}${BOLD}Commands:${NC}"
    echo "  ${CYAN}avidia tui${NC}              - Launch Text User Interface"
    echo "  ${CYAN}avidia setup${NC}            - Setup environment"
    echo "  ${CYAN}avidia list${NC}             - List AVDs"
    echo "  ${CYAN}avidia create <name>${NC}    - Create AVD"
    echo "  ${CYAN}avidia start <name>${NC}     - Start AVD"
    echo "  ${CYAN}avidia --help${NC}           - Show help"
    echo ""
    echo "For troubleshooting, check: ${CYAN}$AVIDIA_HOME/avidia.log${NC}"
}

check_existing_installation() {
    if [ -d "$AVIDIA_HOME" ]; then
        print_warning "Existing Avidia installation found at: $AVIDIA_HOME"
        echo ""
        echo "Options:"
        echo "  1. Fresh install (backup and replace)"
        echo "  2. Update existing installation"
        echo "  3. Cancel installation"
        echo ""
        read -p "Choose option [2]: " choice

        case "${choice:-2}" in
            1)
                print_info "Backing up existing installation..."
                local backup_dir="$AVIDIA_HOME.backup.$(date +%Y%m%d_%H%M%S)"
                mv "$AVIDIA_HOME" "$backup_dir"
                print_success "Backup created: $backup_dir"
                ;;
            2)
                print_info "Updating existing installation..."
                # Keep existing SDK and AVDs
                ;;
            3)
                print_info "Installation cancelled"
                exit 0
                ;;
            *)
                print_error "Invalid choice"
                exit 1
                ;;
        esac
    fi
}

# Main installation process
install_avidia() {
    print_header

    check_existing_installation
    check_java
    check_dependencies
    setup_directories
    download_lanterna
    compile_avidia
    create_avidia_script
    setup_environment

    # Optional: Install Android CLI tools
    echo ""
    read -p "Install Android Command Line Tools now? (yes/no) [yes]: " install_cli
    if [[ "${install_cli:-yes}" =~ ^(yes|y)$ ]]; then
        if install_android_cli_tools; then
            accept_android_licenses
        fi
    fi

    update_shell_config
    show_instructions
}

# Uninstallation process
uninstall_avidia() {
    print_header
    echo -e "${YELLOW}${BOLD}UNINSTALL AVIDIA${NC}"
    echo ""

    echo "This will remove:"
    echo "  • $AVIDIA_HOME (including all SDK files and AVDs)"
    echo "  • $AVIDIA_SCRIPT_PATH"
    echo ""
    echo -e "${RED}${BOLD}WARNING: This action cannot be undone!${NC}"
    echo "All Android SDK components and virtual devices will be permanently deleted."
    echo ""

    read -p "Are you absolutely sure? (type 'DELETE' to confirm): " confirm

    if [ "$confirm" != "DELETE" ]; then
        echo ""
        echo -e "${GREEN}Uninstallation cancelled.${NC}"
        exit 0
    fi

    print_step "Stopping any running emulators..."
    # Try to stop any running emulators
    pkill -f "emulator.*-avd" 2>/dev/null || true

    print_step "Removing Avidia directories..."
    if [ -d "$AVIDIA_HOME" ]; then
        rm -rf "$AVIDIA_HOME"
        print_success "Removed $AVIDIA_HOME"
    else
        print_success "AVIDIA directory not found"
    fi

    print_step "Removing Avidia script..."
    if [ -f "$AVIDIA_SCRIPT_PATH" ]; then
        rm -f "$AVIDIA_SCRIPT_PATH"
        print_success "Removed $AVIDIA_SCRIPT_PATH"
    else
        print_success "Avidia script not found"
    fi

    print_step "Cleaning up shell configuration..."
    # Remove Avidia references from shell configs
    for config in "$HOME/.bashrc" "$HOME/.zshrc"; do
        if [ -f "$config" ]; then
            sed -i '/avidia\/env.sh/d' "$config" 2>/dev/null || true
            sed -i '/\.avidia/d' "$config" 2>/dev/null || true
        fi
    done

    echo ""
    echo -e "${GREEN}${BOLD}================================================"
    echo "         AVIDIA UNINSTALLATION COMPLETE         "
    echo "================================================"
    echo -e "${NC}"
    echo "Avidia has been completely removed from your system."
    echo ""
    echo "Note: Android SDK files and virtual devices have been"
    echo "      permanently deleted."
    echo ""
    echo -e "${YELLOW}You may need to restart your terminal for changes to take effect.${NC}"
}

# Repair installation
repair_avidia() {
    print_header
    echo -e "${YELLOW}${BOLD}REPAIR AVIDIA INSTALLATION${NC}"
    echo ""

    if [ ! -d "$AVIDIA_HOME" ]; then
        print_error "Avidia not found. Please install first."
        exit 1
    fi

    print_step "Repairing Avidia installation..."

    # Recompile Avidia
    if [ -f "$AVIDIA_HOME/src/avidia.java" ]; then
        compile_avidia
    else
        print_error "Source file not found. Cannot repair."
        exit 1
    fi

    # Recreate script
    create_avidia_script

    print_success "Repair completed"
    echo ""
    echo "Run ${CYAN}avidia tui${NC} to verify the installation."
}

# Show help
show_help() {
    print_header
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  install      Install Avidia (default)"
    echo "  uninstall    Uninstall Avidia"
    echo "  repair       Repair existing installation"
    echo "  help         Show this help"
    echo ""
    echo "Examples:"
    echo "  $0 install       # Install Avidia"
    echo "  $0 uninstall     # Uninstall Avidia"
    echo "  $0              # Same as install"
    echo ""
}

# Main function
main() {
    local command="${1:-install}"

    case "$command" in
        install)
            install_avidia
            ;;
        uninstall)
            uninstall_avidia
            ;;
        repair)
            repair_avidia
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
