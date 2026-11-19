package com.hisabbondhu.app.data.repo

import com.hisabbondhu.app.data.local.PartyEntity
import com.hisabbondhu.app.data.local.ProductEntity
import com.hisabbondhu.app.data.local.TxEntity
import com.hisabbondhu.app.data.prefs.BusinessProfile
import com.hisabbondhu.app.data.remote.dto.BusinessDto
import com.hisabbondhu.app.data.remote.dto.PartyDto
import com.hisabbondhu.app.data.remote.dto.ProductDto
import com.hisabbondhu.app.data.remote.dto.TxDto
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.domain.PayMethod
import com.hisabbondhu.app.domain.SyncState
import com.hisabbondhu.app.domain.TxType

fun BusinessProfile.toDto() = BusinessDto(
    id = id, name = name, ownerName = ownerName, phone = phone, address = address,
    businessType = type, openingCashPaisa = openingCashPaisa, updatedAt = updatedAt,
)

fun BusinessDto.toProfile() = BusinessProfile(
    id = id, name = name, ownerName = ownerName, phone = phone, address = address,
    type = businessType, openingCashPaisa = openingCashPaisa, updatedAt = updatedAt,
)

fun PartyEntity.toDto() = PartyDto(
    id = id, type = type.name, name = name, phone = phone, address = address, note = note,
    openingBalancePaisa = openingBalancePaisa, reminderAt = reminderAt, createdAt = createdAt,
    updatedAt = updatedAt, deleted = deleted, deviceId = deviceId,
)

fun PartyDto.toEntityOrNull(): PartyEntity? {
    val t = runCatching { PartyType.valueOf(type) }.getOrNull() ?: return null
    return PartyEntity(
        id = id, type = t, name = name, phone = phone, address = address, note = note,
        openingBalancePaisa = openingBalancePaisa, reminderAt = reminderAt, createdAt = createdAt,
        updatedAt = updatedAt, deleted = deleted, deviceId = deviceId, syncState = SyncState.SYNCED,
    )
}

fun ProductEntity.toDto() = ProductDto(
    id = id, name = name, category = category, sku = sku, barcode = barcode, unit = unit,
    purchasePricePaisa = purchasePricePaisa, salePricePaisa = salePricePaisa,
    openingStockMilli = openingStockMilli, minStockMilli = minStockMilli, createdAt = createdAt,
    updatedAt = updatedAt, deleted = deleted, deviceId = deviceId,
)

fun ProductDto.toEntity() = ProductEntity(
    id = id, name = name, category = category, sku = sku, barcode = barcode, unit = unit,
    purchasePricePaisa = purchasePricePaisa, salePricePaisa = salePricePaisa,
    openingStockMilli = openingStockMilli, minStockMilli = minStockMilli, createdAt = createdAt,
    updatedAt = updatedAt, deleted = deleted, deviceId = deviceId, syncState = SyncState.SYNCED,
)

fun TxEntity.toDto() = TxDto(
    id = id, type = type.name, partyId = partyId, productId = productId, qtyMilli = qtyMilli,
    ratePaisa = ratePaisa, unitCostPaisa = unitCostPaisa, discountPaisa = discountPaisa,
    totalPaisa = totalPaisa, paidPaisa = paidPaisa, method = method.name, category = category,
    note = note, occurredAt = occurredAt, createdAt = createdAt, deviceId = deviceId, reversalOf = reversalOf,
)

fun TxDto.toEntityOrNull(): TxEntity? {
    val t = runCatching { TxType.valueOf(type) }.getOrNull() ?: return null
    val m = runCatching { PayMethod.valueOf(method) }.getOrDefault(PayMethod.OTHER)
    return TxEntity(
        id = id, type = t, partyId = partyId, productId = productId, qtyMilli = qtyMilli,
        ratePaisa = ratePaisa, unitCostPaisa = unitCostPaisa, discountPaisa = discountPaisa,
        totalPaisa = totalPaisa, paidPaisa = paidPaisa, method = m, category = category, note = note,
        occurredAt = occurredAt, createdAt = createdAt, deviceId = deviceId, reversalOf = reversalOf,
        syncState = SyncState.SYNCED,
    )
}
