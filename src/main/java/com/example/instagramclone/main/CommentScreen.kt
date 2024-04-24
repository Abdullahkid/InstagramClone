package com.example.instagramclone.main

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.instagramclone.IgViewModel
import com.example.instagramclone.data.CommentData
import org.w3c.dom.Comment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    navController: NavController,
    vm: IgViewModel,
    postId: String,
){
    var commentText by remember { mutableStateOf("") }
    var focusManager = LocalFocusManager.current

    val comments = vm.comments.value
    val commentsProgress = vm.commentsProgress.value

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ){
                TextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color.LightGray),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        textColor = Color.Black,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Button(
                    onClick = {
                        vm.createComments(postId= postId,text = commentText)
                        commentText = ""
                        focusManager.clearFocus()
                    },modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(text = "Comment")
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(it)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Comments")
            }
            if (commentsProgress) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CommonProgressSpinner()
                }
            } else if (comments.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No comments available")
                }
            } else {
                LazyColumn {
                    items(items = comments) { comment ->
                        CommentRow(comment = comment)
                    }
                }
            }
        }
    }
}

@Composable
fun CommentRow(comment : CommentData) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)) {
        Text(text = comment.username ?: "", fontWeight = FontWeight.Bold)
        comment.text?.let { Text(text = it, ) }
    }
}
