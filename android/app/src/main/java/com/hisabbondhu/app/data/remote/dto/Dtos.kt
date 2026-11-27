package com.hisabbondhu.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OtpRequest(val email: String)

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
    val email: String,
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
    @SerialName("new_user") val newUser: Boolean = false,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val phone: String? = null,
    val name: String? = null,
    @SerialName("phone_verified") val phoneVerified: Boolean = false,
    @SerialName("marketing_opt_in") val marketingOptIn: Boolean = false,
    @SerialName("profile_complete") val profileComplete: Boolean = false,
)

@Serializable
data class ProfileRequest(
    val name: String,
    val phone: String,
    @SerialName("marketing_opt_in") val marketingOptIn: Boolean,
)

@Serializable
data class UserResponse(val user: UserDto)

@Serializable
data class BusinessListResponse(val businesses: List<BusinessDto> = emptyList())

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

// ---- Referral programme & Hisab points ----

@Serializable
data class ReferralApplyRequest(val code: String)

@Serializable
data class RedeemRequest(val option: String)

@Serializable
data class RedeemOptionDto(val id: String, val points: Int, val days: Int)

@Serializable
data class ReferralRulesDto(
    @SerialName("referrer_points") val referrerPoints: Int = 100,
    @SerialName("welcome_points") val welcomePoints: Int = 50,
    @SerialName("min_transactions") val minTransactions: Int = 5,
    @SerialName("apply_window_days") val applyWindowDays: Int = 7,
    @SerialName("redeem_options") val redeemOptions: List<RedeemOptionDto> = emptyList(),
)

@Serializable
data class ReferralStatsDto(val pending: Int = 0, val qualified: Int = 0)

@Serializable
data class PointsEntryDto(
    val delta: Int,
    val reason: String,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class ReferralSummary(
    val code: String,
    @SerialName("share_url") val shareUrl: String,
    val points: Int = 0,
    @SerialName("pro_until") val proUntil: Long? = null,
    @SerialName("pro_active") val proActive: Boolean = false,
    @SerialName("redeem_enabled") val redeemEnabled: Boolean = false,
    @SerialName("can_apply") val canApply: Boolean = false,
    @SerialName("applied_code") val appliedCode: String? = null,
    @SerialName("applied_status") val appliedStatus: String? = null,
    val stats: ReferralStatsDto = ReferralStatsDto(),
    val rules: ReferralRulesDto = ReferralRulesDto(),
    val history: List<PointsEntryDto> = emptyList(),
)
