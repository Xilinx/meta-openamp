SRCBRANCH ?= "master-rel-2021.1"
SRCREV = "31d7099a077e03e45198aa9324ada982c896a172"
BRANCH = "master-rel-2021.1"
LIC_FILES_CHKSUM ?= "file://LICENSE.md;md5=0e6d7bfe689fe5b0d0a89b2ccbe053fa"
PV = "${SRCBRANCH}+git${SRCPV}"

include open-amp.inc
