package com.hisabbondhu.app.ui.screens.tx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisabbondhu.app.AppContainer
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.core.Money
import com.hisabbondhu.app.core.Qty
import com.hisabbondhu.app.data.local.PartyEntity
import com.hisabbondhu.app.data.local.ProductEntity
import com.hisabbondhu.app.data.local.ProductWithStock
import com.hisabbondhu.app.data.local.TxEntity
import com.hisabbondhu.app.data.repo.LedgerValidationException
import com.hisabbondhu.app.data.repo.TxDraft
import com.hisabbondhu.app.domain.Ledger
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.domain.PayMethod
import com.hisabbondhu.app.domain.TxType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TxEntryViewModel(private val c: AppContainer, state: SavedStateHandle) : ViewModel() {
    val type: TxType = TxType.valueOf(checkNotNull(state.get<String>("type")))
    private val initialPartyId: String? = state["partyId"]
    private val initialProductId: String? = state["productId"]

    var party by mutableStateOf<PartyEntity?>(null)
    var product by mutableStateOf<ProductEntity?>(null)
    var amount by mutableStateOf("")
    var qty by mutableStateOf("1")
    var rate by mutableStateOf("")
    var discount by mutableStateOf("")
    var paid by mutableStateOf("")
    var credit by mutableStateOf(initialPartyId != null && (type == TxType.SALE || type == TxType.PURCHASE))
    var method by mutableStateOf(PayMethod.CASH)
    var category by mutableStateOf("other")
    var note by mutableStateOf("")
    var occurredAt by mutableStateOf(Dates.now())
    var increase by mutableStateOf(true)
    var error by mutableStateOf<LedgerValidationException.Reason?>(null)
    var saved by mutableStateOf<TxEntity?>(null)
    var saving by mutableStateOf(false)

    val parties = c.ledger.allParties().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val products = c.ledger.products().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val showsParty = type !in setOf(TxType.EXPENSE, TxType.STOCK_ADJUST, TxType.CASH_IN, TxType.CASH_OUT)
    val showsProduct = Ledger.usesProduct(type)
    val allowsCredit = Ledger.allowsCredit(type)
    val defaultPartyType: PartyType = Ledger.defaultPartyType(type) ?: PartyType.CUSTOMER

    init {
        viewModelScope.launch {
            initialPartyId?.let { party = c.ledger.getParty(it) }
            initialProductId?.let { id -> c.ledger.getProduct(id)?.let { selectProduct(it) } }
        }
    }

    fun selectProduct(p: ProductEntity) {
        product = p
        val price = if (type == TxType.SALE || type == TxType.SALE_RETURN) p.salePricePaisa else p.purchasePricePaisa
        rate = Money.toInput(price)
        if (qty.isBlank()) qty = "1"
        error = null
    }

    fun selectProduct(p: ProductWithStock) = selectProduct(p.product)

    fun clearProduct() {
        product = null
        rate = ""
        discount = ""
    }

    fun quickAddParty(name: String, phone: String, partyType: PartyType) {
        viewModelScope.launch {
            val id = c.ledger.saveParty(null, partyType, name, phone, null, null, 0, null)
            party = c.ledger.getParty(id)
            error = null
        }
    }

    val qtyMilli: Long get() = Qty.parseToMilli(qty) ?: 0

    val totalPaisa: Long
        get() = when {
            type == TxType.STOCK_ADJUST -> 0
            product != null -> maxOf(0, Qty.lineTotal(qtyMilli, Money.parseToPaisa(rate) ?: 0) - (Money.parseToPaisa(discount) ?: 0))
            else -> Money.parseToPaisa(amount) ?: 0
        }

    val paidPaisa: Long
        get() = when {
            type == TxType.STOCK_ADJUST -> 0
            allowsCredit && credit -> Money.parseToPaisa(paid) ?: 0
            else -> totalPaisa
        }

    val duePaisa: Long get() = (totalPaisa - paidPaisa).coerceAtLeast(0)

    fun save() {
        if (saving) return
        error = null
        val draft = TxDraft(
            type = type,
            partyId = party?.id,
            productId = product?.id,
            qtyMilli = if (type == TxType.STOCK_ADJUST) (if (increase) qtyMilli else -qtyMilli) else if (product != null) qtyMilli else 0,
            ratePaisa = if (product != null) Money.parseToPaisa(rate) ?: 0 else 0,
            discountPaisa = if (product != null) Money.parseToPaisa(discount) ?: 0 else 0,
            totalPaisa = totalPaisa,
            paidPaisa = paidPaisa,
            method = method,
            category = if (type == TxType.EXPENSE) category else null,
            note = note,
            occurredAt = occurredAt,
        )
        saving = true
        viewModelScope.launch {
            try {
                saved = c.ledger.addTransaction(draft)
            } catch (e: LedgerValidationException) {
                error = e.reason
            } finally {
                saving = false
            }
        }
    }

    /** Clear the form for the next entry but keep the type (fast repeated entry at the counter). */
    fun reset() {
        saved = null
        party = null
        product = null
        amount = ""; qty = "1"; rate = ""; discount = ""; paid = ""; note = ""
        credit = false
        occurredAt = Dates.now()
        error = null
    }

    suspend fun partyBalance(id: String): Long? = c.ledger.partyWithBalance(id)?.receivablePaisa
    suspend fun businessName(): String = c.settings.currentBusiness()?.name.orEmpty()
}
