LIBMETAL_DEF:zynqmp = " -DPLATFORM_ZYNQMP"
LIBMETAL_DEF:versal = " -Dversal"
LIBMETAL_DEF:versal-net = " -DVERSAL_NET"
LIBMETAL_DEF:versal-2ve-2vm = " -DVERSAL_NET -DIS_VERSAL2 "
LIBMETAL_DEF ?= ""
TARGET_CFLAGS:append = " ${LIBMETAL_DEF} "

EXTRA_OECMAKE:append = " -DPROJECT_VENDOR=xlnx "
FILES:${PN}:append = " ${bindir}/*demo "
