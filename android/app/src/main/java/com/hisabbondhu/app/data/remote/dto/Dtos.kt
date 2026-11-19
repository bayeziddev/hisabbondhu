package com.hisabbondhu.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OtpRequest(val phone: String)

@Serializable
data class OtpRequestResponse(
    val ok: Boolean = true,
    @SerialName("expires_in") val expiresIn: Int = 300,
    @SerialName("resend_after") val resendAfter: Int = 60,
    /** Only returned by non-production servers for testing. */
    @SerialName("dev_code") val devCode: String? = null,
)

@Serializable
data class VerifyOtpRequest(
    val phone: String,
    val code: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String,
)

@Serializable
data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int = 900,
    val user: UserDto? = null,
)

@Serializable
data class UserDto(val id: String, val phone: String)

@Serializable
data class OkResponse(val ok: Boolean = true)

@Serializable
data class ErrorResponse(val error: String = "error", val message: String? = null)

@Serializable
data class BusinessDto(
    val id: String,
    val name: String,
    @SerialName("owner_name") val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    @SerialName("business_type") val businessType: String = "general_store",
    @SerialName("opening_cash_paisa") val openingCashPaisa: Long = 0,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class PartyDto(
    val id: String,
    val type: String,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val note: String? = null,
    @SerialName("opening_balance_paisa") val openingBalancePaisa: Long = 0,
    @SerialName("reminder_at") val reminderAt: Long? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    val deleted: Boolean = false,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val category: String? = null,
    val sku: String? = null,
    val barcode: String? = null,
    val unit: String = "pcs",
    @SerialName("purchase_price_paisa") val purchasePricePaisa: Long = 0,
    @SerialName("sale_price_paisa") val salePricePaisa: Long = 0,
    @SerialName("opening_stock_milli") val openingStockMilli: Long = 0,
    @SerialName("min_stock_milli") val minStockMilli: Long = 0,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    val deleted: Boolean = false,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class TxDto(
    val id: String,
    val type: String,
    @SerialName("party_id") val partyId: String? = null,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("qty_milli") val qtyMilli: Long = 0,
    @SerialName("rate_paisa") val ratePaisa: Long = 0,
    @SerialName("unit_cost_paisa") val unitCostPaisa: Long = 0,
    @SerialName("discount_paisa") val discountPaisa: Long = 0,
    @SerialName("total_paisa") val totalPaisa: Long,
    @SerialName("paid_paisa") val paidPaisa: Long,
    val method: String = "CASH",
    val category: String? = null,
    val note: String? = null,
    @SerialName("occurred_at") val occurredAt: Long,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("device_id") val deviceId: String,
    @SerialName("reversal_of") val reversalOf: String? = null,
)

@Serializable
data class PushRequest(
    @SerialName("device_id") val deviceId: String,
    val parties: List<PartyDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
    val transactions: List<TxDto> = emptyList(),
)

@Serializable
data class RejectedRow(val entity: String, val id: String, val reason: String = "")

@Serializable
data class PushResponse(
    val ok: Boolean = true,
    val rejected: List<RejectedRow> = emptyList(),
)

@Serializable
data class Cursors(
    val parties: Long = 0,
    val products: Long = 0,
    val transactions: Long = 0,
)

@Serializable
data class PullRequest(val cursors: Cursors, val limit: Int = 500)

@Serializable
data class PullResponse(
    val business: BusinessDto? = null,
    val parties: List<PartyDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
    val transactions: List<TxDto> = emptyList(),
    val cursors: Cursors,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class ContactRequest(
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val subject: String,
    val message: String,
    val source: String = "android",
)
