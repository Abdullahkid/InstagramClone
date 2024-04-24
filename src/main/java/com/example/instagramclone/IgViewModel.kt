package com.example.instagramclone

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.instagramclone.data.CommentData
import com.example.instagramclone.data.Event
import com.example.instagramclone.data.PostData
import com.example.instagramclone.data.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject

const val users = "users"
const val posts = "posts"
const val COMMENTS = "comments"

@HiltViewModel
class IgViewModel @Inject constructor(
    val auth : FirebaseAuth,
    val db : FirebaseFirestore,
    val storage : FirebaseStorage
):ViewModel(){

    val signedIn = mutableStateOf(false)
    val inProgress = mutableStateOf(false)
    val userData = mutableStateOf<UserData?>(null)
    val popupNotification = mutableStateOf<Event<String>?>(null)

    val refreshPostsProgress = mutableStateOf(false)
    val Posts = mutableStateOf<List<PostData>>(listOf())

    val searchPosts = mutableStateOf<List<PostData>>(listOf())
    val searchPostsProgress = mutableStateOf(false)

    val postFeed = mutableStateOf<List<PostData>>(listOf())
    val postFeedProgress = mutableStateOf(false)

    val comments = mutableStateOf<List<CommentData>>(listOf())
    val commentsProgress = mutableStateOf(false)

    val followers = mutableStateOf(0)

    init {
//        auth.signOut()
        val currentUser = auth.currentUser
        signedIn.value = currentUser != null
        currentUser?.uid?.let {uid->
            getUserData(uid)
        }
    }
    fun onSignup(username: String, email: String, password : String) {
        if(username.isEmpty()||email.isEmpty()||password.isEmpty()){
            handleException(customMessage = "Please fill in all fields")
            return
        }
        inProgress.value = true

        db.collection(users).whereEqualTo("username",username).get()
            .addOnSuccessListener { documents ->
                if (documents.size() > 0){
                    handleException(customMessage = "Username already exists")
                    inProgress.value = false
                }else{
                    auth.createUserWithEmailAndPassword(email,password)
                        .addOnCompleteListener {task->
                            if (task.isSuccessful){
                                signedIn.value = true
                                createOrUpdateProfile(username = username)
                            }else{
                                handleException(
                                    exception = task.exception,
                                    customMessage = "Signup failed")
                            }
                            inProgress.value = false
                        }
                }
            }
            .addOnFailureListener{

            }
    }

    fun onLogin(email : String, password : String){
        if (email.isEmpty() or password.isEmpty()){
            handleException(customMessage = "Please fill in all fields")
            return
        }

        inProgress.value = true
        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener{task->
                if (task.isSuccessful){
                    signedIn.value = true
                    inProgress.value= false
                    auth.currentUser?.uid?.let {uid->
//                        handleException(customMessage = "Login success ")
                        getUserData(uid)
                    }
                }else{
                    handleException(task.exception,"Login failed")
                }
            }.addOnFailureListener{exc->
                handleException(exc,"Login failed")
                inProgress.value = false
            }
    }

    private fun createOrUpdateProfile(
        name : String? = null,
        username: String? = null,
        bio : String? = null,
        imageUrl: String? = null
    ) {
        val uid = auth.currentUser?.uid
        val userData = UserData(
            userId = uid,
            name = name ?: userData.value?.name,
            username = username ?: userData.value?.username,
            bio = bio ?: userData.value?.bio,
            imageUrl = imageUrl ?: userData.value?.imageUrl,
            following = userData.value?.following
        )

        uid?.let {uid->
            inProgress.value = true
            db.collection(users).document(uid).get()
                .addOnSuccessListener {
                if (it.exists()){
                    it.reference.update(userData.toMap())
                        .addOnSuccessListener {
                            this.userData.value = userData
                            inProgress.value = false
                        }
                        .addOnFailureListener{
                            handleException(it, "Cannot update user")
                            inProgress.value = false
                        }
                }else{
                    db.collection(users).document(uid).set(userData)
                    getUserData(uid)
                    inProgress.value = false
                }
            }.addOnFailureListener{exc->
                    handleException(exc, "Cannot create User")
                    inProgress.value = false

            }
        }
    }

    private fun getUserData(uid: String) {
        inProgress.value = true
        db.collection(users).document(uid).get()
            .addOnSuccessListener {
                val user = it.toObject<UserData>()
                userData.value = user
                inProgress.value = false
                refreshPosts()
                getPersonalizedFeed()
                getFollowers(user?.userId)
            }.addOnFailureListener{exc->
                handleException(exc, "Cannot retrieve user data")
                inProgress.value = false
            }
    }

    fun handleException(
        exception: Exception? = null,
        customMessage: String = "",
    ){
        exception?.printStackTrace()
        val errorMsg = exception?.localizedMessage ?: ""
        val message = if(customMessage.isEmpty()) errorMsg else "$customMessage : $errorMsg"
        popupNotification.value = Event(message)
    }

    fun updateProfileData(
        name: String,
        username : String,
        bio : String
    ){
        createOrUpdateProfile(name ,username,bio)
    }


    fun uploadImage(uri : Uri, onSuccess: (Uri) -> Unit){
        inProgress.value = true

        val storageRef = storage.reference
        val uuid = UUID.randomUUID()
        val imageRef = storageRef.child("image/$uuid")
        val uploadTask = imageRef.putFile(uri)

        uploadTask
            .addOnSuccessListener {
                val result = it.metadata?.reference?.downloadUrl
                result?.addOnSuccessListener(onSuccess)
            }.addOnFailureListener{exc->
                handleException(exc)
                inProgress.value = false
            }
    }

    fun uploadProfileImage(uri : Uri){
        uploadImage(uri){
            createOrUpdateProfile(imageUrl = it.toString())
            updatePostUserImageData(it.toString())
        }
    }

    private fun updatePostUserImageData(imageUrl: String?){
        val currentUid = auth.currentUser?.uid
        db.collection(posts).whereEqualTo("userId", currentUid).get()
            .addOnSuccessListener {
                val POSTS = mutableStateOf<List<PostData>>(arrayListOf())
                convertPosts(it,POSTS)
                val refs = arrayListOf<DocumentReference>()
                for (post in POSTS.value){
                    post.postId?.let { id->
                        refs.add(db.collection(posts).document(id))
                    }
                }
                if (refs.isNotEmpty()){
                    db.runBatch{batch ->
                        for (ref in refs){
                            batch.update(ref,"userImage",imageUrl)
                        }
                    }
                       .addOnSuccessListener{
                            refreshPosts()
                        }
                }
            }
    }

    fun onLogout(){
        auth.signOut()
        signedIn.value = false
        userData.value = null
        popupNotification.value = Event("Logged out")
        searchPosts.value = listOf()
        postFeed.value = listOf()
        comments.value = listOf()
    }

    fun onNewPost(
        uri: Uri,
        description: String,
        onPostSuccess: () -> Unit,
    ){
        uploadImage(uri){uri->
            onCreatePost(
                imageUri = uri,
                description = description,
                onPostSuccess = onPostSuccess
            )
        }
    }

    private fun onCreatePost(
        imageUri: Uri,
        description: String,
        onPostSuccess: () -> Unit,
    ) {
        val currentUid = auth.currentUser?.uid
        val currentUsername = userData.value?.username
        val currentUserImage = userData.value?.imageUrl

        if (currentUid != null){
            val postUuid = UUID.randomUUID().toString()

            val fillerWords = listOf("this","the","be","to","is","of","and","or","a","in","it")
            val searchTerms = description
                .split(" " , "." , "," , "?" , "!" , "#")
                .map { it.lowercase() }
                .filter { it.isNotEmpty() && !fillerWords.contains(it) }

            val post = PostData(
                postId = postUuid,
                userId = currentUid,
                username = currentUsername,
                userImage = currentUserImage,
                postImage = imageUri.toString(),
                postDescription = description,
                time = System.currentTimeMillis(),
                likes = listOf<String>(),
                searchTerms = searchTerms
            )

            db.collection(posts).document(postUuid).set(post)
                .addOnSuccessListener {
                    popupNotification.value = Event("Post successfully created")
                    inProgress.value = false
                    refreshPosts()
                    onPostSuccess.invoke()
                }.addOnFailureListener{exc->
                    handleException(exc, "Unable to create post")
                    inProgress.value = false
                }

        }else{ 
            handleException(customMessage = "Error: username unavailable. Unable to create post ")
            onLogout()
            inProgress.value = false
        }
    }

    private fun refreshPosts(){
        val currentUid = auth.currentUser?.uid
        if (currentUid != null){
            refreshPostsProgress.value = true
            db.collection(posts).whereEqualTo("userId" ,currentUid).get()
                .addOnSuccessListener {documents->
                    convertPosts(documents , Posts)
                    refreshPostsProgress.value = false
                }
                .addOnFailureListener {exc->
                    handleException(exc , "Cannot fetch posts")
                    refreshPostsProgress.value = false
                }
        }else{
            handleException(customMessage = "Error: Username unavailable . Unable to refresh post")
            onLogout()
        }
    }


    private fun convertPosts(
        documents: QuerySnapshot,
        outState: MutableState<List<PostData>>
    ) {
        val newPosts = mutableListOf<PostData>()
        documents.forEach{doc->
            val post = doc.toObject<PostData>()
            newPosts.add(post)
        }
        val sortedPosts = newPosts.sortedByDescending { it.time }
        outState.value = sortedPosts
    }

    fun searchPosts(searchTerm : String){
        if (searchTerm.isNotEmpty()){
            searchPostsProgress.value = true
            db.collection(posts)
                .whereArrayContains("searchTerms",searchTerm.trim().lowercase())
                .get()
                .addOnSuccessListener {
                    convertPosts(it,searchPosts)
                    searchPostsProgress.value = false
                }.addOnFailureListener {exc->
                    handleException(exc, "Cannot search posts")
                    searchPostsProgress.value = false
                }
        }
    }

    fun onFollowClick(userId : String){
        auth.currentUser?.uid?.let {currentUser->
            val following = arrayListOf<String>()
            userData.value?.following?.let {
                following.addAll(it)
            }
            if (following.contains(userId)){
                following.remove(userId)
            }else{
                following.add(userId)
            }
            db.collection(users).document(currentUser).update("following", following)
                .addOnSuccessListener {
                    getUserData(currentUser)
                }
        }
    }

    private fun getPersonalizedFeed(){
        postFeedProgress.value = true
         val following = userData.value?.following
        if (!following.isNullOrEmpty()){
            db.collection(posts).whereIn("userId" , following).get()
                .addOnSuccessListener {
                    convertPosts(it,postFeed)
                    if (postFeed.value.isEmpty()){
                        getGeneralFeed()
                    }else{
                        postFeedProgress.value = false
                    }
                }.addOnFailureListener {exc->
                    handleException(exc , " Cannot get personalized feed")
                    postFeedProgress.value = false
                }
        }else{
            getGeneralFeed()
        }
    }

    private fun getGeneralFeed() {
        postFeedProgress.value = true
        val currentTime = System.currentTimeMillis()
        val difference = 24*60*60*1000
        db.collection(posts).whereGreaterThan("time",currentTime - difference)
            .get()
            .addOnSuccessListener {
                convertPosts(documents = it, outState = postFeed)
                postFeedProgress.value = false
            }.addOnFailureListener {exc->
                handleException(exc , "Cannot get feed")
            }
    }

    fun onLikePost(postData: PostData){
        auth.currentUser?.uid?.let {userId->
            postData.likes?.let {likes->
                val newLikes = arrayListOf<String>()
                if (likes.contains(userId)){
                    newLikes.addAll(likes.filter { userId != it })
                }else{
                    newLikes.addAll(likes)
                    newLikes.add(userId)
                }
                postData.postId?.let {postId->
                    db.collection(posts).document(postId)
                        .update("likes",newLikes)
                        .addOnSuccessListener {
                            postData.likes = newLikes
                        }.addOnFailureListener {
                            handleException(it, "Unable to like post")
                        }
                }
            }
        }
    }

    fun createComments(postId :String,text :String){
        userData.value?.username?.let {username->
            val commentId = UUID.randomUUID().toString()
            val comment= CommentData(
                commentId = commentId,
                postId = postId,
                username = username,
                text = text,
                timestamp = System.currentTimeMillis()
            )

            db.collection(COMMENTS).document(commentId).set(comment)
                .addOnSuccessListener {
                    getComments(postId)
                }
                .addOnFailureListener {exc->
                    handleException(exc, "Cannot create comment.")
                }
        }
    }

    fun getComments(postId : String?){
        commentsProgress.value = true
        db.collection(COMMENTS).whereEqualTo("postId", postId).get()
            .addOnSuccessListener {documents->
                val newComments = mutableListOf<CommentData>()
                documents.forEach{doc->
                    val comment = doc.toObject<CommentData>()
                    newComments.add(comment)
                }
                val sortedComments = newComments.sortedByDescending { it.timestamp }
                comments.value  = sortedComments
                commentsProgress.value = false
            }.addOnFailureListener {exc->
                handleException(exc,"Cannot retrieve comments")
                commentsProgress.value = false
            }
    }

    private fun getFollowers(uid: String?){
        db.collection(users).whereArrayContains("following" , uid?: "").get()
            .addOnSuccessListener {documents->
                followers.value = documents.size()
            }
            .addOnFailureListener {

            }
    }
}



















