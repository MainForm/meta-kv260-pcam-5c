# Keep the PCAM sensor driver configuration separate from the PL firmware.
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:zynqmp-kv260-sdt-full = " \
    file://pcam-ov5640.cfg \
    file://0001-media-xilinx-fix-vdma-packed-yuv422.patch \
"
