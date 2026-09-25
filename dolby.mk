#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from the proprietary version
$(call inherit-product, vendor/dolby/common/common-vendor.mk)

DOLBY_PATH := vendor/dolby

# Configs
PRODUCT_COPY_FILES += \
    $(DOLBY_PATH)/configs/dolby/dax-default.xml:$(TARGET_COPY_OUT_VENDOR)/etc/dolby/dax-default.xml

# Properties
PRODUCT_VENDOR_PROPERTIES += \
    ro.vendor.dolby.dax.version=DAX3_3.7.0.8_r1 \
    vendor.audio.dolby.ds2.enabled=false \
    vendor.audio.dolby.ds2.hardbypass=false

# Sepolicy
BOARD_VENDOR_SEPOLICY_DIRS += \
    $(DOLBY_PATH)/sepolicy/vendor
