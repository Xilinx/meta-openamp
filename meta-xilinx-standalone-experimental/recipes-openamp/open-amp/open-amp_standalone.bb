LICENSE = "BSD"
LIC_FILES_CHKSUM = "file://LICENSE.md;md5=0e6d7bfe689fe5b0d0a89b2ccbe053fa"
SRC_URI:armv7r:xilinx-standalone = "git://gitenterprise.xilinx.com/OpenAMP/open-amp.git;branch=xlnx_decoupling"
SRCREV = "d7c774269feb1e9d3d7079b203b890081ff559c1"

S = "${WORKDIR}/git"
B = "${WORKDIR}/build"
OECMAKE_SOURCEPATH = "${S}/"
PROVIDES = "openamp"
DEPENDS:armv7r:xilinx-standalone += " scugic libmetal xilstandalone python3-pyyaml-native lopper-native python3-dtc-native  nativesdk-xilinx-lops "
DEPENDS:append:versal = " uartpsv "
DEPENDS:append:zynqmp = " uartps "
DTS_FILE = "/scratch/decoupling/lopper/lopper-sdt.dtb"
FILESEXTRAPATHS:append := ":${THISDIR}/overlays"

SRC_URI:append:zynqmp = " file://openamp-overlay-zynqmp.yaml "
SRC_URI:append:versal = " file://openamp-overlay-versal.yaml "
OPENAMP_OVERLAY:zynqmp ?= "${S}/../openamp-overlay-zynqmp.yaml"
OPENAMP_OVERLAY:versal ?= "${S}/../openamp-overlay-versal.yaml"

inherit cmake deploy python3native
# We need the deployed output
do_configure:armv7r:xilinx-standalone[depends] += "device-tree-lops:do_deploy lopper-native:do_install libmetal:do_depoy "
do_compile:armv7r:xilinx-standalone[depends] += "device-tree-lops:do_deploy"
do_install:armv7r:xilinx-standalone[depends] += "device-tree-lops:do_deploy"

BB_STRICT_CHECKSUM = "0"


EXTRA_OECMAKE = " \
       -DLIB_INSTALL_DIR=${libdir} \
       -DSOC_FAMILY="${SOC_FAMILY}" \
       "

COMPATIBLE_HOST = ".*-elf"
COMPATIBLE_HOST:arm = "[^-]*-[^-]*-eabi"

OPENAMP_CMAKE_MACHINE:versal = "Versal"
OPENAMP_CMAKE_MACHINE:zynqmp = "Zynqmp"

def get_cross_prefix(oe_cmake_c_compiler):
  if oe_cmake_c_compiler == 'arm-xilinx-eabi-gcc':
    return 'arm-xilinx-eabi-'

OPENAMP_CROSS_PREFIX:armv7r:xilinx-standalone = "${@get_cross_prefix(d.getVar('OECMAKE_C_COMPILER'))}"

OPENAMP_MACHINE:armv7r:xilinx-standalone = "zynqmp_r5"

ALLOW_EMPTY:${PN}-demos = "1"
PACKAGES:append += "${PN}-demos"
EXTRA_OECMAKE:append = "-DWITH_APPS=ON "

REQUIRED_DISTRO_FEATURES:armv7r:xilinx-standalone = "${DISTRO_FEATURES}"
PACKAGECONFIG:armv7r:xilinx-standalone ?= "${DISTRO_FEATURES} ${MACHINE_FEATURES}"

FILES:${PN}-demos:armv7r:xilinx-standalone = " \
    ${base_libdir}/firmware/*\.out \
"

LOPS_DIR="${RECIPE_SYSROOT_NATIVE}/${PYTHON_SITEPACKAGES_DIR}/lopper/lops/"

CHANNEL_INFO_FILE = "openamp-channel-info.txt"

PACKAGE_DEBUG_SPLIT_STYLE:armv7r:xilinx-standalone='debug-file-directory'
INHIBIT_PACKAGE_STRIP:armv7r:xilinx-standalone = '1'
INHIBIT_PACKAGE_DEBUG_SPLIT:armv7r:xilinx-standalone = '1'
PACKAGE_MINIDEBUGINFO:armv7r:xilinx-standalone = '1'

