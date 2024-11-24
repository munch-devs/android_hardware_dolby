#
# Automatically generated file. DO NOT MODIFY
#

PRODUCT_SOONG_NAMESPACES += \
    hardware/dolby

PRODUCT_COPY_FILES += \
    hardware/dolby/proprietary/vendor/etc/init/vendor.dolby.hardware.dms@2.0-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.dolby.hardware.dms@2.0-service.rc \
    hardware/dolby/proprietary/vendor/etc/init/vendor.dolby.media.c2@1.0-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.dolby.media.c2@1.0-service.rc

PRODUCT_PACKAGES += \
    libdapparamstorage \
    libdlbpreg \
    libdlbvol \
    libspatialaudio \
    libswdap \
    libswgamedap \
    libswvqe \
    vendor.dolby.hardware.dms@2.0 \
    libcodec2_soft_ac4dec \
    libcodec2_soft_ddpdec \
    libcodec2_soft_dolby \
    libcodec2_store_dolby \
    libdeccfg \
    libdlbdsservice \
    vendor.dolby.hardware.dms@2.0-impl \
    vendor.dolby.hardware.dms@2.0-service \
    vendor.dolby.media.c2@1.0-service
