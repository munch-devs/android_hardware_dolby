#
# Automatically generated file. DO NOT MODIFY
#

PRODUCT_SOONG_NAMESPACES += \
    vendor/dolby/common

PRODUCT_COPY_FILES += \
    vendor/dolby/common/proprietary/vendor/etc/init/vendor.dolby.hardware.dms@2.0-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.dolby.hardware.dms@2.0-service.rc \
    vendor/dolby/common/proprietary/vendor/etc/init/vendor.dolby.media.c2@1.0-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.dolby.media.c2@1.0-service.rc \
    vendor/dolby/common/proprietary/vendor/etc/media_codecs_dolby_audio.xml:$(TARGET_COPY_OUT_VENDOR)/etc/media_codecs_dolby_audio.xml

PRODUCT_PACKAGES += \
    libdapparamstorage \
    libdlbpreg \
    libdlbvol \
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
    liboem_specific \
    vendor.dolby.hardware.dms@2.0-impl \
    vendor.dolby.hardware.dms.xml \
    vendor.dolby.media.c2@1.0-service.xml \
    vendor.dolby.hardware.dms@2.0-service \
    vendor.dolby.media.c2@1.0-service
