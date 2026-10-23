SUMMARY = "KV260 PL bitstream and device tree overlay"
DESCRIPTION = "Package the SDT bitstream and generated PL overlay for runtime loading."
LICENSE = "CLOSED"

inherit dfx_user_dts

COMPATIBLE_MACHINE = "^zynqmp-kv260-sdt-full$"

# Read the original bitstream and gen-machine.sh output without duplicating them.
FILESEXTRAPATHS:prepend := "${THISDIR}/../../hw/sdt:${THISDIR}/../../conf/dts/${KV260_MACHINE_NAME}/pl-overlay-full:"

def kv260_find_bitstream(d):
    import glob
    import os
    
    sdt_dir = os.path.join(d.getVar('KV260_LAYER_DIR'), 'hw', 'sdt')
    # Reparse when files are added, removed or renamed in the SDT directory.
    bb.parse.mark_dependency(d, sdt_dir)
    bitstreams = sorted(os.path.basename(path)
                        for path in glob.glob(os.path.join(sdt_dir, '*.bit'))
                        if os.path.isfile(path))
    if len(bitstreams) != 1:
        raise bb.parse.SkipRecipe(
            'Expected exactly one .bit file in %s; found: %s. '
            'Keep one .bit file or set KV260_BITSTREAM to its filename in local.conf.'
            % (sdt_dir, ', '.join(bitstreams) or '(none)'))
    return bitstreams[0]

# Override with a filename from hw/sdt when multiple bitstreams are present.
KV260_BITSTREAM ?= "${@kv260_find_bitstream(d)}"

SRC_URI = " \
    file://${KV260_BITSTREAM} \
    file://pl.dtso \
    file://pcam-user.dtsi \
"

DT_FILES = "pl.dtso"

# Include PCAM settings after the generated PL nodes in the build copy only.
python do_configure:append() {

    path = os.path.join(d.getVar('S'), 'pl.dtso')
    include = '#include "pcam-user.dtsi"'
    with open(path, 'r') as f:
        content = f.read()
    if include not in content:
        with open(path, 'a') as f:
            f.write('\n' + include + '\n')
}
