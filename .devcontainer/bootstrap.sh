#!/usr/bin/env bash
set -euo pipefail

if [[ "$USER" != "vscode" ]]; then
  echo "Please run this script as the vscode user."
  exit 1
fi

echo "user: $USER"
echo "shell: $SHELL"

# ---- system dependencies ----
echo -e "\033[1;34m[setup] Installing system dependencies...\033[0m"

export DEBIAN_FRONTEND=noninteractive
sudo bash -lc 'apt-get update || (sleep 2 && apt-get update)'
sudo bash -lc 'apt-get install -y --no-install-recommends ca-certificates curl git ripgrep'
sudo apt-get clean
sudo rm -rf /var/lib/apt/lists/*

## ---- set up mise-en-place ----
echo -e "\033[1;34m[setup] Setting up mise for this script...\033[0m"
# activating with and without shims (not the right approach) for now while we get a prototype working
eval "$(mise activate bash --shims)"

echo -e "\033[1;34m[setup] Persisting mise setup for future bash sessions...\033[0m"

echo -e "\033[1;34m[setup] mise activated. Current version:\033[0m"
mise --version

## ---- make sure mise can use the checked out project's tools definition ----
mise trust || true
mise install

echo -e "\033[1;32m========== setup: complete ==========\033[0m"
