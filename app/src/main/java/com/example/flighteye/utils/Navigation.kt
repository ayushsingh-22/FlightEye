package com.example.flighteye.utils

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.flighteye.screens.MainScreen
import com.example.flighteye.screens.PopupScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(navController)
        }

        composable("popup") {
            PopupScreen(navController)
        }
    }
}
