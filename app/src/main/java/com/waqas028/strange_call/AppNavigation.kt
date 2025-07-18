package com.waqas028.strange_call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetUserInfo
import java.net.URL

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    val callViewModel: CallViewModel = hiltViewModel()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate("home") {
                    popUpTo("signin") { inclusive = true }
                }
            }
            is AuthState.Unauthenticated -> {
                navController.navigate("signin") {
                    popUpTo("home") { inclusive = true }
                }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("signin") {
            SignInScreen(
                authViewModel = authViewModel,
                onNavigateToHome = {
                    navController.navigate("home")
                }
            )
        }

        composable("home") {
            HomeScreen(
                viewModel = callViewModel,
                onStartCall = { roomId ->
                    val userInfo = JitsiMeetUserInfo().apply {
                        displayName = callViewModel.currentUser?.displayName
                        email = callViewModel.currentUser?.email
                        avatar = URL(callViewModel.currentUser?.photoUrl?.toString())
                    }
                    val options = JitsiMeetConferenceOptions.Builder()
                        .setServerURL(URL("https://meet.jit.si"))
                        .setRoom(roomId.ifEmpty { "TestRoom123" })
                        .setUserInfo(userInfo)
                        .setFeatureFlag("welcomepage.enabled", false)
                        .setConfigOverride("prejoinPageEnabled", false)
                        .build()

                    JitsiMeetActivity.launch(context, options)
                    callViewModel.updateCallState()
                },
                onSignOut = {
                    authViewModel.signOut()
                }
            )
        }
    }
}