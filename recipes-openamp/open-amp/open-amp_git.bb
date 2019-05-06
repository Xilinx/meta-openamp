SRCBRANCH ?= "master"
SRCREV ?= "f9039c27a00caa7f1548ffd53d863776edc6f223"
LIC_FILES_CHKSUM ?= "file://LICENSE.md;md5=a8d8cf662ef6bf9936a1e1413585ecbf"
PV = "${SRCBRANCH}+git${SRCPV}"

include open-amp.inc
