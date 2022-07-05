SUMMARY = "Libopen_amp : Libmetal implements an abstraction layer across user-space Linux, baremetal, and RTOS environments"

HOMEPAGE = "https://github.com/OpenAMP/open-amp/"

SECTION = "libs"

LICENSE = "BSD"
LIC_FILES_CHKSUM ?= "file://LICENSE.md;md5=0e6d7bfe689fe5b0d0a89b2ccbe053fa"

SRC_URI = "git://gitenterprise.xilinx.com/OpenAMP/open-amp.git;protocol=https;branch=xlnx_decoupling"
SRCREV = "d7c774269feb1e9d3d7079b203b890081ff559c1"
S = "${WORKDIR}/git"

DEPENDS = "libmetal lopper-native "
PROVIDES = "openamp"

inherit python3native pkgconfig cmake yocto-cmake-translation

EXTRA_OECMAKE = " \
	-DLIB_INSTALL_DIR=${libdir} \
	-DLIBEXEC_INSTALL_DIR=${libexecdir} \
	-DMACHINE=zynqmp \
	"

SOC_FAMILY_ARCH ??= "${TUNE_PKGARCH}"
PACKAGE_ARCH = "${SOC_FAMILY_ARCH}"

CFLAGS:versal += " -Dversal -O1 "
# OpenAMP apps not ready for Zynq
EXTRA_OECMAKE:append:zynqmp = "-DWITH_APPS=ON -DWITH_PROXY=on -DWITH_PROXY_APPS=on "
EXTRA_OECMAKE:append:versal = "-DWITH_APPS=ON -DWITH_PROXY=on -DWITH_PROXY_APPS=on "

ALLOW_EMPTY:${PN}-demos = "1"
PACKAGES:append += "${PN}-demos"

FILES:${PN} = " \
    ${libdir}/*.so* \
"

FILES:${PN}-demos = " \
    ${bindir}/*-shared \
"
do_install:append () {
	# Only install echo test client, matrix multiplication client,
	# and proxy app server for ZynqMP
	rm -rf ${D}/${bindir}/*-static
}

FILESEXTRAPATHS:append := ":${THISDIR}/overlays"
SRC_URI:append:zynqmp = "  file://openamp-overlay-zynqmp.yaml "
SRC_URI:append:versal = "  file://openamp-overlay-versal.yaml "
OPENAMP_OVERLAY:zynqmp ?= "${S}/../openamp-overlay-zynqmp.yaml"
OPENAMP_OVERLAY:versal ?= "${S}/../openamp-overlay-versal.yaml"

# We need the deployed output
do_configure[depends] += " lopper-native:do_install"

LOPS_DIR="${RECIPE_SYSROOT_NATIVE}/${PYTHON_SITEPACKAGES_DIR}/lopper/lops/"

CHANNEL_INFO_FILE = "openamp-channel-info.txt"
LOPPER_OPENAMP_OUT_DTB = "${WORKDIR}/openamp-lopper-output.dtb"

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
OPENAMP_LOPPER_INPUTS:zynqmp:linux = "            \
    -i ${LOPS_DIR}/lop-a53-imux.dts               \
    -i ${LOPS_DIR}/lop-domain-linux-a53.dts       \
    -i ${LOPS_DIR}/lop-openamp-versal.dts         \
    -i ${LOPS_DIR}/lop-domain-linux-a53-prune.dts "

OPENAMP_LOPPER_INPUTS:versal:linux = "      \
    -i ${LOPS_DIR}/lop-a72-imux.dts         \
    -i ${LOPS_DIR}/lop-domain-a72.dts      \
    -i ${LOPS_DIR}/lop-openamp-versal.dts  \
    -i ${LOPS_DIR}/lop-domain-a72-prune.dts "

do_run_lopper() {
    cd ${WORKDIR}

    lopper -f -v --enhanced  --permissive \
    -i ${OPENAMP_OVERLAY}		  \
    -i ${LOPS_DIR}/lop-load.dts           \
    -i ${LOPS_DIR}/lop-xlate-yaml.dts     \
    ${OPENAMP_LOPPER_INPUTS}              \
    ${OPENAMP_DTFILE}                     \
    ${LOPPER_OPENAMP_OUT_DTB}

    cd -
}

addtask run_lopper after do_unpack
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
                    return ret

        return ""


    def get_sum(a, b):
        return hex( int(a,16) + int(b, 16) )

    def get_rsc_mem_pa(base):
        return hex( int(base,16) + 0x20000 )

    def get_rsc_mem_pa_str(val):
        return "\"" + val.replace('0x','') + '.shm0' + "\""

    def get_ipi_str(val):
        return "\""+val.replace('0x','').lower() +'.openamp_ipi0'+"\""

    ELFLOADBASE =     parse_channel_info( "CHANNEL0ELFBASE", d)
    RSC_MEM_PA     = get_rsc_mem_pa( ELFLOADBASE )
    RSC_MEM_SIZE = "0x2000UL"
    VDEV0VRING0SIZE = parse_channel_info( "CHANNEL0VRING0SIZE", d)
    VDEV0VRING1SIZE = parse_channel_info( "CHANNEL0VRING1SIZE", d)

    d.setVar("IPI_DEV_NAME", get_ipi_str(parse_channel_info( "CHANNEL0TO_HOST", d)))
    d.setVar("IPI_CHN_BITMASK", parse_channel_info( "CHANNEL0TO_REMOTE-BITMASK", d))
    d.setVar("RSC_MEM_PA", RSC_MEM_PA)
    d.setVar("SHM_DEV_NAME", get_rsc_mem_pa_str( RSC_MEM_PA ))
    d.setVar("RSC_MEM_SIZE", "0x2000UL")
    d.setVar("VRING_MEM_PA", parse_channel_info( "CHANNEL0VRING0BASE", d))
    d.setVar("VRING_MEM_SIZE", get_sum( VDEV0VRING0SIZE, VDEV0VRING1SIZE ))
    d.setVar("SHARED_BUF_PA", parse_channel_info( "CHANNEL0VDEV0BUFFERBASE", d))
    d.setVar("SHARED_BUF_SIZE", parse_channel_info( "CHANNEL0VDEV0BUFFERSIZE", d))
}

# run lopper before parsing lopper-generated file
do_set_openamp_cmake_vars[prefuncs]  += "do_run_lopper"

# set openamp vars before using them in toolchain file
do_generate_toolchain_file[prefuncs] += "do_set_openamp_cmake_vars"

python openamp_toolchain_file_setup() {
    toolchain_file_path = d.getVar("WORKDIR") + "/toolchain.cmake"
    toolchain_file = open(toolchain_file_path, "a") # a for append

    # openamp app specific info
    config_vars = [ "IPI_CHN_BITMASK", "IPI_DEV_NAME", "SHM_DEV_NAME",
                    "RSC_MEM_PA", "RSC_MEM_SIZE", "VRING_MEM_PA",
                    "VRING_MEM_SIZE", "SHARED_BUF_PA", "SHARED_BUF_SIZE" ]

    defs = " "
    for cv in config_vars:
        defs += " -D" + cv + "=" + d.getVar(cv)

    toolchain_file.write("add_definitions( " + defs + " )")
}

do_generate_toolchain_file[postfuncs] += "openamp_toolchain_file_setup"

do_deploy() {
	install -Dm 0644 ${LOPPER_OPENAMP_OUT_DTB} ${DEPLOY_DIR_IMAGE}/
}

addtask deploy before do_build after do_install
