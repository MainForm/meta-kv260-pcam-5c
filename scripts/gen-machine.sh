#!/usr/bin/env bash
set -euo pipefail

# Run after: source ./edf-init-build-env build
if ! command -v gen-machine-conf >/dev/null 2>&1 ||
   ! command -v bitbake >/dev/null 2>&1 ||
   [[ -z "${BUILDDIR:-}" || ! -d "${BUILDDIR}/conf" ]]; then
    echo "ERROR: 먼저 source ./edf-init-build-env build로 환경을 초기화하세요." >&2
    exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
LAYER_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

source "${LAYER_DIR}/conf/kv260-settings.inc"
MACHINE_NAME="${KV260_MACHINE_NAME}"
SDT_DIR="${LAYER_DIR}/hw/sdt"
# Separate from the previous template-based configuration cache.
WORK_DIR="${BUILDDIR}/gen-machine/${MACHINE_NAME}"

if [[ ! -s "${SDT_DIR}/system-top.dts" ]]; then
    echo "ERROR: hw/sdt/system-top.dts가 필요합니다." >&2
    exit 1
fi

# Remove configuration and cache from the previous generation.
rm -rf -- "${WORK_DIR:?}/output"

mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

# Generate configuration without modifying local.conf.
# Use a generic machine for BitBake initialization.
env \
    BB_ENV_PASSTHROUGH_ADDITIONS="${BB_ENV_PASSTHROUGH_ADDITIONS:-} MACHINE:forcevariable" \
    MACHINE:forcevariable=qemuarm64 \
    gen-machine-conf parse-sdt \
        --hw-description "$SDT_DIR" \
        --machine-name "$MACHINE_NAME" \
        -c "${LAYER_DIR}/conf" \
        --output "${WORK_DIR}/output" \
        -g full


MACHINE_CONF="${LAYER_DIR}/conf/machine/${MACHINE_NAME}.conf"

# Replace host-specific paths with portable SDT references.
sed -i \
    -e 's|^SDT_URI = .*|SDT_URI = "file://${KV260_LAYER_DIR}/hw/sdt"|' \
    -e 's|^SDT_URI\[S\] = .*|SDT_URI[S] = "${WORKDIR}${KV260_LAYER_DIR}/hw/sdt"|' \
    "$MACHINE_CONF"

echo "생성 완료: ${LAYER_DIR}/conf/machine/${MACHINE_NAME}.conf"
echo "생성된 설정과 SDT_URI 경로를 검토한 뒤 빌드 머신을 선택하세요."
