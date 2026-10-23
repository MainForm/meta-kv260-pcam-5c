require recipes-extended/images/edf-linux-disk-image.bb

SUMMARY = "KV260 custom SDT Linux image"

COMPATIBLE_MACHINE = "^zynqmp-kv260-sdt-full$"

# PL 펌웨어 및 부팅에서 PL 자동 로딩을 위한 패키지
IMAGE_INSTALL:append = " kv260-pl-firmware fpga-manager-script kv260-pl-autoload"

# v4l-utils provides v4l2-ctl; media-ctl is a separate output package.
IMAGE_INSTALL:append = " v4l-utils media-ctl i2c-tools libgpiod-tools"

# Camera -> packed YUYV -> JPEG -> RTP/UDP, plus a test pattern for network checks.
IMAGE_INSTALL:append = " \
    gstreamer1.0 \
    gstreamer1.0-plugins-base-videoconvertscale \
    gstreamer1.0-plugins-base-videotestsrc \
    gstreamer1.0-plugins-good-video4linux2 \
    gstreamer1.0-plugins-good-jpeg \
    gstreamer1.0-plugins-good-rtp \
    gstreamer1.0-plugins-good-udp \
"

# 네트워크 및 WIFI 관련 패키지
IMAGE_INSTALL:append = " networkmanager"

