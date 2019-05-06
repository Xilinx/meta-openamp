SRCBRANCH ?= "master"
SRCREV ?= "a4d606a40535c8be029d01315303c2608359d789"
LIC_FILES_CHKSUM ?= "file://LICENSE.md;md5=fe0b8a4beea8f0813b606d15a3df3d3c"
PV = "${SRCBRANCH}+git${SRCPV}"

include libmetal.inc
