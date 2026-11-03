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
 * Based on Ghidra reverse engineering of the actual call stack:
 *
 * Premium bundle IAP path:
 *   IAPButton.PurchaseClicked → ScreenHelper.ShowPurchaseConfirmation →
 *   IAPButton.PurchaseCallback → IAPService.PurchaseBundle → Unity IAP →
 *   GooglePlayIAP.ProcessPurchase → PlayFabApiCall<object,object>.Execute (0x2CA7DF0)
 *
 * In-game store path (gold/items):
 *   StoreService.PurchaseItem (0x251EDBC) → CloudScriptFunction.Execute →
 *   CloudScriptFunctionBase.SendRequest → PlayFabCloudScriptAPI.ExecuteFunction
 *
 * This patch targets both paths:
 * 1. PlayFabApiCall<object,object>.Execute at 0x2CA7DF0 — intercepts premium bundle purchases
 *    before they reach PlayFabClientAPI.ValidateGooglePlayPurchase
 * 2. StoreService.PurchaseItem at 0x251EDBC — intercepts in-game store purchases before
 *    they reach the CloudScript path
 *
 * Note: PlayFabApiCall<object,object>.ExecuteAsync (0x2CA7F3C) is NOT the purchase path.
 * It's used for catalog/config/save/auth operations. The purchase path uses the callback
 * overload PlayFabApiCall<object,object>.Execute.
 *
 * RVA and file offset come from Ghidra analysis of libil2cpp.so:
 * - PlayFabApiCall<object,object>.Execute: RVA 0x2CA7DF0, file offset 0x2CA3DF0
 * - StoreService.PurchaseItem: RVA 0x251EDBC, file offset 0x251ADBC
 *
 * These offsets are version-specific. Re-dump both ABIs before reusing them on a
 * different app version.
 */
private const val ARM64_PLAYFAB_EXECUTE_RVA = 0x2CA7DF0L
private const val ARM64_PLAYFAB_EXECUTE_FILE_OFFSET = 0x2CA3DF0L
private const val ARM64_STORE_PURCHASE_RVA = 0x251EDBCL
private const val ARM64_STORE_PURCHASE_FILE_OFFSET = 0x251ADBCL

private const val ARMV7_PLAYFAB_EXECUTE_RVA = 0x1D48E38L
private const val ARMV7_PLAYFAB_EXECUTE_FILE_OFFSET = 0x1D48E38L
private const val ARMV7_STORE_PURCHASE_RVA = 0x14A05B0L
private const val ARMV7_STORE_PURCHASE_FILE_OFFSET = 0x14A05B0L

// ARM64 `ret`
private val ARM64_RET = byteArrayOf(0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte())

// ARMv7 ARM-mode `bx lr`
private val ARMV7_BX_LR = byteArrayOf(0x1E.toByte(), 0xFF.toByte(), 0x2F.toByte(), 0xE1.toByte())

val iapFreePatch = resourcePatch(
    name = "IAP Free (All Items)",
    description = "Makes all in-game store purchases free by bypassing the PlayFab server. " +
        "Patches PlayFabApiCall.Execute (premium bundles) and StoreService.PurchaseItem " +
        "(in-game store) to return immediately without contacting the server.",
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
        description = "Patch PlayFabApiCall.Execute and StoreService.PurchaseItem to bypass " +
            "the PlayFab server and return immediately. All store purchases will succeed " +
            "without contacting the server. Disabled by default.",
    )

    execute {
        log.info("=== IAP Free patch ===")

        if (enableFreeIAP.value != true) {
            log.info("Disabled; set iapFreeEnable=true to make all store items free.")
        } else {
            val arm64 = get("lib/arm64-v8a/libil2cpp.so")
            if (arm64.exists()) {
                // Patch PlayFabApiCall<object,object>.Execute — premium bundle purchases
                patchMethod(
                    file = arm64,
                    abi = "arm64-v8a",
                    methodName = "PlayFabApiCall<object,object>.Execute",
                    rva = ARM64_PLAYFAB_EXECUTE_RVA,
                    expectedOffset = ARM64_PLAYFAB_EXECUTE_FILE_OFFSET,
                    patch = ARM64_RET,
                )

                // Patch StoreService.PurchaseItem — in-game store purchases
                patchMethod(
                    file = arm64,
                    abi = "arm64-v8a",
                    methodName = "StoreService.PurchaseItem",
                    rva = ARM64_STORE_PURCHASE_RVA,
                    expectedOffset = ARM64_STORE_PURCHASE_FILE_OFFSET,
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
                // Patch PlayFabApiCall<object,object>.Execute — premium bundle purchases
                patchMethod(
                    file = armv7,
                    abi = "armeabi-v7a",
                    methodName = "PlayFabApiCall<object,object>.Execute",
                    rva = ARMV7_PLAYFAB_EXECUTE_RVA,
                    expectedOffset = ARMV7_PLAYFAB_EXECUTE_FILE_OFFSET,
                    patch = ARMV7_BX_LR,
                )

                // Patch StoreService.PurchaseItem — in-game store purchases
                patchMethod(
                    file = armv7,
                    abi = "armeabi-v7a",
                    methodName = "StoreService.PurchaseItem",
                    rva = ARMV7_STORE_PURCHASE_RVA,
                    expectedOffset = ARMV7_STORE_PURCHASE_FILE_OFFSET,
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
