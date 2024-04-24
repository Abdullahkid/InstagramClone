package com.example.instagramclone.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.instagramclone.DestinationScreen
import com.example.instagramclone.IgViewModel
import com.example.instagramclone.R
import com.example.instagramclone.data.PostData

@Composable
fun SinglePostScreen(
    navController: NavController,
    vm: IgViewModel,
    post: PostData,
){
    val comments = vm.comments.value
    
    LaunchedEffect(Unit){
        vm.getComments(post.postId)
    }
    val state  = rememberScrollState()
    post.userId?.let {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp)
            ) {
                Text(text = "Back", modifier = Modifier.clickable {
                    navController.popBackStack()
                })

                CommonDivider()
                SinglePostDisplay(
                    navController = navController,
                    vm = vm,
                    post = post,
                    nbComments = comments.size
                )
            }
        }
    }
}

@Composable
fun SinglePostDisplay(
    navController : NavController,
    vm : IgViewModel,
    post : PostData,
    nbComments : Int
){
    val userData = vm.userData.value
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)){
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = CircleShape,
                modifier = Modifier
                    .padding()
                    .size(32.dp),
            ){
                Image(
                    painter = rememberAsyncImagePainter(model = post.userImage),
                    contentDescription = null
                )
            }
            
            Text(text = post.username ?: "")
            Text(text = ".", modifier = Modifier.padding(8.dp))

            if (userData?.userId == post.userId){
                //Current user's post . Dont show anything

            }else if (userData?.following?.contains(post.userId) == true){
                Text(
                    text = "Following",
                    color = Color.Gray,
                    modifier = Modifier.clickable { vm.onFollowClick(post.userId!!) })
            }
            else{
                Text(
                    text = "Follow",
                    color = Color.Blue,
                    modifier = Modifier.clickable {
                        //Follow a user
                        vm.onFollowClick(post.userId!!)
                    }
                )
            }
        }
    }

    Box {
        val modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 150.dp)
        CommonImage(
            data = post.postImage,
            modifier = modifier,
            contentScale = ContentScale.FillWidth
        )
    }

    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.baseline_favorite_24),
            contentDescription = "Like",
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(Color.Red)
        )
        Text(
            text = "${post.likes?.size ?: 0} likes",
            modifier = Modifier.padding(start = 0.dp)
        )
    }

    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = post.username ?: "",
            fontWeight = FontWeight.Bold
        )
        Text(
            text = post.postDescription ?: "",
            modifier = Modifier
                .padding(start = 8.dp)
                .align(Alignment.CenterVertically),
//            textAlign = TextAlign.Center
        )
    }

    Row(modifier = Modifier.padding(8.dp)){
        Text(
            text = "$nbComments comments",
            color = Color.Gray,
            modifier = Modifier
                .padding(8.dp)
                .clickable {
                    post.postId?.let {
                        navController.navigate(DestinationScreen.CommentScreen.createRoute(it))
                    }
                }
        )
    }
}