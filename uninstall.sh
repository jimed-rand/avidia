#!/bin/bash
set -euo pipefail

# ================================================
#               AVIDIA UNINSTALLER
# ================================================

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Directories
AVIDIA_HOME="$HOME/.avidia"
AVIDIA_BIN_DIR="$HOME/.local/bin"
AVIDIA_SCRIPT_PATH="$AVIDIA_BIN_DIR/avidia"

print_header() {
    echo -e "${CYAN}${BOLD}"
    echo "================================================"
    echo "              AVIDIA UNINSTALLER                "
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

print_step() {
    echo -e "${BLUE}➜ $1${NC}"
}

stop_running_emulators() {
    print_step "Checking for running emulators..."

    local emulator_count=$(pgrep -f "emulator.*-avd" 2>/dev/null | wc -l)

    if [ "$emulator_count" -gt 0 ]; then
        print_warning "Found $emulator_count running emulator(s)"
        echo ""
        echo "Options:"
        echo "  1. Stop all emulators (recommended)"
        echo "  2. Keep running and continue"
        echo "  3. Cancel uninstallation"
        echo ""
        read -p "Choose option [1]: " choice

        case "${choice:-1}" in
            1)
                print_step "Stopping all emulators..."
                pkill -f "emulator.*-avd" 2>/dev/null || true
                sleep 2
                print_success "Emulators stopped"
                ;;
            2)
                print_warning "Keeping emulators running..."
                ;;
            3)
                print_success "Uninstallation cancelled"
                exit 0
                ;;
            *)
                print_error "Invalid choice"
                exit 1
                ;;
        esac
    else
        print_success "No running emulators found"
    fi
}

remove_avidia_directories() {
    print_step "Removing Avidia directories..."

    if [ -d "$AVIDIA_HOME" ]; then
        # Show what will be removed
        echo ""
        echo "The following will be deleted:"
        echo "  • $AVIDIA_HOME/sdk/ (Android SDK files)"
        echo "  • $AVIDIA_HOME/avd/ (Virtual devices)"
        echo "  • $AVIDIA_HOME/lib/ (Libraries)"
        echo "  • $AVIDIA_HOME/* (Configuration files)"
        echo ""

        read -p "Continue with deletion? (yes/no): " confirm
        if [[ ! "$confirm" =~ ^(yes|y)$ ]]; then
            print_success "Deletion cancelled"
            return
        fi

        rm -rf "$AVIDIA_HOME"
        print_success "Removed $AVIDIA_HOME"
    else
        print_success "AVIDIA directory not found (already removed?)"
    fi
}

remove_avidia_script() {
    print_step "Removing Avidia script..."

    if [ -f "$AVIDIA_SCRIPT_PATH" ]; then
        rm -f "$AVIDIA_SCRIPT_PATH"
        print_success "Removed $AVIDIA_SCRIPT_PATH"
    else
        print_success "Avidia script not found"
    fi

    # Also remove any other avidia scripts
    local other_scripts=(
        "$HOME/bin/avidia"
        "/usr/local/bin/avidia"
        "/usr/bin/avidia"
    )

    for script in "${other_scripts[@]}"; do
        if [ -f "$script" ] && [ -L "$script" ]; then
            rm -f "$script"
            print_success "Removed symlink: $script"
        fi
    done
}

cleanup_shell_config() {
    print_step "Cleaning up shell configuration..."

    local config_files=(
        "$HOME/.bashrc"
        "$HOME/.zshrc"
        "$HOME/.bash_profile"
        "$HOME/.profile"
        "$HOME/.config/fish/config.fish"
    )

    local cleaned=false

    for config in "${config_files[@]}"; do
        if [ -f "$config" ]; then
            # Backup the config file
            cp "$config" "${config}.avidia-backup" 2>/dev/null || true

            # Remove Avidia environment sourcing
            if grep -q "avidia/env.sh" "$config"; then
                sed -i '/avidia\/env.sh/d' "$config"
                cleaned=true
            fi

            # Remove any AVIDIA-related lines
            if grep -q "AVIDIA" "$config" || grep -q "\.avidia" "$config"; then
                sed -i '/AVIDIA/d' "$config"
                sed -i '/\.avidia/d' "$config"
                cleaned=true
            fi
        fi
    done

    if [ "$cleaned" = true ]; then
        print_success "Shell configuration cleaned"
    else
        print_success "No Avidia references found in shell config"
    fi
}

