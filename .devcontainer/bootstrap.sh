#!/usr/bin/env bash
set -euo pipefail

echo "$USER"
echo "$SHELL"

# ---- system dependencies ----
echo -e "\033[1;34m[setup] Installing system dependencies...\033[0m"

export DEBIAN_FRONTEND=noninteractive
sudo bash -lc 'apt-get update || (sleep 2 && apt-get update)'
sudo bash -lc 'apt-get install -y --no-install-recommends ca-certificates curl git ripgrep'
sudo apt-get clean
sudo rm -rf /var/lib/apt/lists/*

## ---- set up mise-en-place ----
echo -e "\033[1;34m[setup] Setting up mise for this script...\033[0m"
eval "$(mise activate bash --shims)"

echo -e "\033[1;34m[setup] Persisting mise setup for future bash sessions...\033[0m"
echo 'eval "$(mise activate bash --shims)"' >> ~vscode/.bashrc

echo -e "\033[1;34m[setup] mise activated. Current version:\033[0m"
mise --version

## ---- make sure mise can use the checked out project's tools definition ----
mise trust || true

## Check if mise is activated by inspecting 'mise current' output
#if mise current | grep -q 'No tools are currently active'; then
#  echo -e "\033[1;31m[setup] WARNING: mise is on PATH but not activated for this shell\033[0m"
#else
#  echo -e "\033[1;32m[setup] mise is activated\033[0m"
#fi

## installing application toolchain with mise
#echo -e "\033[1;34m[setup] Installing developer tools...\033[0m"
#MAX_INSTALL_RETRIES=3
#INSTALL_RETRY_COUNT=0
#until mise install; do
#  INSTALL_RETRY_COUNT=$((INSTALL_RETRY_COUNT + 1))
#  if [[ INSTALL_RETRY_COUNT -ge MAX_INSTALL_RETRIES ]]; then
#    echo -e "\033[1;31m[setup] mise install failed after $MAX_INSTALL_RETRIES attempts\033[0m"
#    exit 1
#  fi
#  echo -e "\033[1;31m[setup] mise install failed, retrying (INSTALL_RETRY_COUNT/$MAX_INSTALL_RETRIES)...\033[0m"
#  sleep 2
#  mise reshim
#done
#
## ---- install other tools on the container ----
#echo -e "\033[1;34m[setup] Installing global dev tools on the container...\033[0m"
#npm i -g @anthropic-ai/claude-code
#claude --version || true

echo -e "\033[1;32m========== setup: complete ==========\033[0m"
