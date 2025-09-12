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
# Needed because shims mode doesn't update paths on failed installs
# This blocks sbt installation via mise when Java is also managed by mise
eval "$(mise activate bash)"

echo -e "\033[1;34m[setup] Persisting mise setup for future bash sessions...\033[0m"

echo -e "\033[1;34m[setup] mise activated. Current version:\033[0m"
mise --version

## ---- make sure mise can use the checked out project's tools definition ----
mise trust || true

# try mise install twice to handle dependencies between installations
# In particular, sbt requires Java and will fail in the initial install
# Once Java is installed (after the first installation) the second will succeed
# See: https://github.com/mise-plugins/mise-sbt/issues/3
for i in 1 2; do
  mise install && break
  echo "mise install failed, retrying in 2 seconds... (attempt $i)"
  sleep 2
done

echo -e "\033[1;32m========== setup: complete ==========\033[0m"