# The order of lops is as follows:
# 1. lop-load - This must be run first to ensure enabled lops and plugins
#    can be used.
# 2. lop-xlate-yaml - Ensure any following lops can be YAML
# 3. imux - This is used to make sure the imux node has correct
#    interrupt parent and interrupt-multiplex node is trimmed. This is
#    present for all Xilinx Lopper runs, with or without OpenAMP. This
#    is done first for all lop runs as the imux node may be referenced by
#    later plugins.
# 4. domain - domain processing should be done before OpenAMP as there
#    can be nodes that are stripped out or modified based on the domain.
# 5. OpenAMP - This lopper processing is done on top of the domain as
#    noted due to domain reasons above.
# 6. domain-prune - Only prune files AFTER all other processing is complete
#    so that a lopper plugin or lop does not inadvertently process a
#    non-existent node.
OPENAMP_LOPPER_INPUTS:armv7r:xilinx-standalone  = " \
    -i ${OPENAMP_OVERLAY} \
    -i ${LOPS_DIR}/lop-load.dts \
    -i ${LOPS_DIR}/lop-xlate-yaml.dts \
    -i ${LOPS_DIR}/lop-r5-imux.dts \
    -i ${LOPS_DIR}/lop-openamp-versal.dts "

do_run_lopper() {
    cd ${WORKDIR}

    lopper -f -v --enhanced  --permissive \
    ${OPENAMP_LOPPER_INPUTS} \
    ${OPENAMP_DTFILE} \
    ${LOPPER_OPENAMP_OUT_DTB}

    cd -
}

addtask run_lopper before do_generate_toolchain_file
addtask run_lopper after do_prepare_recipe_sysroot

python do_set_openamp_cmake_vars() {
    def parse_channel_info( val, d ):
        filename = d.getVar('WORKDIR') + '/' + d.getVar('CHANNEL_INFO_FILE')
        with open(filename, "r") as f:
            lines = f.readlines()
            for l in lines:
                if val in l:
                    ret = l.replace(val+'=','').replace('\n','').replace('"','').upper().replace('resource','').replace('0X','0x')
                    if 'TO_GROUP' in val:
                        ret = ret.split('-')[2].replace('@','_').upper()
                    return ret

        return ""


    def get_vring_mem_size(vring0, vring1):
        return hex( int(vring0,16) + int(vring1, 16) )

    def get_ipi_str(val):
        return val.replace('0x','') +'.ipi'

    VDEV0VRING0SIZE = parse_channel_info( "CHANNEL0VRING0SIZE", d)
    VDEV0VRING1SIZE = parse_channel_info( "CHANNEL0VRING1SIZE", d)

    d.setVar("IPI_DEV_NAME", get_ipi_str(parse_channel_info( "CHANNEL0TO_REMOTE", d)))
    d.setVar("IPI_CHN_BITMASK", parse_channel_info( "CHANNEL0TO_HOST-BITMASK", d))
    d.setVar("IPI_IRQ_VECT_ID", parse_channel_info( "CHANNEL0TO_REMOTE-IPIIRQVECTID", d))
    d.setVar("POLL_BASE_ADDR", parse_channel_info( "CHANNEL0TO_REMOTE", d))

    d.setVar("VRING_MEM_PA", parse_channel_info(    "CHANNEL0VDEV0BUFFERRX", d))
    d.setVar("SHARED_BUF_PA", parse_channel_info( "CHANNEL0VDEV0BUFFERBASE", d))
    d.setVar("SHARED_BUF_SIZE", parse_channel_info( "CHANNEL0VDEV0BUFFERSIZE", d))
    d.setVar("RING_TX", parse_channel_info( "CHANNEL0VDEV0BUFFERTX", d) )
    d.setVar("RING_RX", parse_channel_info( "CHANNEL0VDEV0BUFFERRX", d) )
    d.setVar("SHARED_MEM_PA", parse_channel_info( "CHANNEL0VRING0BASE", d) )
    d.setVar("SHARED_MEM_SIZE", parse_channel_info( "CHANNEL0VDEV0BUFFERSIZE", d) )
    d.setVar("SHARED_BUF_OFFSET",  get_vring_mem_size( VDEV0VRING0SIZE, VDEV0VRING1SIZE ))
    d.setVar("ELFLOAD_START", parse_channel_info("CHANNEL0ELFBASE", d))
    d.setVar("ELFLOAD_END", parse_channel_info("CHANNEL0ELFSIZE", d))

    rsc_table = parse_channel_info("CHANNEL0ELFBASE", d)
    rsc_table = int(rsc_table,16) + 0x20000
    rsc_table = hex(rsc_table)
    d.setVar("RSC_TABLE", rsc_table)
}
# run lopper before parsing lopper-generated file
do_set_openamp_cmake_vars[prefuncs]  += "do_run_lopper"

