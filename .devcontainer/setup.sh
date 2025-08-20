#!/usr/bin/env bash
set -euo pipefail

# ---- system dependencies ----
echo -e "\033[1;34m[setup] Installing system dependencies...\033[0m"

export DEBIAN_FRONTEND=noninteractive
sudo bash -lc 'apt-get update || (sleep 2 && apt-get update)'
sudo bash -lc 'apt-get install -y --no-install-recommends ca-certificates curl git ripgrep'
sudo apt-get clean
sudo rm -rf /var/lib/apt/lists/*

# ---- set up mise for this script ----
echo -e "\033[1;34m[setup] Setting up mise for this script...\033[0m"
eval "$(mise activate bash 2>/dev/null || mise hook-env -s bash)"
echo -e "\033[1;34m[setup] mise activated. Current version:\033[0m"
mise --version

# Check if mise is activated by inspecting 'mise current' output
if mise current | grep -q 'No tools are currently active'; then
  echo -e "\033[1;31m[setup] WARNING: mise is on PATH but not activated for this shell (no tools active).\033[0m"
  echo -e "\033[1;31m[setup] Current PATH: $PATH\033[0m"
  echo -e "\033[1;31m[setup] Current SHELL: $SHELL\033[0m"
else
  echo -e "\033[1;32m[setup] mise is activated and tools are available.\033[0m"
fi

# ---- persist mise setup for future bash sessions ----
echo -e "\033[1;34m[setup] Persisting mise setup for future bash sessions...\033[0m"

# login shells: /etc/profile.d
sudo tee /etc/profile.d/mise.sh >/dev/null <<'EOF'
# mise activation for bash login shells
export PATH="$HOME/.local/bin:$PATH"
if [ -n "${BASH_VERSION-}" ] && command -v mise >/dev/null 2>&1; then
  eval "$(mise activate bash 2>/dev/null || mise hook-env -s bash)"
fi
EOF

# interactive shells: ~/.bashrc
grep -q 'mise activate' "$HOME/.bashrc" 2>/dev/null || {
  {
    echo ''
    echo '# Activate mise for interactive bash'
    echo 'eval "$(mise activate bash 2>/dev/null || mise hook-env -s bash)"'
  } >> "$HOME/.bashrc"
}

# ensure bash login shells load ~/.bashrc
if ! grep -q 'BASH_VERSION' "$HOME/.profile" 2>/dev/null; then
  {
    echo ''
    echo '# Ensure bash login shells load bashrc'
    echo '[ -n "$BASH_VERSION" ] && [ -f "$HOME/.bashrc" ] && . "$HOME/.bashrc"'
  } >> "$HOME/.profile"
fi

# ---- install dev tools on the container ----
echo -e "\033[1;34m[setup] Installing global dev tools on the container...\033[0m"

# installing dev tools with mise
echo -e "\033[1;34m[setup] Installing developer tools...\033[0m"
mise install

# Make sure Claude Code is available in the container
npm i -g @anthropic-ai/claude-code
claude --version || true

echo -e "\033[1;32m========== setup: complete ==========\033[0m"
