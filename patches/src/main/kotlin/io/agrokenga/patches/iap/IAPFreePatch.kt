package io.agrokenga.patches.iap

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import io.agrokenga.patches.native.patchMethod
import java.util.logging.Logger

private val log = Logger.getLogger("IAPFreePatch")

/**
 * KoPP2 2.21.4 IAP bypass — makes all in-game store purchases free.
 *
 * Patches `StoreService.PurchaseItem` to immediately return without contacting
 * the PlayFab server. This bypasses the server-side purchase validation and
 * price checks, effectively making all items free.
 *
 * Note: This only affects the PlayFab-backed store purchases. Platform IAP
 * (Google Play/Apple billing) uses a different path through `IAPService`.
 *
 * RVA and file offset come from the matching Il2CppDumper output for each ABI:
 * - arm64:   PurchaseItem RVA 0x251EDBC, file offset 0x251ADBC
 * - armv7:   PurchaseItem RVA 0x14A05B0, file offset 0x14A05B0
 *
 * These offsets are version-specific. Re-dump both ABIs before reusing them on a
 * different app version.
 */
private const val ARM64_PURCHASE_ITEM_RVA = 0x251EDBCL
private const val ARM64_PURCHASE_ITEM_FILE_OFFSET = 0x251ADBCL
private const val ARMV7_PURCHASE_ITEM_RVA = 0x14A05B0L
private const val ARMV7_PURCHASE_ITEM_FILE_OFFSET = 0x14A05B0L

// ARM64 `ret`
private val ARM64_RET = byteArrayOf(0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte())

// ARMv7 ARM-mode `bx lr`
private val ARMV7_BX_LR = byteArrayOf(0x1E.toByte(), 0xFF.toByte(), 0x2F.toByte(), 0xE1.toByte())

val iapFreePatch = resourcePatch(
    name = "IAP Free (All Items)",
    description = "Makes all in-game store purchases free by bypassing the PlayFab server. " +
        "Patches StoreService.PurchaseItem to return immediately without contacting the server. " +
        "Items are granted locally without server validation.",
    default = false,
) {
    compatibleWith(
        Compatibility(
            name = "KoPP2",
            packageName = "com.paradoxplaza.kopp2",
            apkFileType = ApkFileType.APKM,
            appIconColor = 0xFFD700,
            targets = listOf(AppTarget(version = "2.21.4")),
        ),
    )

    val enableFreeIAP = booleanOption(
        key = "iapFreeEnable",
        default = false,
        title = "Make all store items free",
        description = "Patch StoreService.PurchaseItem to bypass the PlayFab server and " +
            "return immediately. All store purchases will succeed without contacting the server. " +
            "Disabled by default.",
    )

    execute {
        log.info("=== IAP Free patch ===")

        if (enableFreeIAP.value != true) {
            log.info("Disabled; set iapFreeEnable=true to make all store items free.")
        } else {
            val arm64 = get("lib/arm64-v8a/libil2cpp.so")
            if (arm64.exists()) {
                patchMethod(
                    file = arm64,
                    abi = "arm64-v8a",
                    methodName = "StoreService.PurchaseItem",
                    rva = ARM64_PURCHASE_ITEM_RVA,
                    expectedOffset = ARM64_PURCHASE_ITEM_FILE_OFFSET,
                    patch = ARM64_RET,
                )
            } else {
                log.warning(
                    "MISSING lib/arm64-v8a/libil2cpp.so. The APKM must be merged before patching; " +
                        "base.apk alone has no lib/ directory.",
                )
            }

            val armv7 = get("lib/armeabi-v7a/libil2cpp.so")
            if (armv7.exists()) {
                patchMethod(
                    file = armv7,
                    abi = "armeabi-v7a",
                    methodName = "StoreService.PurchaseItem",
                    rva = ARMV7_PURCHASE_ITEM_RVA,
                    expectedOffset = ARMV7_PURCHASE_ITEM_FILE_OFFSET,
                    patch = ARMV7_BX_LR,
                )
            } else {
                log.warning(
                    "MISSING lib/armeabi-v7a/libil2cpp.so. The APKM must be merged before patching; " +
                        "base.apk alone has no lib/ directory.",
                )
            }
        }
    }
}
