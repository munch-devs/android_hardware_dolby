#
# Automatically generated file. DO NOT MODIFY
#

PRODUCT_SOONG_NAMESPACES += \
    vendor/dolby

PRODUCT_COPY_FILES += \
    vendor/dolby/proprietary/odm/etc/init/vendor.dolby.media.c2@1.0-service.rc:$(TARGET_COPY_OUT_ODM)/etc/init/vendor.dolby.media.c2@1.0-service.rc \
    vendor/dolby/proprietary/odm/etc/init/vendor.dolby_sp.hardware.dmssp@2.0-service.rc:$(TARGET_COPY_OUT_ODM)/etc/init/vendor.dolby_sp.hardware.dmssp@2.0-service.rc \
    vendor/dolby/proprietary/odm/etc/init/vendor.dolby_v3_6.hardware.dms360@2.0-service.rc:$(TARGET_COPY_OUT_ODM)/etc/init/vendor.dolby_v3_6.hardware.dms360@2.0-service.rc

PRODUCT_PACKAGES += \
    libdapparamstorage_v3_6 \
    libdeccfg_v3_6 \
    libdlbdsservice_v3_6 \
    vendor.dolby_v3_6.hardware.dms360@2.0 \
    libstagefright_soft_ddpdec \
    libhwdap_v3_6 \
    libswdap_v3_6 \
    libcodec2_hidl@1.0_sp \
    libcodec2_hidl_plugin_sp \
    libcodec2_soft_ac4dec_sp \
    libcodec2_soft_common_sp \
    libcodec2_soft_ddpdec_sp \
    libcodec2_store_dolby_sp \
    libcodec2_vndk_sp \
    libdapparamstorage_sp \
    libdeccfg_sp \
    libdlbdsservice_sp \
    libui_sp \
    vendor.dolby_sp.hardware.dmssp@2.0-impl \
    vendor.dolby_sp.hardware.dmssp@2.0 \
    vendor.dolby_v3_6.hardware.dms360@2.0-impl \
    manifest_dax_dolby_v3_6.xml \
    vendor.dolby.hardware.dms.xml \
    vendor.dolby_sp.hardware.dmssp@2.0-service \
    vendor.dolby_sp.media.c2@1.0-service \
    vendor.dolby_v3_6.hardware.dms360@2.0-service
