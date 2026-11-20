package com.hisabbondhu.app.ui.nav

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hisabbondhu.app.AppContainer
import com.hisabbondhu.app.R
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.domain.TxType
import com.hisabbondhu.app.ui.screens.home.HomeScreen
import com.hisabbondhu.app.ui.screens.more.AccountScreen
import com.hisabbondhu.app.ui.screens.more.ContactScreen
import com.hisabbondhu.app.ui.screens.more.DeleteDataScreen
import com.hisabbondhu.app.ui.screens.more.LegalScreen
import com.hisabbondhu.app.ui.screens.more.MoreScreen
import com.hisabbondhu.app.ui.screens.onboarding.AuthScreen
import com.hisabbondhu.app.ui.screens.onboarding.BusinessSetupScreen
import com.hisabbondhu.app.ui.screens.onboarding.ProfileScreen
import com.hisabbondhu.app.ui.screens.onboarding.LanguageScreen
import com.hisabbondhu.app.ui.screens.parties.PartiesScreen
import com.hisabbondhu.app.ui.screens.parties.PartyDetailScreen
import com.hisabbondhu.app.ui.screens.parties.PartyEditScreen
import com.hisabbondhu.app.ui.screens.products.ProductDetailScreen
import com.hisabbondhu.app.ui.screens.products.ProductEditScreen
import com.hisabbondhu.app.ui.screens.products.ProductsScreen
import com.hisabbondhu.app.ui.screens.reports.ReportsScreen
import com.hisabbondhu.app.ui.screens.tx.TransactionsScreen
import com.hisabbondhu.app.ui.screens.tx.TxDetailScreen
import com.hisabbondhu.app.ui.screens.tx.TxEntryScreen

enum class StartState { Language, Auth, Profile, Setup, Main }

object Routes {
    const val HOME = "home"
    const val PARTIES = "parties"
    const val PRODUCTS = "products"
    const val REPORTS = "reports"
    const val MORE = "more"

    const val PARTY = "party/{id}"
    fun party(id: String) = "party/$id"

    const val PARTY_EDIT = "partyEdit?type={type}&id={id}"
    fun partyEdit(type: PartyType, id: String? = null) =
        "partyEdit?type=${type.name}" + (id?.let { "&id=$it" } ?: "")

    const val PRODUCT = "product/{id}"
    fun product(id: String) = "product/$id"

    const val PRODUCT_EDIT = "productEdit?id={id}"
    fun productEdit(id: String? = null) = "productEdit" + (id?.let { "?id=$it" } ?: "")

    const val TX_NEW = "txNew/{type}?partyId={partyId}&productId={productId}"
    fun txNew(type: TxType, partyId: String? = null, productId: String? = null): String {
        val params = listOfNotNull(partyId?.let { "partyId=$it" }, productId?.let { "productId=$it" })
        return "txNew/${type.name}" + if (params.isEmpty()) "" else params.joinToString("&", prefix = "?")
    }

    const val TX = "tx/{id}"
    fun tx(id: String) = "tx/$id"

    const val TRANSACTIONS = "transactions"
    const val BUSINESS = "business"
    const val ACCOUNT = "account"
    const val PROFILE = "profile"
    const val CONTACT = "contact"
    const val DELETE = "deleteData"
    const val LEGAL = "legal/{page}"
    fun legal(page: String) = "legal/$page"
}

private data class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Rounded.Home),
    Tab(Routes.PARTIES, R.string.nav_khata, Icons.Rounded.Groups),
    Tab(Routes.PRODUCTS, R.string.nav_products, Icons.Rounded.Inventory2),
    Tab(Routes.REPORTS, R.string.nav_reports, Icons.Rounded.BarChart),
    Tab(Routes.MORE, R.string.nav_more, Icons.Rounded.MoreHoriz),
)

@Composable
fun AppRoot(container: AppContainer, start: StartState) {
    when (start) {
        StartState.Language -> LanguageScreen()
        StartState.Auth -> AuthScreen()
        StartState.Profile -> ProfileScreen()
        StartState.Setup -> BusinessSetupScreen()
        StartState.Main -> MainNav()
    }
}

fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MainNav() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = tabs.any { it.route == route }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(bottom).consumeWindowInsets(bottom),
        ) {
            composable(Routes.HOME) { HomeScreen(nav) }
            composable(Routes.PARTIES) { PartiesScreen(nav) }
            composable(Routes.PRODUCTS) { ProductsScreen(nav) }
            composable(Routes.REPORTS) { ReportsScreen(nav) }
            composable(Routes.MORE) { MoreScreen(nav) }

            composable(Routes.PARTY, listOf(navArgument("id") { type = NavType.StringType })) { PartyDetailScreen(nav) }
            composable(
                Routes.PARTY_EDIT,
                listOf(
                    navArgument("type") { type = NavType.StringType; defaultValue = PartyType.CUSTOMER.name },
                    navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { PartyEditScreen(nav) }

            composable(Routes.PRODUCT, listOf(navArgument("id") { type = NavType.StringType })) { ProductDetailScreen(nav) }
            composable(
                Routes.PRODUCT_EDIT,
                listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { ProductEditScreen(nav) }

            composable(
                Routes.TX_NEW,
                listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("partyId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("productId") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { TxEntryScreen(nav) }
            composable(Routes.TX, listOf(navArgument("id") { type = NavType.StringType })) { TxDetailScreen(nav) }
            composable(Routes.TRANSACTIONS) { TransactionsScreen(nav) }

            composable(Routes.BUSINESS) { BusinessSetupScreen(editMode = true, onDone = { nav.popBackStack() }) }
            composable(Routes.ACCOUNT) { AccountScreen(nav) }
            composable(Routes.PROFILE) { ProfileScreen(editMode = true, onDone = { nav.popBackStack() }) }
            composable(Routes.CONTACT) { ContactScreen(nav) }
            composable(Routes.DELETE) { DeleteDataScreen(nav) }
            composable(Routes.LEGAL, listOf(navArgument("page") { type = NavType.StringType })) {
                LegalScreen(nav, it.arguments?.getString("page") ?: "privacy")
            }
        }
    }
}