remove_environment_variables() {
    print_step "Removing environment variables..."

    # Unset variables in current session
    unset ANDROID_SDK_ROOT 2>/dev/null || true
    unset ANDROID_HOME 2>/dev/null || true
    unset AVD_HOME 2>/dev/null || true

    # Remove from PATH
    export PATH=$(echo "$PATH" | sed -e "s|:$HOME/.avidia/sdk/cmdline-tools/latest/bin||g")
    export PATH=$(echo "$PATH" | sed -e "s|:$HOME/.avidia/sdk/platform-tools||g")
    export PATH=$(echo "$PATH" | sed -e "s|:$HOME/.avidia/sdk/emulator||g")
    export PATH=$(echo "$PATH" | sed -e "s|:$HOME/.local/bin||g")

    print_success "Environment variables removed"
}

show_backup_options() {
    print_step "Backup options..."

    echo ""
    echo "Would you like to backup any data before uninstalling?"
    echo "  1. Backup AVDs (virtual devices)"
    echo "  2. Backup Android SDK"
    echo "  3. Backup everything"
    echo "  4. Skip backup"
    echo ""
    read -p "Choose option [4]: " choice

    local backup_dir="$HOME/avidia-backup-$(date +%Y%m%d_%H%M%S)"

    case "${choice:-4}" in
        1)
            mkdir -p "$backup_dir"
            if [ -d "$AVIDIA_HOME/avd" ]; then
                cp -r "$AVIDIA_HOME/avd" "$backup_dir/"
                print_success "AVDs backed up to: $backup_dir/avd"
            fi
            ;;
        2)
            mkdir -p "$backup_dir"
            if [ -d "$AVIDIA_HOME/sdk" ]; then
                cp -r "$AVIDIA_HOME/sdk" "$backup_dir/"
                print_success "SDK backed up to: $backup_dir/sdk"
            fi
            ;;
        3)
            mkdir -p "$backup_dir"
            if [ -d "$AVIDIA_HOME" ]; then
                cp -r "$AVIDIA_HOME" "$backup_dir/"
                print_success "Complete backup to: $backup_dir"
            fi
            ;;
        4)
            print_success "Skipping backup"
            ;;
        *)
            print_error "Invalid choice"
            ;;
    esac
}

show_final_message() {
    echo ""
    echo -e "${GREEN}${BOLD}================================================"
    echo "         AVIDIA UNINSTALLATION COMPLETE         "
    echo "================================================"
    echo -e "${NC}"
    echo "Avidia has been completely removed from your system."
    echo ""
    echo "Summary of actions:"
    echo "  • Removed Avidia directory: $AVIDIA_HOME"
    echo "  • Removed Avidia script: $AVIDIA_SCRIPT_PATH"
    echo "  • Cleaned shell configuration files"
    echo "  • Removed environment variables"
    echo ""
    echo "Note:"
    echo "  • Any running emulators were stopped"
    echo "  • Shell config backups were created as *.avidia-backup"
    echo "  • You may need to restart your terminal"
    echo ""
    echo -e "${YELLOW}Thank you for using Avidia!${NC}"
    echo ""
}

confirm_uninstallation() {
    print_header

    echo "This will completely remove Avidia from your system."
    echo ""
    echo -e "${RED}${BOLD}WARNING: This action cannot be undone!${NC}"
    echo ""
    echo "The following will be deleted:"
    echo "  • $AVIDIA_HOME (including all SDK files and AVDs)"
    echo "  • Avidia executable script"
    echo "  • Avidia environment configuration"
    echo ""
    echo "All Android SDK components and virtual devices will be lost."
    echo ""

    read -p "Type 'UNINSTALL' to confirm: " confirm

    if [ "$confirm" != "UNINSTALL" ]; then
        echo ""
        echo -e "${GREEN}Uninstallation cancelled.${NC}"
        exit 0
    fi

    return 0
}

# Main uninstallation process
main() {
    confirm_uninstallation
    show_backup_options
    stop_running_emulators
    remove_avidia_directories
    remove_avidia_script
    cleanup_shell_config
    remove_environment_variables
    show_final_message
}

# Run main function
main "$@"
