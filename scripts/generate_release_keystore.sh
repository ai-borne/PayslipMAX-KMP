#!/usr/bin/env bash
# ==============================================================================
# PayslipMax - Android Production Keystore Generator
# ==============================================================================
# Generates an RSA 2048-bit PKCS12 production release keystore and creates
# a local (gitignored) keystore.properties file for automated release builds.
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "========================================================"
echo " 🔐 PayslipMax Android Release Keystore Generator"
echo "========================================================"
echo ""

if ! command -v keytool &> /dev/null; then
    echo "❌ Error: 'keytool' command not found. Ensure Java JDK 17+ is installed."
    exit 1
fi

KEYSTORE_NAME="release.keystore"
KEYSTORE_PATH="$PROJECT_ROOT/$KEYSTORE_NAME"
KEY_ALIAS="payslipmax"

if [[ -f "$KEYSTORE_PATH" ]]; then
    echo "⚠️ Warning: Keystore already exists at: $KEYSTORE_PATH"
    read -rp "Overwrite existing keystore? (y/N): " OVERWRITE
    if [[ ! "$OVERWRITE" =~ ^[Yy]$ ]]; then
        echo "Operation cancelled. Existing keystore preserved."
        exit 0
    fi
fi

echo "Enter a strong password for your release keystore (min 6 characters)."
read -rsp "Keystore Password: " STORE_PASSWORD
echo ""
read -rsp "Confirm Keystore Password: " STORE_PASSWORD_CONFIRM
echo ""

if [[ "$STORE_PASSWORD" != "$STORE_PASSWORD_CONFIRM" ]]; then
    echo "❌ Passwords do not match. Aborting."
    exit 1
fi

if [[ ${#STORE_PASSWORD} -lt 6 ]]; then
    echo "❌ Password must be at least 6 characters. Aborting."
    exit 1
fi

echo ""
echo "Enter certificate identification details (press Enter to accept defaults):"
read -rp "Your Name / Organization [ai-borne]: " DNAME_CN
DNAME_CN="${DNAME_CN:-ai-borne}"
read -rp "Organizational Unit [Engineering]: " DNAME_OU
DNAME_OU="${DNAME_OU:-Engineering}"
read -rp "Organization [ai-borne]: " DNAME_O
DNAME_O="${DNAME_O:-ai-borne}"
read -rp "City / Locality [Bangalore]: " DNAME_L
DNAME_L="${DNAME_L:-Bangalore}"
read -rp "State / Province [Karnataka]: " DNAME_ST
DNAME_ST="${DNAME_ST:-Karnataka}"
read -rp "Two-letter Country Code [IN]: " DNAME_C
DNAME_C="${DNAME_C:-IN}"

DNAME="CN=$DNAME_CN, OU=$DNAME_OU, O=$DNAME_O, L=$DNAME_L, ST=$DNAME_ST, C=$DNAME_C"

echo ""
echo "Generating production release keystore..."

keytool -genkeypair -v \
    -keystore "$KEYSTORE_PATH" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storetype PKCS12 \
    -storepass "$STORE_PASSWORD" \
    -keypass "$STORE_PASSWORD" \
    -dname "$DNAME"

echo ""
echo "✅ Keystore created successfully at: $KEYSTORE_PATH"

PROPERTIES_PATH="$PROJECT_ROOT/keystore.properties"
echo ""
read -rp "Create local keystore.properties configured for this keystore? (Y/n): " WRITE_PROPS
WRITE_PROPS="${WRITE_PROPS:-Y}"

if [[ "$WRITE_PROPS" =~ ^[Yy]$ ]]; then
    cat > "$PROPERTIES_PATH" <<EOF
# PayslipMax Local Release Signing Configuration
# GENERATED ON: $(date)
# WARNING: NEVER COMMIT THIS FILE TO GIT

KEYSTORE_PATH=$KEYSTORE_NAME
KEYSTORE_PASSWORD=$STORE_PASSWORD
KEY_ALIAS=$KEY_ALIAS
KEY_PASSWORD=$STORE_PASSWORD
EOF
    chmod 600 "$PROPERTIES_PATH"
    echo "✅ Created $PROPERTIES_PATH (permissions locked to 600)"
fi

echo ""
echo "========================================================"
echo " ⚠️ CRITICAL BACKUP NOTICE"
echo "========================================================"
echo " Store a safe backup of '$KEYSTORE_NAME' and your password"
echo " in a secure password manager (e.g. 1Password/Bitwarden)."
echo " If you lose this key, Google Play will NOT allow updating"
echo " the app in the future without an app signing key reset!"
echo "========================================================"
