package com.morphe.patches.iap

import com.morphe.core.api.*
import com.morphe.core.api.HookMethod
import com.morphe.core.api.HookPhase
import com.morphe.core.api.MethodCallContext
import com.morphe.core.api.PatchContext
import kotlinx.coroutines.CompletableFuture
import java.util.Collections
import java.util.concurrent.CompletableFuture

/**
 * Bypasses PlayFab IAP validation by mocking the Catalog API response.
 * 
 * This patch intercepts PlayFabDataService.ExecuteAsync calls that contain
 * PurchaseRequest data, extracting the requested item and returning a matching
 * fulfillment response without contacting the actual backend.
 * 
 * @author Morphe Patches
 * @version 1.0.0
 */
@HookMethod(
    className = "Victor.PlayFabDataService",
    methodName = "ExecuteAsync",
    phase = HookPhase.BEFORE,
    priority = 100
)
public fun PlayFabDataService_ExecuteAsync(
    request: Request,
    customData: Any?,
    headers: Map<String, String>?
): CompletableFuture<PlayFabResult> {
    
    val url = request?.getUrl() ?: return super.ExecuteAsync(request, customData, headers)
    
    // Detect IAP-related PlayFab Catalog calls
    if (url.contains("/Catalog/") && 
        customData != null && 
        customData.javaClass.name.contains("PurchaseRequest", ignoreCase = true)) {
        
        // Extract requested item details from PurchaseRequest
        val purchaseRequest = customData as? PurchaseRequest ?: return super.ExecuteAsync(request, customData, headers)
        
        val requestedItemId = purchaseRequest.itemId ?: return super.ExecuteAsync(request, customData, headers)
        val requestedPrice = purchaseRequest.price
        val requestedStoreId = purchaseRequest.storeId
        
        // Build dynamic fulfillment matching the exact request
        val reportItem = PlayFabReport.ItemBuilder()
            .setId(requestedItemId)
            .setCurrency("USD")
            .setAmount(requestedPrice)
            .build()
        
        val fulfillmentItem = PlayFabResult.ItemBuilder()
            .setId(requestedItemId)
            .setCatalogItemId(requestedItemId)
            .setCount(1)
            .build()
        
        val mockResult = PlayFabResult.Builder()
            .setReport(PlayFabReport.Builder().addItem(reportItem).build())
            .setItems(Collections.singletonList(fulfillmentItem))
            .build()
        
        return CompletableFuture.completedFuture(mockResult)
    }
    
    // For all other calls, delegate to the real implementation
    return super.ExecuteAsync(request, customData, headers)
}

/**
 * Inner class representing the PurchaseRequest data structure.
 * This must match the actual class used in the target app.
 */
private open class PurchaseRequest(
    val catalogVersion: String?,
    val storeId: String?,
    val itemId: String?,
    val price: Int
)
