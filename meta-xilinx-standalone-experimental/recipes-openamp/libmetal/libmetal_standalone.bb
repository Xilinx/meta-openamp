require ${LAYER_PATH_openamp-layer}/recipes-openamp/libmetal/libmetal.inc

SRCREV = "${AUTOREV}"
S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

SRC_URI_armrm_xilinx-standalone = "git://gitenterprise.xilinx.com/OpenAMP/libmetal.git;branch=xlnx_decoupling"

OECMAKE_SOURCEPATH = "${S}/"
PROVIDES_armrm_xilinx-standalone = "libmetal "
DEPENDS_armrm_xilinx-standalone += " libxil scugic doxygen-native xilstandalone"
inherit cmake
LICENSE = "BSD"
LIC_FILES_CHKSUM = "file://LICENSE.md;md5=1ff609e96fc79b87da48a837cbe5db33"

EXTRA_OECMAKE_armrm_xilinx-standalone = " \
	-DLIB_INSTALL_DIR=${libdir} \
	-DSOC_FAMILY="${SOC_FAMILY}" \
	-DWITH_EXAMPLES=ON \
"

ALLOW_EMPTY_${PN}-demos = "1"

FILES_${PN}-demos_armrm_xilinx-standalone = " \
    ${bindir}/libmetal_* \
    ${bindir}/*ocm_demo.elf \
"

COMPATIBLE_HOST = ".*-elf"
COMPATIBLE_HOST_arm = "[^-]*-[^-]*-eabi"

def get_cross_prefix(oe_cmake_c_compiler):
  if oe_cmake_c_compiler == 'arm-xilinx-eabi-gcc':
    return 'arm-xilinx-eabi-'

LIBMETAL_CROSS_PREFIX_armrm_xilinx-standalone = "${@get_cross_prefix(d.getVar('OECMAKE_C_COMPILER'))}"

def get_libmetal_machine(soc_family):
  if soc_family in ['versal']:
    return 'zynqmp_r5'
  return ''


LIBMETAL_MACHINE_armrm_xilinx-standalone = "${@get_libmetal_machine(d.getVar('SOC_FAMILY'))}"

cmake_do_generate_toolchain_file_armrm_xilinx-standalone_append() {
    cat >> ${WORKDIR}/toolchain.cmake <<EOF
    set( CMAKE_SYSTEM_PROCESSOR "${TRANSLATED_TARGET_ARCH}" )
    set( MACHINE "${LIBMETAL_MACHINE}" )
    set( CMAKE_MACHINE "${LIBMETAL_CMAKE_MACHINE}" )
    SET(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ")
    SET(CMAKE_C_ARCHIVE_CREATE "<CMAKE_AR> qcs <TARGET> <LINK_FLAGS> <OBJECTS>")
    set( CMAKE_SYSTEM_NAME "Generic")
    set (CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER CACHE STRING "")
    set (CMAKE_FIND_ROOT_PATH_MODE_LIBRARY NEVER CACHE STRING "")
    set (CMAKE_FIND_ROOT_PATH_MODE_INCLUDE NEVER CACHE STRING "")

    include (CMakeForceCompiler)
    CMAKE_FORCE_C_COMPILER("${OECMAKE_C_COMPILER}" GNU)
    set (CROSS_PREFIX           "${LIBMETAL_CROSS_PREFIX}" CACHE STRING "")
    set (CMAKE_LIBRARY_PATH "${S}/../recipe-sysroot/usr/lib" CACHE STRING "")
    SET(CMAKE_C_ARCHIVE_FINISH   true)
    set (CMAKE_INCLUDE_PATH "${S}/../recipe-sysroot/usr/include/" CACHE STRING "")
    include (cross-generic-gcc)
    add_definitions(-DWITH_DOC=OFF)
EOF
}