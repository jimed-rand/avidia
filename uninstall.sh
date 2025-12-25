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

# Directories (must match install.sh)
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

check_installation() {
    print_step "Checking for Avidia installation..."
    
    local found=false
    
    if [ -d "$AVIDIA_HOME" ]; then
        print_success "Found Avidia directory: $AVIDIA_HOME"
        found=true
    fi
    
    if [ -f "$AVIDIA_SCRIPT_PATH" ]; then
        print_success "Found Avidia script: $AVIDIA_SCRIPT_PATH"
        found=true
    fi
    
    if [ "$found" = false ]; then
        print_error "Avidia is not installed on this system"
        echo ""
        echo "Nothing to uninstall."
        exit 0
    fi
}

show_installation_info() {
    echo ""
    echo "Current installation:"
    echo ""
    
    if [ -d "$AVIDIA_HOME" ]; then
        echo "  Directory: ${CYAN}$AVIDIA_HOME${NC}"
        
        # Calculate size
        local dir_size=$(du -sh "$AVIDIA_HOME" 2>/dev/null | cut -f1)
        echo "  Size: ${YELLOW}$dir_size${NC}"
        
        # Count AVDs
        local avd_count=0
        if [ -d "$AVIDIA_HOME/avd" ]; then
            avd_count=$(find "$AVIDIA_HOME/avd" -maxdepth 1 -type d | wc -l)
            avd_count=$((avd_count - 1)) # Subtract the parent directory
        fi
        echo "  Virtual Devices: ${YELLOW}$avd_count${NC}"
    fi
    
    if [ -f "$AVIDIA_SCRIPT_PATH" ]; then
        echo "  Script: ${CYAN}$AVIDIA_SCRIPT_PATH${NC}"
    fi
    
    echo ""
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
                
                # Verify they're stopped
                local still_running=$(pgrep -f "emulator.*-avd" 2>/dev/null | wc -l)
                if [ "$still_running" -gt 0 ]; then
                    print_warning "Some emulators are still running, force stopping..."
                    pkill -9 -f "emulator.*-avd" 2>/dev/null || true
                    sleep 1
                fi
                
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
            if [ -d "$AVIDIA_HOME/avd" ] && [ "$(ls -A "$AVIDIA_HOME/avd")" ]; then
                mkdir -p "$backup_dir"
                print_step "Backing up AVDs..."
                cp -r "$AVIDIA_HOME/avd" "$backup_dir/"
                print_success "AVDs backed up to: $backup_dir/avd"
            else
                print_warning "No AVDs found to backup"
            fi
            ;;
        2)
            if [ -d "$AVIDIA_HOME/sdk" ] && [ "$(ls -A "$AVIDIA_HOME/sdk")" ]; then
                mkdir -p "$backup_dir"
                print_step "Backing up SDK..."
                cp -r "$AVIDIA_HOME/sdk" "$backup_dir/"
                print_success "SDK backed up to: $backup_dir/sdk"
            else
                print_warning "No SDK found to backup"
            fi
            ;;
        3)
            if [ -d "$AVIDIA_HOME" ]; then
                mkdir -p "$backup_dir"
                print_step "Creating complete backup..."
                cp -r "$AVIDIA_HOME" "$backup_dir/"
                print_success "Complete backup created at: $backup_dir"
            else
                print_warning "No Avidia directory found to backup"
            fi
            ;;
        4)
            print_success "Skipping backup"
            ;;
        *)
            print_error "Invalid choice, skipping backup"
            ;;
    esac
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
            print_warning "Deletion cancelled"
            echo "Uninstallation aborted."
            exit 0
        fi

        rm -rf "$AVIDIA_HOME"
        print_success "Removed $AVIDIA_HOME"
    else
        print_warning "AVIDIA directory not found (already removed?)"
    fi
}

remove_avidia_script() {
    print_step "Removing Avidia script..."

    if [ -f "$AVIDIA_SCRIPT_PATH" ]; then
        rm -f "$AVIDIA_SCRIPT_PATH"
        print_success "Removed $AVIDIA_SCRIPT_PATH"
    else
        print_warning "Avidia script not found"
    fi

    # Also remove any other avidia scripts or symlinks
    local other_scripts=(
        "$HOME/bin/avidia"
        "/usr/local/bin/avidia"
        "/usr/bin/avidia"
    )

    for script in "${other_scripts[@]}"; do
        if [ -f "$script" ] || [ -L "$script" ]; then
            if rm -f "$script" 2>/dev/null; then
                print_success "Removed: $script"
            fi
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
    local backup_created=false

    for config in "${config_files[@]}"; do
        if [ -f "$config" ]; then
            # Backup the config file (only once per file)
            if [ ! -f "${config}.avidia-backup" ]; then
                cp "$config" "${config}.avidia-backup" 2>/dev/null || true
                backup_created=true
            fi

            # Remove Avidia environment sourcing
            if grep -q "avidia/env.sh" "$config" 2>/dev/null; then
                sed -i '/avidia\/env.sh/d' "$config"
                cleaned=true
            fi

            # Remove any AVIDIA-related lines
            if grep -q "AVIDIA\|\.avidia" "$config" 2>/dev/null; then
                sed -i '/AVIDIA/d' "$config"
                sed -i '/\.avidia/d' "$config"
                cleaned=true
            fi
            
            # Remove Android environment variables set by Avidia
            if grep -q "ANDROID_SDK_ROOT.*\.avidia\|ANDROID_HOME.*\.avidia\|AVD_HOME.*\.avidia" "$config" 2>/dev/null; then
                sed -i '/ANDROID_SDK_ROOT.*\.avidia/d' "$config"
                sed -i '/ANDROID_HOME.*\.avidia/d' "$config"
                sed -i '/AVD_HOME.*\.avidia/d' "$config"
                cleaned=true
            fi
        fi
    done

    if [ "$cleaned" = true ]; then
        print_success "Shell configuration cleaned"
        if [ "$backup_created" = true ]; then
            print_info "Backups created with .avidia-backup extension"
        fi
    else
        print_success "No Avidia references found in shell config"
    fi
}

remove_environment_variables() {
    print_step "Removing environment variables..."

    # Unset variables in current session (only if they point to Avidia)
    if [[ "${ANDROID_SDK_ROOT:-}" == *".avidia"* ]]; then
        unset ANDROID_SDK_ROOT 2>/dev/null || true
    fi
    
    if [[ "${ANDROID_HOME:-}" == *".avidia"* ]]; then
        unset ANDROID_HOME 2>/dev/null || true
    fi
    
    if [[ "${AVD_HOME:-}" == *".avidia"* ]]; then
        unset AVD_HOME 2>/dev/null || true
    fi

    # Remove Avidia paths from PATH (current session)
    export PATH=$(echo "$PATH" | sed -e "s|:$HOME/.avidia/sdk/cmdline-tools/latest/bin||g" | sed -e "s|$HOME/.avidia/sdk/cmdline-tools/latest/bin:||g")
    export PATH=$(echo "$PATH" | sed -e "s|:$HOME/.avidia/sdk/platform-tools||g" | sed -e "s|$HOME/.avidia/sdk/platform-tools:||g")
    export PATH=$(echo "$PATH" | sed -e "s|:$HOME/.avidia/sdk/emulator||g" | sed -e "s|$HOME/.avidia/sdk/emulator:||g")

    print_success "Environment variables removed"
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
    echo "  • You may need to restart your terminal for changes to take effect"
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
    
    show_installation_info
    
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
    check_installation
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
