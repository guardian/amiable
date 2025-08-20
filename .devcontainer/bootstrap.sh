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

# ---- set up mise for this script ----
echo -e "\033[1;34m[setup] Setting up mise for this script...\033[0m"
eval "$(mise activate bash --shims)"

# ---- persist mise setup for future bash sessions ----
echo 'eval "$(mise activate bash --shims)"' >> ~vscode/.bashrc

echo -e "\033[1;34m[setup] mise activated. Current version:\033[0m"
mise --version

# Check if mise is activated by inspecting 'mise current' output
if mise current | grep -q 'No tools are currently active'; then
  echo -e "\033[1;31m[setup] WARNING: mise is on PATH but not activated for this shell\033[0m"
else
  echo -e "\033[1;32m[setup] mise is activated\033[0m"
fi

echo -e "\033[1;34m[setup] Persisting mise setup for future bash sessions...\033[0m"
mise trust || true

# installing application toolchain with mise
echo -e "\033[1;34m[setup] Installing developer tools...\033[0m"
mise install

# ---- install other tools on the container ----
echo -e "\033[1;34m[setup] Installing global dev tools on the container...\033[0m"
npm i -g @anthropic-ai/claude-code
claude --version || true

echo -e "\033[1;32m========== setup: complete ==========\033[0m"