# set openamp vars before using them in toolchain file
do_generate_toolchain_file[prefuncs] += "do_set_openamp_cmake_vars"

python openamp_toolchain_file_setup() {
    toolchain_file_path = d.getVar("WORKDIR") + "/toolchain.cmake"
    toolchain_file = open(toolchain_file_path, "a") # a for append

    # generate boilerplate for toolchain file
    lines = [
      "set( CMAKE_SYSTEM_PROCESSOR \"" + d.getVar("TRANSLATED_TARGET_ARCH") + "\" )",
      "set( MACHINE \""                + d.getVar("OPENAMP_MACHINE")        + "\" )",
      "set( CMAKE_MACHINE \""          + d.getVar("OPENAMP_CMAKE_MACHINE")  + "\" )",
      "set (DECOUPLING true)",
      "set( CMAKE_C_ARCHIVE_CREATE \"<CMAKE_AR> qcs <TARGET> <LINK_FLAGS> <OBJECTS>\")",
      "set( CMAKE_SYSTEM_NAME \"Generic\")",
      "set( CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER CACHE STRING \"\")",
      "set (CMAKE_FIND_ROOT_PATH_MODE_LIBRARY NEVER CACHE STRING \"\")",
      "set (CMAKE_FIND_ROOT_PATH_MODE_INCLUDE NEVER CACHE STRING \"\")",
      "include (CMakeForceCompiler)",
      "CMAKE_FORCE_C_COMPILER( \""        + d.getVar("OECMAKE_C_COMPILER") + "\" GNU)",
      "set (CROSS_PREFIX \""           + d.getVar("OPENAMP_CROSS_PREFIX") + "\" )",
      "set (CMAKE_C_ARCHIVE_FINISH true)",
      "set (LIBMETAL_PATH \""     + d.getVar("S") + "/../recipe-sysroot/usr/include/\" CACHE STRING \"\")",
      "set (LIBMETAL_INCLUDE_DIR \""     + d.getVar("S") + "/../recipe-sysroot/usr/include/\" CACHE STRING \"\")",
      "set (XIL_INCLUDE_DIR \""     + d.getVar("S") + "/../recipe-sysroot/usr/include/\" CACHE STRING \"\")",
      "include (cross_generic_gcc)",
      "set (RSC_TABLE \"" + d.getVar("RSC_TABLE") + "\" CACHE STRING \"\")",
      "set (ELFLOAD_START \"" + d.getVar("ELFLOAD_START") + "\" CACHE STRING \"\")",
      "set (ELFLOAD_END \"" + d.getVar("ELFLOAD_END") + "\" CACHE STRING \"\")",

    ]
    for line in lines:
        toolchain_file.write(line + "\n")

    # openamp app specific info
    config_vars = [ "RING_RX", "RING_TX", "SHARED_MEM_PA", "SHARED_MEM_SIZE", "SHARED_BUF_OFFSET", "POLL_BASE_ADDR", "IPI_CHN_BITMASK", "IPI_IRQ_VECT_ID"]

    defs = " "
    for cv in config_vars:
        defs += " -D" + cv + "=" + d.getVar(cv)

    toolchain_file.write("add_definitions( " + defs + " )")
}

do_generate_toolchain_file[postfuncs] += "openamp_toolchain_file_setup"

# deploy for other recipes
DEPLOY_MACHINE = "${@ d.getVar('MACHINE_ARCH').replace('_','-') }"
SHOULD_DEPLOY = "${@'true' if ( 'Standalone' in  d.getVar('DISTRO_NAME') ) else 'false'}"
do_deploy() {
    echo "get the following: ";
    if ${SHOULD_DEPLOY}; then
        install -Dm 0644 ${D}/usr/bin/*.out ${DEPLOY_DIR}/images/${DEPLOY_MACHINE}/
    fi
}
addtask deploy before do_build after do_install
