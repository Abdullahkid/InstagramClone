package com.example.instagramclone

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.instagramclone.auth.LoginScreen
import com.example.instagramclone.ui.theme.InstagramCloneTheme
import com.example.instagramclone.auth.SignupScreen
import com.example.instagramclone.data.PostData
import com.example.instagramclone.main.CommentScreen
import com.example.instagramclone.main.FeedScreeen
import com.example.instagramclone.main.MyPostsScreen
import com.example.instagramclone.main.NewPostScreen
import com.example.instagramclone.main.NotificationMessage
import com.example.instagramclone.main.ProfileImage
import com.example.instagramclone.main.ProfileScreen
import com.example.instagramclone.main.SearchScreen
import com.example.instagramclone.main.SinglePostScreen
import com.google.android.gms.common.internal.safeparcel.SafeParcelable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstagramCloneTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InstagramApp()
                }
            }
        }
    }
}

sealed class DestinationScreen(val route : String){
    object Signup : DestinationScreen("signup")
    object Login : DestinationScreen("login")
    object Feed : DestinationScreen("feed")
    object Search : DestinationScreen("search")
    object MyPosts : DestinationScreen("myposts")
    object Profile : DestinationScreen("profile")
    object NewPostScreen : DestinationScreen("newpost/{imageUri}"){
        fun createRoute(uri: String) = "newpost/$uri"
    }
    object SinglePost : DestinationScreen("singlepost")
    object CommentScreen : DestinationScreen("comments/{postId}"){
        fun createRoute(postId : String) = "comments/$postId"
    }
}

@Composable
fun InstagramApp(){
    val vm = hiltViewModel<IgViewModel>()
    val navController = rememberNavController()

    NotificationMessage(vm = vm)

    NavHost(
        navController = navController,
        startDestination = DestinationScreen.Signup.route
    ){
        composable(DestinationScreen.Signup.route){
            SignupScreen(navController = navController, vm = vm )
        }
        composable(DestinationScreen.Login.route){
            LoginScreen(navController = navController, vm = vm )
        }
        composable(DestinationScreen.Feed.route){
            FeedScreeen(navController = navController, vm = vm )
        }
        composable(DestinationScreen.Search.route){
            SearchScreen(navController = navController, vm = vm )
        }
        composable(DestinationScreen.MyPosts.route){
            MyPostsScreen(navController = navController, vm = vm )
        }
        composable(DestinationScreen.Profile.route){
            ProfileScreen(navController = navController, vm = vm )
        }
        composable(DestinationScreen.NewPostScreen.route){navBackStackEntry ->
            val imageUri = navBackStackEntry.arguments?.getString("imageUri")
            imageUri?.let {
                NewPostScreen(navController = navController, vm = vm, encodedUri = it  )
            }
        }
        composable(DestinationScreen.SinglePost.route){
            val postData = navController
                .previousBackStackEntry?.savedStateHandle?.get<PostData>("post")
//                ?.arguments
//                ?.getParcelable("post",PostData::class.java)
            postData?.let {
                SinglePostScreen(
                    navController = navController,
                    vm = vm,
                    post = postData
                )
            }
        }
        composable(DestinationScreen.CommentScreen.route){navBackStackEntry ->
            val postId = navBackStackEntry.arguments?.getString("postId")
            postId?.let { CommentScreen(
                navController = navController,
                vm = vm,
                postId = postId
            ) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    InstagramCloneTheme {
        InstagramApp()
    }
}




