package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.auth.GoogleAuthGateScreen
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOutFirebaseAndCredentials
import com.example.data.repository.InvestmentRepository
import com.example.ui.components.MaintenanceOverlayScreen
import com.example.ui.components.TwoFactorPinDialog
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthPortalScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LiveChatConversationPane
import com.example.ui.screens.ProfileSecurityKycScreen
import com.example.ui.screens.VipPlansScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.CrescentWealthTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.TwoFactorChallenge
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private object Routes {
    const val DASHBOARD = "dashboard"
    const val PLANS = "plans"
    const val WALLET = "wallet"
    const val HISTORY = "history"
    const val PROFILE = "profile"
    const val LIVE_CHAT = "live_chat"
}

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val TopDestinations = listOf(
    NavDestination(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Home, Icons.Outlined.Home),
    NavDestination(Routes.PLANS, R.string.nav_plans, Icons.Filled.Diamond, Icons.Outlined.Diamond),
    NavDestination(Routes.WALLET, R.string.nav_wallet, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    NavDestination(Routes.HISTORY, R.string.nav_history, Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    NavDestination(Routes.PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            CrescentWealthTheme {
                CrescentWealthRoot()
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
private fun CrescentWealthRoot(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var showWelcomeSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(2400L)
        showWelcomeSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val user = currentUser
        if (user == null) {
            GoogleAuthGateScreen(onAuthSuccess = {})
        } else {
            key(user.uid) {
                val viewModel: AppViewModel = viewModel(
                    key = user.uid,
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY])
                            val databaseId = app.getString(R.string.firestore_database_id)
                            val db = FirebaseFirestore.getInstance(databaseId)
                            AppViewModel(
                                repository = InvestmentRepository(db, auth),
                                currentUserId = user.uid
                            )
                        }
                    }
                )
                AuthenticatedAppHost(
                    viewModel = viewModel,
                    googleEmail = user.email ?: "",
                    onGoogleSignOut = {
                        viewModel.lockAppSession()
                        signOutFirebaseAndCredentials(credentialManager, scope)
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = showWelcomeSplash,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            WelcomeOpeningScreen(onContinue = { showWelcomeSplash = false })
        }
    }
}

@Composable
private fun WelcomeOpeningScreen(onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .systemBarsPadding()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_wealth_emblem),
                    contentDescription = "Crescent Wealth Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.welcome_to_app),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("welcome_to_app_heading")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "کریسنٹ ویلتھ میں خوش آمدید",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.tagline_wealth),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("welcome_continue_button")
            ) {
                Text(
                    text = "جاری رکھیں",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedAppHost(
    viewModel: AppViewModel,
    googleEmail: String,
    onGoogleSignOut: () -> Unit
) {
    val isAppAuthenticated by viewModel.isAppAuthenticated.collectAsStateWithLifecycle()
    val isAdminMode by viewModel.isAdminMode.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val faceAiState by viewModel.faceAiState.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val twoFactorChallenge by viewModel.twoFactorChallenge.collectAsStateWithLifecycle()

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val investments by viewModel.userInvestments.collectAsStateWithLifecycle()
    val deposits by viewModel.userDeposits.collectAsStateWithLifecycle()
    val withdrawals by viewModel.userWithdrawals.collectAsStateWithLifecycle()
    val transactions by viewModel.userTransactions.collectAsStateWithLifecycle()
    val supportThread by viewModel.userSupportThread.collectAsStateWithLifecycle()
    val supportMessages by viewModel.userSupportMessages.collectAsStateWithLifecycle()

    val adminDeposits by viewModel.adminDeposits.collectAsStateWithLifecycle()
    val adminWithdrawals by viewModel.adminWithdrawals.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminPromoCodes by viewModel.adminPromoCodes.collectAsStateWithLifecycle()
    val adminSupportThreads by viewModel.adminSupportThreads.collectAsStateWithLifecycle()
    val selectedChatUserId by viewModel.selectedChatThreadUserId.collectAsStateWithLifecycle()
    val adminChatMessages by viewModel.adminSelectedChatMessages.collectAsStateWithLifecycle()
    val adminNotifications by viewModel.adminNotifications.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(feedback) {
        val current = feedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(current.message)
        viewModel.clearFeedback()
    }

    val profile = (profileState as? UiState.Success)?.data

    if (isAdminMode) {
        BackHandler {
            if (selectedChatUserId != null) {
                viewModel.selectAdminChatThread(null)
            } else {
                viewModel.lockAppSession()
            }
        }
        AdminPanelScreen(
            settings = appSettings,
            deposits = adminDeposits,
            withdrawals = adminWithdrawals,
            users = adminUsers,
            promoCodes = adminPromoCodes,
            supportThreads = adminSupportThreads,
            selectedChatUserId = selectedChatUserId,
            selectedChatMessages = adminChatMessages,
            notifications = adminNotifications,
            isBusy = isBusy,
            snackbarHostState = snackbarHostState,
            onSaveSettings = viewModel::adminSaveSettings,
            onApproveDeposit = viewModel::adminApproveDeposit,
            onRejectDeposit = viewModel::adminRejectDeposit,
            onApproveWithdrawal = viewModel::adminApproveWithdrawal,
            onRejectWithdrawal = viewModel::adminRejectWithdrawal,
            onReviewFace = viewModel::adminReviewFace,
            onCreatePromoCode = viewModel::adminCreatePromoCode,
            onTogglePromoActive = viewModel::adminTogglePromoCode,
            onDeletePromoCode = viewModel::adminDeletePromoCode,
            onSelectChatThread = viewModel::selectAdminChatThread,
            onSendAdminReply = viewModel::adminSendChatMessage,
            onMarkNotificationRead = viewModel::adminMarkNotificationRead,
            onExitAdmin = viewModel::lockAppSession
        )
    } else if (!isAppAuthenticated) {
        if (profileState is UiState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    AuthPortalScreen(
                        existingProfile = profile,
                        googleEmail = googleEmail,
                        isBusy = isBusy,
                        onLoginSubmit = viewModel::loginWithCredentials,
                        onRegisterSubmit = viewModel::registerAccount,
                        onGoogleSignOut = onGoogleSignOut
                    )
                }
            }
        }
    } else if (appSettings.maintenanceMode) {
        MaintenanceOverlayScreen(
            message = appSettings.maintenanceMessage,
            onSwitchAccountClick = viewModel::lockAppSession
        )
    } else {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: Routes.DASHBOARD
        var walletInitialMode by remember { mutableIntStateOf(0) }

        fun navigateTo(route: String) {
            navController.navigate(route) {
                popUpTo(Routes.DASHBOARD) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail && currentRoute != Routes.LIVE_CHAT) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            TopDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && currentRoute != Routes.LIVE_CHAT) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                            TopDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) }
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Routes.DASHBOARD,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(Routes.DASHBOARD) {
                            DashboardScreen(
                                profile = profile,
                                investments = investments,
                                transactions = transactions,
                                referralBonusAmount = appSettings.referralBonusAmount,
                                unreadSupportCount = supportThread?.unreadByUser ?: 0,
                                isBusy = isBusy,
                                onNavigateDeposit = {
                                    walletInitialMode = 0
                                    navigateTo(Routes.WALLET)
                                },
                                onNavigateWithdraw = {
                                    walletInitialMode = 1
                                    navigateTo(Routes.WALLET)
                                },
                                onNavigatePlans = { navigateTo(Routes.PLANS) },
                                onNavigateHistory = { navigateTo(Routes.HISTORY) },
                                onNavigateProfile = { navigateTo(Routes.PROFILE) },
                                onOpenLiveChat = {
                                    viewModel.markUserChatRead()
                                    navController.navigate(Routes.LIVE_CHAT) { launchSingleTop = true }
                                },
                                onRedeemPromoCode = viewModel::redeemPromoCode,
                                onClaimProfit = viewModel::claimDailyProfit,
                                onCopiedNotice = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                },
                                onLockSession = viewModel::lockAppSession
                            )
                        }

                        composable(Routes.PLANS) {
                            VipPlansScreen(
                                walletBalance = profile?.balance ?: 0.0,
                                isBusy = isBusy,
                                onInvestPlan = viewModel::investInPlan,
                                onNavigateDeposit = {
                                    walletInitialMode = 0
                                    navigateTo(Routes.WALLET)
                                }
                            )
                        }

                        composable(Routes.WALLET) {
                            WalletScreen(
                                initialMode = walletInitialMode,
                                profile = profile,
                                settings = appSettings,
                                deposits = deposits,
                                withdrawals = withdrawals,
                                isBusy = isBusy,
                                onSubmitDeposit = viewModel::submitDeposit,
                                onSubmitWithdrawal = viewModel::requestWithdrawal,
                                onCopiedNotice = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        }

                        composable(Routes.HISTORY) {
                            HistoryScreen(transactions = transactions)
                        }

                        composable(Routes.PROFILE) {
                            ProfileSecurityKycScreen(
                                profile = profile,
                                googleEmail = googleEmail,
                                isBusy = isBusy,
                                faceAiState = faceAiState,
                                onUpdateProfile = viewModel::updateProfile,
                                onUpdateAvatar = viewModel::updateAvatar,
                                onSubmitAiFaceVerification = viewModel::submitAiFaceVerification,
                                onUpdateTwoFactor = viewModel::updateTwoFactor,
                                onOpenLiveChat = {
                                    viewModel.markUserChatRead()
                                    navController.navigate(Routes.LIVE_CHAT) { launchSingleTop = true }
                                },
                                onCopiedNotice = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                },
                                onLockAppSession = viewModel::lockAppSession,
                                onGoogleSignOut = onGoogleSignOut
                            )
                        }

                        composable(Routes.LIVE_CHAT) {
                            LaunchedEffect(supportMessages.size) {
                                viewModel.markUserChatRead()
                            }
                            LiveChatConversationPane(
                                headerTitle = "ایڈمن سپورٹ",
                                headerSubtitle = "لائیو چیٹ • فوری مدد",
                                messages = supportMessages,
                                viewerIsAdmin = false,
                                onBack = { navController.popBackStack() },
                                onSendMessage = viewModel::sendUserChatMessage
                            )
                        }
                    }
                }
            }
        }
    }

    val activeChallenge = twoFactorChallenge
    if (activeChallenge != null) {
        val subtitle = when (activeChallenge) {
            is TwoFactorChallenge.LoginUnlock -> "اپنا اکاؤنٹ کھولنے کے لیے 2FA سیکیورٹی پن درج کریں۔"
            is TwoFactorChallenge.WithdrawalConfirm -> {
                "${activeChallenge.method.label} کے ذریعے Rs. ${activeChallenge.amount.toInt()} نکالنے کی تصدیق کے لیے 2FA سیکیورٹی پن درج کریں۔"
            }
        }
        TwoFactorPinDialog(
            subtitle = subtitle,
            onConfirm = viewModel::verifyTwoFactorPin,
            onDismiss = viewModel::dismissTwoFactorChallenge
        )
    }
}
