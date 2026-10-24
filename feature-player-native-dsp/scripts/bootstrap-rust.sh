#!/usr/bin/env sh
# Optional Linux x86_64 setup, entirely inside the repository's ignored .gradle.
set -eu
if [ "$(uname -s)" != Linux ] || [ "$(uname -m)" != x86_64 ]; then
    printf '%s\n' 'Install Rust 1.90.0 with rustup on this host, then add aarch64-linux-android.' >&2
    exit 1
fi
project_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
export CARGO_HOME="$project_root/.gradle/spica-rust/cargo"
export RUSTUP_HOME="$project_root/.gradle/spica-rust/rustup"
installer=$(mktemp)
trap 'rm -f "$installer"' EXIT HUP INT TERM
curl -fL https://sh.rustup.rs -o "$installer"
sh "$installer" -y --profile minimal --default-toolchain 1.90.0 \
    --target aarch64-linux-android --no-modify-path
