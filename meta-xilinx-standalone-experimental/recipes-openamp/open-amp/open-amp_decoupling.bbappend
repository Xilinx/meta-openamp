OPENAMP_MC_DEPENDS:versal = "mc::cortexr5-versal-baremetal:open-amp:do_deploy"
OPENAMP_MC_DEPENDS:zynqmp = "mc::cortexr5-zynqmp-baremetal:open-amp:do_deploy"

FW_DEPLOY_DIR:versal = "${BASE_TMPDIR}/tmp-cortexr5-versal-baremetal/deploy/images/${MACHINE}"
FW_DEPLOY_DIR:zynqmp = "${BASE_TMPDIR}/tmp-cortexr5-zynqmp-baremetal/deploy/images/${MACHINE}"

do_fetch[mcdepends] += "${OPENAMP_MC_DEPENDS}"
FW_LIB_DIR = "/lib/firmware"

do_install:append() {
	install -d ${D}${FW_LIB_DIR}
	dest=${D}${FW_LIB_DIR}/
	install -m 0644 ${FW_DEPLOY_DIR}/matrix_multiplyd.out ${dest}
	install -m 0644 ${FW_DEPLOY_DIR}/rpc_demo.out ${dest}
	install -m 0644 ${FW_DEPLOY_DIR}/rpmsg-echo.out ${dest}


}

FILES:${PN}:append = "${FW_LIB_DIR}/*out"
INSANE_SKIP:${PN} = "arch"                                                         
