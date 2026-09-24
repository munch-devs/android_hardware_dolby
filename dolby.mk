#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from the proprietary version
$(call inherit-product, vendor/dolby/dolby-vendor.mk)

DOLBY_PATH := vendor/dolby

# Configs
PRODUCT_COPY_FILES += \
    $(DOLBY_PATH)/configs/media/media_codecs_c2_dolby_audio.xml:$(TARGET_COPY_OUT_VENDOR)/etc/media_codecs_c2_dolby_audio.xml

ifneq ($(TARGET_USES_CUSTOM_DOLBY_XML),true)
PRODUCT_COPY_FILES += \
    $(DOLBY_PATH)/configs/dolby/multimedia_dolby_dax_default.xml:$(TARGET_COPY_OUT_ODM)/etc/dolby/multimedia_dolby_dax_default.xml
endif

# DolbyManager
PRODUCT_PACKAGES += \
    DolbyAtmos

# Overlay-RRO
PRODUCT_PACKAGES += \
    DolbyResCommon

# Properties
PRODUCT_ODM_PROPERTIES += \
    ro.vendor.dolby.dax.version=DAX3_3.6.0.12_r1 \
    ro.vendor.dolby.model=PAFM00 \
    ro.vendor.dolby.device=OP46C3 \
    ro.vendor.dolby.manufacturer=OPLUS \
    ro.vendor.dolby.brand=OPLUS

# Sepolicy
BOARD_VENDOR_SEPOLICY_DIRS += \
    $(DOLBY_PATH)/sepolicy/vendor

# VINTF
DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE += $(DOLBY_PATH)/configs/hidl/dolby_framework_matrix.xml
ODM_MANIFEST_FILES += $(DOLBY_PATH)/configs/hidl/c2_manifest.xml
