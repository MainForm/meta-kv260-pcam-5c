require recipes-extended/images/edf-linux-disk-image.bb

SUMMARY = "KV260 custom SDT Linux image"

COMPATIBLE_MACHINE = "^zynqmp-kv260-sdt-full$"

# PL 펌웨어 및 부팅에서 PL 자동 로딩을 위한 패키지
IMAGE_INSTALL:append = " kv260-pl-firmware fpga-manager-script kv260-pl-autoload"

# 네트워크 및 WIFI 관련 패키지
IMAGE_INSTALL:append = " networkmanager"