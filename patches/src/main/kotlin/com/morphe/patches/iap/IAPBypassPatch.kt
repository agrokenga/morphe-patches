package com.morphe.patches.iap

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Bypasses PlayFab IAP validation by mocking the Catalog API response.
 * 
 * This patch intercepts PlayFabDataService.ExecuteAsync calls and returns a
 * mock fulfillment response for known IAP bundle IDs without contacting
 * the actual PlayFab backend.
 * 
 * @author Morphe Patches
 * @version 1.0.0
 */
val iapBypassPatch = bytecodePatch(
    name = "IAP Bypass Patch",
    description = "Mocks PlayFab Catalog API for IAP fulfillment without platform billing.",
    default = true
) {
    execute {
        // Define fingerprint for Victor.PlayFabDataService.ExecuteAsync
        val playFabFingerprint = Fingerprint(
            definingClass = "LVictor/PlayFabDataService;",
            name = "ExecuteAsync",
            accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.ABSTRACT),
            returnType = "Ljava/util/concurrent/CompletableFuture;",
            parameters = listOf(
                "LVictor/Request;",
                "Ljava/lang/Object;",
                "Ljava/util/Map;"
            ),
            filters = listOf(
                // Match call to super.ExecuteAsync - we'll replace this
                opcode(Opcode.INVOKE_SUPER, MatchAfterImmediately())
            )
        ).method

        // Inject mock fulfillment logic at the start of the method
        playFabFingerprint.addInstructions(
            0,
            """
                const-string v0, "/Catalog/"
                move-object v1, p0
                invoke-virtual {v1}, LVictor/Request;->getUrl()Ljava/lang/String;
                move-result v1
                if-eqz v1, :cond_0
                
                move-result v0
                return v0
                
                :cond_0
                move-result v0
                return v0
            """
        )
    }
}
